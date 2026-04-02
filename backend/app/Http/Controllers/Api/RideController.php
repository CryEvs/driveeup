<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\RideOrder;
use App\Models\RideOrderSkip;
use App\Models\User;
use App\Models\UserNotification;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class RideController extends Controller
{
    private function resolveUser(Request $request): ?User
    {
        $header = $request->header('Authorization', '');
        $token = str_starts_with($header, 'Bearer ') ? substr($header, 7) : null;
        if (! $token) {
            return null;
        }

        return User::where('api_token', $token)->first();
    }

    public function store(Request $request)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        if (! in_array($user->role, ['PASSENGER', 'ADMIN'], true)) {
            return response()->json(['error' => 'Only passengers can create orders'], 403);
        }

        $validated = $request->validate([
            'fromLat' => ['required', 'numeric'],
            'fromLon' => ['required', 'numeric'],
            'fromAddress' => ['required', 'string', 'max:2000'],
            'toLat' => ['required', 'numeric'],
            'toLon' => ['required', 'numeric'],
            'toAddress' => ['required', 'string', 'max:2000'],
            'priceRub' => ['required', 'integer', 'min:1', 'max:999999'],
        ]);

        $active = RideOrder::where('passenger_id', $user->id)
            ->whereIn('status', ['searching', 'accepted', 'at_pickup', 'in_trip'])
            ->exists();
        if ($active) {
            return response()->json(['error' => 'Active order already exists'], 422);
        }

        $ride = RideOrder::create([
            'passenger_id' => $user->id,
            'from_lat' => $validated['fromLat'],
            'from_lon' => $validated['fromLon'],
            'from_address' => $validated['fromAddress'],
            'to_lat' => $validated['toLat'],
            'to_lon' => $validated['toLon'],
            'to_address' => $validated['toAddress'],
            'price_rub' => $validated['priceRub'],
            'agreed_price_rub' => null,
            'status' => 'searching',
        ]);

        return response()->json($this->transformRide($ride->fresh(['passenger', 'driver'])), 201);
    }

    public function passengerActive(Request $request)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $ride = RideOrder::with(['passenger', 'driver'])
            ->where('passenger_id', $user->id)
            ->where(function ($q) {
                $q->whereIn('status', ['searching', 'accepted', 'at_pickup', 'in_trip'])
                    ->orWhere(function ($q2) {
                        $q2->where('status', 'completed')->whereNull('driver_rating');
                    });
            })
            ->orderByDesc('id')
            ->first();

        return response()->json(['ride' => $ride ? $this->transformRide($ride) : null]);
    }

    public function driverActive(Request $request)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        if (! in_array($user->role, ['DRIVER', 'ADMIN'], true)) {
            return response()->json(['error' => 'Only drivers'], 403);
        }

        $ride = RideOrder::with(['passenger', 'driver'])
            ->where('driver_id', $user->id)
            ->whereIn('status', ['accepted', 'at_pickup', 'in_trip'])
            ->orderByDesc('id')
            ->first();

        return response()->json(['ride' => $ride ? $this->transformRide($ride) : null]);
    }

    public function driverFeed(Request $request)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        if (! in_array($user->role, ['DRIVER', 'ADMIN'], true)) {
            return response()->json(['error' => 'Only drivers'], 403);
        }

        $skipped = RideOrderSkip::where('driver_id', $user->id)->pluck('ride_order_id');

        $orders = RideOrder::with('passenger')
            ->where('status', 'searching')
            ->whereNotIn('id', $skipped)
            ->orderByDesc('id')
            ->limit(50)
            ->get();

        return response()->json([
            'orders' => $orders->map(fn ($r) => $this->transformRide($r))->values(),
        ]);
    }

    public function show(Request $request, int $id)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $ride = RideOrder::with(['passenger', 'driver'])->find($id);
        if (! $ride) {
            return response()->json(['error' => 'Not found'], 404);
        }
        if ($ride->passenger_id !== $user->id && $ride->driver_id !== $user->id && ! $user->is_admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        return response()->json($this->transformRide($ride));
    }

    public function skip(Request $request, int $id)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        if (! in_array($user->role, ['DRIVER', 'ADMIN'], true)) {
            return response()->json(['error' => 'Only drivers'], 403);
        }

        $ride = RideOrder::find($id);
        if (! $ride || $ride->status !== 'searching') {
            return response()->json(['error' => 'Not found'], 404);
        }

        RideOrderSkip::firstOrCreate([
            'ride_order_id' => $ride->id,
            'driver_id' => $user->id,
        ]);

        return response()->json(['ok' => true]);
    }

    public function counter(Request $request, int $id)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        if (! in_array($user->role, ['DRIVER', 'ADMIN'], true)) {
            return response()->json(['error' => 'Only drivers'], 403);
        }

        $validated = $request->validate([
            'priceRub' => ['required', 'integer', 'min:1', 'max:999999'],
        ]);

        $ride = RideOrder::where('id', $id)->where('status', 'searching')->first();
        if (! $ride) {
            return response()->json(['error' => 'Not found'], 404);
        }

        $ride->agreed_price_rub = $validated['priceRub'];
        $ride->save();

        return response()->json($this->transformRide($ride->fresh(['passenger', 'driver'])));
    }

    public function accept(Request $request, int $id)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        if (! in_array($user->role, ['DRIVER', 'ADMIN'], true)) {
            return response()->json(['error' => 'Only drivers'], 403);
        }

        $validated = $request->validate([
            'etaMinutes' => ['nullable', 'integer', 'min:1', 'max:120'],
            'agreedPriceRub' => ['nullable', 'integer', 'min:1', 'max:999999'],
        ]);

        try {
            $ride = DB::transaction(function () use ($id, $user, $validated) {
                $r = RideOrder::lockForUpdate()->find($id);
                if (! $r || $r->status !== 'searching') {
                    return 'unavailable';
                }
                if ($r->driver_id !== null) {
                    return 'taken';
                }

                $price = $validated['agreedPriceRub'] ?? $r->agreed_price_rub ?? $r->price_rub;

                $r->driver_id = $user->id;
                $r->agreed_price_rub = $price;
                $r->driver_eta_minutes = $validated['etaMinutes'] ?? 3;
                $r->status = 'accepted';
                $r->save();

                return $r;
            });
        } catch (\Throwable $e) {
            return response()->json(['error' => 'Server error'], 500);
        }

        if ($ride === 'unavailable') {
            return response()->json(['error' => 'Order not available'], 409);
        }
        if ($ride === 'taken') {
            return response()->json(['error' => 'Already taken'], 409);
        }

        return response()->json($this->transformRide($ride->fresh(['passenger', 'driver'])));
    }

    public function arrived(Request $request, int $id)
    {
        return $this->updateStatusInternal($request, $id, 'at_pickup', ['driver']);
    }

    public function startTrip(Request $request, int $id)
    {
        return $this->updateStatusInternal($request, $id, 'in_trip', ['driver']);
    }

    public function complete(Request $request, int $id)
    {
        return $this->updateStatusInternal($request, $id, 'completed', ['driver']);
    }

    public function passengerExit(Request $request, int $id)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $ride = RideOrder::find($id);
        if (! $ride || $ride->passenger_id !== $user->id) {
            return response()->json(['error' => 'Not found'], 403);
        }
        if (! in_array($ride->status, ['accepted', 'at_pickup'], true)) {
            return response()->json(['error' => 'Invalid state'], 422);
        }

        $ride->passenger_exiting = true;
        $ride->save();

        return response()->json($this->transformRide($ride->fresh(['passenger', 'driver'])));
    }

    public function cancelPassenger(Request $request, int $id)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $ride = RideOrder::find($id);
        if (! $ride || $ride->passenger_id !== $user->id) {
            return response()->json(['error' => 'Not found'], 403);
        }
        if (! in_array($ride->status, ['searching', 'accepted', 'at_pickup'], true)) {
            return response()->json(['error' => 'Cannot cancel'], 422);
        }

        $ride->status = 'cancelled';
        $ride->save();

        return response()->json($this->transformRide($ride->fresh(['passenger', 'driver'])));
    }

    public function cancelDriver(Request $request, int $id)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $ride = RideOrder::find($id);
        if (! $ride || $ride->driver_id !== $user->id) {
            return response()->json(['error' => 'Not found'], 403);
        }
        if (! in_array($ride->status, ['accepted', 'at_pickup', 'in_trip'], true)) {
            return response()->json(['error' => 'Cannot cancel'], 422);
        }

        $ride->status = 'cancelled';
        $ride->save();

        return response()->json($this->transformRide($ride->fresh(['passenger', 'driver'])));
    }

    public function rate(Request $request, int $id)
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $validated = $request->validate([
            'stars' => ['required', 'integer', 'min:1', 'max:5'],
            'target' => ['required', 'in:driver,passenger'],
        ]);

        $ride = RideOrder::with(['passenger', 'driver'])->find($id);
        if (! $ride || $ride->status !== 'completed') {
            return response()->json(['error' => 'Not found'], 404);
        }

        if ($validated['target'] === 'driver') {
            if ($ride->passenger_id !== $user->id) {
                return response()->json(['error' => 'Forbidden'], 403);
            }
            if ($ride->driver_rating !== null) {
                return response()->json(['error' => 'Already rated'], 422);
            }
            $ride->driver_rating = $validated['stars'];
            $ride->save();
            if ($ride->driver) {
                $this->applyRating($ride->driver, $validated['stars']);
            }
        } else {
            if ($ride->driver_id !== $user->id) {
                return response()->json(['error' => 'Forbidden'], 403);
            }
            if ($ride->passenger_rating !== null) {
                return response()->json(['error' => 'Already rated'], 422);
            }
            $ride->passenger_rating = $validated['stars'];
            $ride->save();
            if ($ride->passenger) {
                $this->applyRating($ride->passenger, $validated['stars']);
            }
        }

        return response()->json($this->transformRide($ride->fresh(['passenger', 'driver'])));
    }

    private function updateStatusInternal(Request $request, int $id, string $newStatus, array $allowedRoles): \Illuminate\Http\JsonResponse
    {
        $user = $this->resolveUser($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        if ($allowedRoles === ['driver'] && ! in_array($user->role, ['DRIVER', 'ADMIN'], true)) {
            return response()->json(['error' => 'Only drivers'], 403);
        }

        $ride = RideOrder::find($id);
        if (! $ride || $ride->driver_id !== $user->id) {
            return response()->json(['error' => 'Not found'], 404);
        }

        $ok = match ($newStatus) {
            'at_pickup' => $ride->status === 'accepted',
            'in_trip' => $ride->status === 'at_pickup',
            'completed' => $ride->status === 'in_trip',
            default => false,
        };

        if (! $ok) {
            return response()->json(['error' => 'Invalid transition'], 422);
        }

        if ($newStatus === 'in_trip') {
            $ride->passenger_exiting = false;
        }

        $ride->status = $newStatus;
        $ride->save();

        if ($newStatus === 'completed') {
            $this->accruePassengerRideCoins((int) $ride->id);
        }

        return response()->json($this->transformRide($ride->fresh(['passenger', 'driver'])));
    }

    /**
     * Начисление DriveCoin пассажиру: стоимость поездки × 0.1 × коэффициент рейтинга.
     * Идемпотентно: один раз на заказ (ride_coin_credited), в транзакции с блокировкой строк.
     */
    private function accruePassengerRideCoins(int $rideId): void
    {
        try {
            DB::transaction(function () use ($rideId) {
                $lockedRide = RideOrder::lockForUpdate()->find($rideId);
                if (! $lockedRide || $lockedRide->ride_coin_credited) {
                    return;
                }

                $passenger = User::lockForUpdate()->find($lockedRide->passenger_id);
                if (! $passenger) {
                    return;
                }

                $priceRub = (float) ($lockedRide->agreed_price_rub ?? $lockedRide->price_rub);
                if ($priceRub <= 0) {
                    return;
                }

                $rating = (float) ($passenger->rating_avg ?? 5.0);
                $coeff = $this->passengerRideRatingCoefficient($rating);
                $amount = round($priceRub * 0.1 * $coeff, 2);
                if ($amount <= 0) {
                    return;
                }

                $passenger->drivee_coin = round((float) $passenger->drivee_coin + $amount, 2);
                $passenger->total_drive_coin = round((float) $passenger->total_drive_coin + $amount, 2);
                $passenger->save();

                $lockedRide->ride_coin_credited = true;
                $lockedRide->save();

                $amountStr = rtrim(rtrim(number_format($amount, 2, '.', ''), '0'), '.');
                $coeffStr = rtrim(rtrim(number_format($coeff, 2, '.', ''), '0'), '.');
                $ratingStr = number_format($rating, 2, '.', '');

                try {
                    UserNotification::create([
                        'user_id' => $passenger->id,
                        'type' => 'DRIVECOIN_ACCRUAL',
                        'title' => 'Начисление ДрайвКойнов',
                        'body' => 'Начисление койнов за поездку: '.$amountStr
                            .' (поездка '.(int) round($priceRub).' ₽, ваш рейтинг '.$ratingStr.', коэффициент '.$coeffStr.')',
                    ]);
                } catch (\Throwable $e) {
                    \Log::warning('ride coin notification: '.$e->getMessage());
                }
            });
        } catch (\Throwable $e) {
            \Log::error('accruePassengerRideCoins: '.$e->getMessage(), ['rideId' => $rideId]);
        }
    }

    private function passengerRideRatingCoefficient(float $rating): float
    {
        $r = max(0.0, min(5.0, $rating));
        if ($r >= 4.95) {
            return 1.0;
        }
        if ($r < 4.1) {
            return 0.1;
        }
        if ($r <= 4.9) {
            return max(0.1, min(0.9, ($r - 4.1) + 0.1));
        }

        return 0.9 + ($r - 4.9) / (4.95 - 4.9) * 0.1;
    }

    private function applyRating(User $ratedUser, int $stars): void
    {
        $count = (int) $ratedUser->rides_count;
        $avg = (float) $ratedUser->rating_avg;
        $newCount = $count + 1;
        $newAvg = $count > 0
            ? round((($avg * $count) + $stars) / $newCount, 2)
            : (float) $stars;
        $ratedUser->rides_count = $newCount;
        $ratedUser->rating_avg = $newAvg;
        $ratedUser->save();
    }

    private function transformRide(RideOrder $ride): array
    {
        $displayPrice = $ride->agreed_price_rub ?? $ride->price_rub;

        return [
            'id' => $ride->id,
            'passengerId' => $ride->passenger_id,
            'driverId' => $ride->driver_id,
            'fromLat' => $ride->from_lat,
            'fromLon' => $ride->from_lon,
            'fromAddress' => $ride->from_address,
            'toLat' => $ride->to_lat,
            'toLon' => $ride->to_lon,
            'toAddress' => $ride->to_address,
            'priceRub' => $ride->price_rub,
            'agreedPriceRub' => $ride->agreed_price_rub,
            'displayPriceRub' => $displayPrice,
            'status' => $ride->status,
            'driverEtaMinutes' => $ride->driver_eta_minutes,
            'passengerExiting' => (bool) $ride->passenger_exiting,
            'passengerRating' => $ride->passenger_rating,
            'driverRating' => $ride->driver_rating,
            'createdAt' => $ride->created_at?->toIso8601String(),
            'passenger' => $ride->passenger ? $this->publicUser($ride->passenger) : null,
            'driver' => $ride->driver ? $this->publicUser($ride->driver) : null,
        ];
    }

    private function publicUser(User $u): array
    {
        $first = $u->first_name ?: (explode(' ', $u->name)[0] ?? $u->name);

        return [
            'id' => $u->id,
            'firstName' => $first,
            'lastName' => $u->last_name ?? '',
            'email' => $u->email,
            'avatarUrl' => $u->avatar_url,
            'ratingAvg' => round((float) $u->rating_avg, 2),
            'ridesCount' => (int) $u->rides_count,
            'vehicleModel' => $u->vehicle_model,
            'vehiclePlate' => $u->vehicle_plate,
        ];
    }
}
