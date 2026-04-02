<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\UserNotification;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\Rule;

class AuthController extends Controller
{
    public function register(Request $request)
    {
        $validated = $request->validate([
            'email' => ['required', 'email', 'max:255', 'unique:users,email'],
            'password' => ['required', 'string', 'min:6'],
            'role' => ['required', Rule::in(['PASSENGER', 'DRIVER'])],
            'firstName' => ['nullable', 'string', 'max:120'],
            'lastName' => ['nullable', 'string', 'max:120'],
        ]);

        $emailLocal = explode('@', $validated['email'])[0];
        $first = $validated['firstName'] ?? null;
        $last = $validated['lastName'] ?? null;
        $displayName = trim(implode(' ', array_filter([$first, $last]))) ?: $emailLocal;

        $user = User::create([
            'name' => $displayName,
            'first_name' => $first,
            'last_name' => $last,
            'email' => mb_strtolower(trim($validated['email'])),
            'password' => Hash::make($validated['password']),
            'role' => $validated['role'],
            'is_admin' => false,
            'drivee_coin' => 0,
            'total_drive_coin' => 0,
            'premium' => false,
            'api_token' => bin2hex(random_bytes(32)),
        ]);

        UserNotification::create([
            'user_id' => $user->id,
            'type' => 'FIRST_LOGIN',
            'title' => 'Добро пожаловать!',
            'body' => 'Добро пожаловать в систему лояльности DriveUP!',
        ]);

        return response()->json([
            'accessToken' => $user->api_token,
            'user' => $this->transformUser($user),
        ], 201);
    }

    public function login(Request $request)
    {
        $validated = $request->validate([
            'email' => ['required', 'email'],
            'password' => ['required', 'string'],
        ]);

        $user = User::where('email', mb_strtolower(trim($validated['email'])))->first();

        if (! $user || ! Hash::check($validated['password'], $user->password)) {
            return response()->json(['error' => 'Invalid credentials'], 401);
        }

        $user->api_token = bin2hex(random_bytes(32));
        $user->save();

        return response()->json([
            'accessToken' => $user->api_token,
            'user' => $this->transformUser($user),
        ]);
    }

    public function me(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        return response()->json($this->transformUser($user));
    }

    public function updateAvatar(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $validated = $request->validate([
            'avatarUrl' => ['required', 'string', 'max:2000000'],
        ]);

        $user->avatar_url = $validated['avatarUrl'];
        $user->save();

        return response()->json($this->transformUser($user));
    }

    public function updateProfile(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $validated = $request->validate([
            'firstName' => ['nullable', 'string', 'max:120'],
            'lastName' => ['nullable', 'string', 'max:120'],
            'email' => ['required', 'email', 'max:255', Rule::unique('users', 'email')->ignore($user->id)],
            'city' => ['nullable', 'string', 'max:120'],
        ]);

        $first = trim((string) ($validated['firstName'] ?? ''));
        $last = trim((string) ($validated['lastName'] ?? ''));
        $displayName = trim(implode(' ', array_filter([$first, $last])));

        $user->first_name = $first;
        $user->last_name = $last;
        $user->email = mb_strtolower(trim((string) $validated['email']));
        $user->city = trim((string) ($validated['city'] ?? ''));
        $user->name = $displayName !== '' ? $displayName : explode('@', $user->email)[0];
        $user->save();

        return response()->json($this->transformUser($user));
    }

    public function setRole(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $validated = $request->validate([
            'role' => ['required', Rule::in(['PASSENGER', 'DRIVER'])],
        ]);

        $user->role = $validated['role'];
        $user->save();

        return response()->json($this->transformUser($user));
    }

    private function resolveUserFromToken(Request $request): ?User
    {
        $header = $request->header('Authorization', '');
        $token = str_starts_with($header, 'Bearer ') ? substr($header, 7) : null;
        if (! $token) {
            return null;
        }
        return User::where('api_token', $token)->first();
    }

    private function transformUser(User $user): array
    {
        $first = $user->first_name;
        $last = $user->last_name;
        if (! $first && ! $last && $user->name) {
            $parts = preg_split('/\s+/', $user->name, 2);
            $first = $first ?: ($parts[0] ?? '');
            $last = $last ?: ($parts[1] ?? '');
        }

        return [
            'id' => $user->id,
            'email' => $user->email,
            'firstName' => $first ?? '',
            'lastName' => $last ?? '',
            'city' => $user->city ?? '',
            'role' => $user->role,
            'isAdmin' => (bool) $user->is_admin,
            'driveCoin' => round((float) $user->drivee_coin, 2),
            'totalDriveCoin' => round((float) $user->total_drive_coin, 2),
            'premium' => (bool) $user->premium,
            'avatarUrl' => $user->avatar_url,
            'ratingAvg' => isset($user->rating_avg) ? round((float) $user->rating_avg, 2) : 5.0,
            'ridesCount' => isset($user->rides_count) ? (int) $user->rides_count : 0,
            'vehicleModel' => $user->vehicle_model,
            'vehiclePlate' => $user->vehicle_plate,
        ];
    }
}
