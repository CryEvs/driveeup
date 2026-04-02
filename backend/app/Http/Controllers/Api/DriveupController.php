<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\DriveupNextRideBenefit;
use App\Models\DriveupStoreItem;
use App\Models\DriveupTask;
use App\Models\RideOrder;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class DriveupController extends Controller
{
    private const RIDES_FOR_SILVER = 15;
    private const RIDES_FOR_GOLD = 50;

    public function content(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $tier = $this->loyaltyTierByRides((int) $user->rides_count);
        $items = $this->storeItemsForTier($tier);
        $tasks = $this->activeTasks();
        $benefits = $this->benefitsMap();
        $descriptions = $this->levelDescriptionsMap();

        return response()->json([
            'loyaltyTier' => $tier,
            'driveCoin' => (int) $user->drivee_coin,
            'ridesCount' => (int) $user->rides_count,
            'storeItems' => $items,
            'tasks' => $tasks,
            'nextRideBenefits' => $benefits,
            'nextRideBenefitForTier' => $benefits[$tier] ?? '',
            'loyaltyLevelDescriptions' => $descriptions,
        ]);
    }

    public function storeItems(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        $tier = $this->loyaltyTierByRides((int) $user->rides_count);
        return response()->json($this->storeItemsForTier($tier));
    }

    public function tasks(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        return response()->json($this->activeTasks());
    }

    public function nextRideBenefits(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        $tier = $this->loyaltyTierByRides((int) $user->rides_count);
        $map = $this->benefitsMap();
        return response()->json([
            'loyaltyTier' => $tier,
            'benefits' => $map,
            'current' => $map[$tier] ?? '',
        ]);
    }

    public function purchaseStoreItem(Request $request, DriveupStoreItem $item)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }
        if (! $item->is_active) {
            return response()->json(['error' => 'Item is inactive'], 422);
        }
        $tier = $this->loyaltyTierByRides((int) $user->rides_count);
        if (! $this->isItemAllowedForTier((string) $item->allowed_tier, $tier)) {
            return response()->json(['error' => 'Item is not available for your loyalty tier'], 403);
        }
        $price = (int) $item->price_drive_coin;
        if ((int) $user->drivee_coin < $price) {
            return response()->json(['error' => 'Not enough DriveCoin'], 422);
        }
        $user->drivee_coin = (int) $user->drivee_coin - $price;
        $user->save();

        return response()->json([
            'ok' => true,
            'driveCoin' => (int) $user->drivee_coin,
            'purchasedItemId' => $item->id,
        ]);
    }

    public function gamesAvailability(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        if ($user->role !== 'PASSENGER') {
            return response()->json([
                'available' => false,
                'reason' => 'games are available only for passenger while waiting for taxi',
            ]);
        }

        $ride = RideOrder::query()
            ->where('passenger_id', $user->id)
            ->whereIn('status', ['searching', 'accepted', 'at_pickup'])
            ->latest('id')
            ->first();

        return response()->json([
            'available' => (bool) $ride,
            'activeRideId' => $ride?->id,
        ]);
    }

    // ------- Admin: store items -------
    public function adminStoreItems(Request $request)
    {
        if (! $this->resolveAdminFromToken($request)) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        return response()->json(
            DriveupStoreItem::query()->orderBy('sort_order')->orderBy('id')->get()
        );
    }

    public function adminCreateStoreItem(Request $request)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        $v = $request->validate([
            'name' => ['required', 'string', 'max:255'],
            'iconUrl' => ['nullable', 'string', 'max:2000000'],
            'shortDescription' => ['nullable', 'string', 'max:2000000'],
            'allowedTier' => ['required', Rule::in(['ANY', 'SILVER', 'GOLD'])],
            'description' => ['nullable', 'string', 'max:2000000'],
            'usageTerms' => ['nullable', 'string', 'max:2000000'],
            'validityText' => ['nullable', 'string', 'max:2000000'],
            'priceDriveCoin' => ['required', 'integer', 'min:0'],
            'isActive' => ['nullable', 'boolean'],
            'sortOrder' => ['nullable', 'integer', 'min:0'],
        ]);
        $item = DriveupStoreItem::create([
            'name' => $v['name'],
            'icon_url' => $v['iconUrl'] ?? null,
            'short_description' => $v['shortDescription'] ?? null,
            'allowed_tier' => $v['allowedTier'],
            'description' => $v['description'] ?? null,
            'usage_terms' => $v['usageTerms'] ?? null,
            'validity_text' => $v['validityText'] ?? null,
            'price_drive_coin' => (int) $v['priceDriveCoin'],
            'is_active' => (bool) ($v['isActive'] ?? true),
            'sort_order' => (int) ($v['sortOrder'] ?? 0),
        ]);
        return response()->json($item, 201);
    }

    public function adminUpdateStoreItem(Request $request, DriveupStoreItem $item)
    {
        if (! $this->resolveAdminFromToken($request)) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        $v = $request->validate([
            'name' => ['required', 'string', 'max:255'],
            'iconUrl' => ['nullable', 'string', 'max:2000000'],
            'shortDescription' => ['nullable', 'string', 'max:2000000'],
            'allowedTier' => ['required', Rule::in(['ANY', 'SILVER', 'GOLD'])],
            'description' => ['nullable', 'string', 'max:2000000'],
            'usageTerms' => ['nullable', 'string', 'max:2000000'],
            'validityText' => ['nullable', 'string', 'max:2000000'],
            'priceDriveCoin' => ['required', 'integer', 'min:0'],
            'isActive' => ['nullable', 'boolean'],
            'sortOrder' => ['nullable', 'integer', 'min:0'],
        ]);
        $item->name = $v['name'];
        $item->icon_url = $v['iconUrl'] ?? null;
        $item->short_description = $v['shortDescription'] ?? null;
        $item->allowed_tier = $v['allowedTier'];
        $item->description = $v['description'] ?? null;
        $item->usage_terms = $v['usageTerms'] ?? null;
        $item->validity_text = $v['validityText'] ?? null;
        $item->price_drive_coin = (int) $v['priceDriveCoin'];
        $item->is_active = (bool) ($v['isActive'] ?? true);
        $item->sort_order = (int) ($v['sortOrder'] ?? 0);
        $item->save();
        return response()->json($item);
    }

    public function adminDeleteStoreItem(Request $request, DriveupStoreItem $item)
    {
        if (! $this->resolveAdminFromToken($request)) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        $item->delete();
        return response()->json(['ok' => true]);
    }

    // ------- Admin: tasks -------
    public function adminTasks(Request $request)
    {
        if (! $this->resolveAdminFromToken($request)) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        return response()->json(DriveupTask::query()->orderBy('sort_order')->orderBy('id')->get());
    }

    public function adminCreateTask(Request $request)
    {
        if (! $this->resolveAdminFromToken($request)) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        $v = $request->validate([
            'title' => ['required', 'string', 'max:255'],
            'description' => ['nullable', 'string', 'max:2000000'],
            'completionType' => ['required', Rule::in(['RIDES', 'RATING', 'REFERRAL'])],
            'requiredRidesCount' => [
                Rule::requiredIf(fn () => ($request->input('completionType') ?? 'RIDES') === 'RIDES'),
                'nullable',
                'integer',
                'min:1',
            ],
            'rewardDriveCoin' => ['required', 'integer', 'min:0'],
            'isActive' => ['nullable', 'boolean'],
            'sortOrder' => ['nullable', 'integer', 'min:0'],
        ]);
        $task = DriveupTask::create([
            'title' => $v['title'],
            'description' => $v['description'] ?? null,
            'completion_type' => $v['completionType'],
            'required_rides_count' => ($v['completionType'] ?? 'RIDES') === 'RIDES'
                ? (int) ($v['requiredRidesCount'] ?? 1)
                : null,
            'reward_drive_coin' => (int) $v['rewardDriveCoin'],
            'is_active' => (bool) ($v['isActive'] ?? true),
            'sort_order' => (int) ($v['sortOrder'] ?? 0),
        ]);
        return response()->json($task, 201);
    }

    public function adminUpdateTask(Request $request, DriveupTask $task)
    {
        if (! $this->resolveAdminFromToken($request)) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        $v = $request->validate([
            'title' => ['required', 'string', 'max:255'],
            'description' => ['nullable', 'string', 'max:2000000'],
            'completionType' => ['required', Rule::in(['RIDES', 'RATING', 'REFERRAL'])],
            'requiredRidesCount' => [
                Rule::requiredIf(fn () => ($request->input('completionType') ?? 'RIDES') === 'RIDES'),
                'nullable',
                'integer',
                'min:1',
            ],
            'rewardDriveCoin' => ['required', 'integer', 'min:0'],
            'isActive' => ['nullable', 'boolean'],
            'sortOrder' => ['nullable', 'integer', 'min:0'],
        ]);
        $task->title = $v['title'];
        $task->description = $v['description'] ?? null;
        $task->completion_type = $v['completionType'];
        $task->required_rides_count = ($v['completionType'] ?? 'RIDES') === 'RIDES'
            ? (int) ($v['requiredRidesCount'] ?? 1)
            : null;
        $task->reward_drive_coin = (int) $v['rewardDriveCoin'];
        $task->is_active = (bool) ($v['isActive'] ?? true);
        $task->sort_order = (int) ($v['sortOrder'] ?? 0);
        $task->save();
        return response()->json($task);
    }

    public function adminDeleteTask(Request $request, DriveupTask $task)
    {
        if (! $this->resolveAdminFromToken($request)) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        $task->delete();
        return response()->json(['ok' => true]);
    }

    // ------- Admin: next ride benefits by tier -------
    public function adminNextRideBenefits(Request $request)
    {
        if (! $this->resolveAdminFromToken($request)) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        $rows = DriveupNextRideBenefit::query()->orderByRaw("FIELD(tier, 'BRONZE','SILVER','GOLD')")->get();
        return response()->json($rows);
    }

    public function adminUpsertNextRideBenefit(Request $request)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }
        $v = $request->validate([
            'tier' => ['required', Rule::in(['BRONZE', 'SILVER', 'GOLD'])],
            'benefitText' => ['required', 'string', 'max:2000000'],
            'levelDescription' => ['nullable', 'string', 'max:2000000'],
        ]);
        $row = DriveupNextRideBenefit::query()->firstOrNew(['tier' => $v['tier']]);
        $row->benefit_text = $v['benefitText'];
        $row->level_description = $v['levelDescription'] ?? null;
        $row->updated_by_user_id = $admin->id;
        $row->save();
        return response()->json($row);
    }

    private function storeItemsForTier(string $tier): array
    {
        return DriveupStoreItem::query()
            ->where('is_active', true)
            ->orderBy('sort_order')
            ->orderBy('id')
            ->get()
            ->map(function (DriveupStoreItem $item) use ($tier) {
                return [
                    'id' => $item->id,
                    'name' => $item->name,
                    'iconUrl' => $item->icon_url,
                    'shortDescription' => $item->short_description,
                    'allowedTier' => $item->allowed_tier,
                    'description' => $item->description,
                    'usageTerms' => $item->usage_terms,
                    'validityText' => $item->validity_text,
                    'priceDriveCoin' => (int) $item->price_drive_coin,
                    'isAvailableForCurrentTier' => $this->isItemAllowedForTier((string) $item->allowed_tier, $tier),
                    'sortOrder' => (int) $item->sort_order,
                ];
            })->values()->all();
    }

    private function activeTasks(): array
    {
        return DriveupTask::query()
            ->where('is_active', true)
            ->orderBy('sort_order')
            ->orderBy('id')
            ->get()
            ->map(fn (DriveupTask $task) => [
                'id' => $task->id,
                'title' => $task->title,
                'description' => $task->description,
                'completionType' => $task->completion_type,
                'requiredRidesCount' => $task->required_rides_count !== null ? (int) $task->required_rides_count : null,
                'rewardDriveCoin' => (int) $task->reward_drive_coin,
                'sortOrder' => (int) $task->sort_order,
            ])->values()->all();
    }

    private function benefitsMap(): array
    {
        $rows = DriveupNextRideBenefit::query()->get();
        $map = [
            'BRONZE' => 'Базовые бонусы программы лояльности DriveUP',
            'SILVER' => 'Расширенные бонусы и приоритет в программе',
            'GOLD' => 'Максимальные привилегии и приоритет DriveUP',
        ];
        foreach ($rows as $row) {
            $map[(string) $row->tier] = (string) $row->benefit_text;
        }
        return $map;
    }

    private function levelDescriptionsMap(): array
    {
        $rows = DriveupNextRideBenefit::query()->get();
        $map = [
            'BRONZE' => 'Бронзовый уровень: стартовые привилегии и базовые преимущества.',
            'SILVER' => 'Серебряный уровень: больше бонусов и улучшенные условия.',
            'GOLD' => 'Золотой уровень: максимальные привилегии и лучшие условия сервиса.',
        ];
        foreach ($rows as $row) {
            if (! empty($row->level_description)) {
                $map[(string) $row->tier] = (string) $row->level_description;
            }
        }
        return $map;
    }

    private function loyaltyTierByRides(int $ridesCount): string
    {
        return match (true) {
            $ridesCount >= self::RIDES_FOR_GOLD => 'GOLD',
            $ridesCount >= self::RIDES_FOR_SILVER => 'SILVER',
            default => 'BRONZE',
        };
    }

    private function isItemAllowedForTier(string $allowedTier, string $userTier): bool
    {
        if ($allowedTier === 'ANY') return true;
        if ($allowedTier === 'SILVER') return in_array($userTier, ['SILVER', 'GOLD'], true);
        if ($allowedTier === 'GOLD') return $userTier === 'GOLD';
        return false;
    }

    private function resolveUserFromToken(Request $request): ?User
    {
        $header = $request->header('Authorization', '');
        $token = str_starts_with($header, 'Bearer ') ? substr($header, 7) : null;
        if (! $token) {
            return null;
        }
        return User::query()->where('api_token', $token)->first();
    }

    private function resolveAdminFromToken(Request $request): ?User
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user || ! $user->is_admin) {
            return null;
        }
        return $user;
    }
}

