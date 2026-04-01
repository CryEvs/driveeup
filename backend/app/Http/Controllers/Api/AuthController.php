<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
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
        ]);

        $user = User::create([
            'name' => explode('@', $validated['email'])[0],
            'email' => mb_strtolower(trim($validated['email'])),
            'password' => Hash::make($validated['password']),
            'role' => $validated['role'],
            'is_admin' => false,
            'drivee_coin' => 0,
            'total_drive_coin' => 0,
            'premium' => false,
            'api_token' => bin2hex(random_bytes(32)),
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
        return [
            'id' => $user->id,
            'email' => $user->email,
            'role' => $user->role,
            'isAdmin' => (bool) $user->is_admin,
            'driveCoin' => (int) $user->drivee_coin,
            'totalDriveCoin' => (int) $user->total_drive_coin,
            'premium' => (bool) $user->premium,
            'avatarUrl' => $user->avatar_url,
        ];
    }
}
