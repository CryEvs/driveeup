<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Achievement;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

class AchievementController extends Controller
{
    public function index(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $rows = Achievement::query()
            ->where('is_active', true)
            ->orderBy('sort_order')
            ->orderBy('id')
            ->get();

        return response()->json($rows->map(fn (Achievement $a) => [
            'id' => $a->id,
            'title' => $a->title,
            'description' => $a->description,
            'iconUrl' => $a->icon_url,
            'sortOrder' => (int) $a->sort_order,
        ])->values());
    }

    public function adminList(Request $request)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        return response()->json(Achievement::query()->orderBy('sort_order')->orderBy('id')->get());
    }

    public function create(Request $request)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $validated = $request->validate([
            'title' => ['required', 'string', 'max:255'],
            'description' => ['nullable', 'string', 'max:2000000'],
            'iconUrl' => ['nullable', 'string', 'max:2000000'],
            'sortOrder' => ['nullable', 'integer', 'min:0'],
            'isActive' => ['nullable', 'boolean'],
        ]);

        $row = Achievement::create([
            'title' => $validated['title'],
            'description' => $validated['description'] ?? null,
            'icon_url' => $validated['iconUrl'] ?? null,
            'sort_order' => (int) ($validated['sortOrder'] ?? 0),
            'is_active' => array_key_exists('isActive', $validated) ? (bool) $validated['isActive'] : true,
        ]);

        return response()->json($row, 201);
    }

    public function update(Request $request, Achievement $achievement)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $validated = $request->validate([
            'title' => ['required', 'string', 'max:255'],
            'description' => ['nullable', 'string', 'max:2000000'],
            'iconUrl' => ['nullable', 'string', 'max:2000000'],
            'sortOrder' => ['nullable', 'integer', 'min:0'],
            'isActive' => ['nullable', 'boolean'],
        ]);

        $achievement->title = $validated['title'];
        $achievement->description = $validated['description'] ?? null;
        $achievement->icon_url = $validated['iconUrl'] ?? null;
        $achievement->sort_order = (int) ($validated['sortOrder'] ?? 0);
        if (array_key_exists('isActive', $validated)) {
            $achievement->is_active = (bool) $validated['isActive'];
        }
        $achievement->save();

        return response()->json($achievement);
    }

    public function delete(Request $request, Achievement $achievement)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $achievement->delete();

        return response()->json(['ok' => true]);
    }

    public function uploadIcon(Request $request)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $validated = $request->validate([
            'icon' => ['required', 'file', 'image', 'max:4096'],
        ]);

        $path = $validated['icon']->store('achievement-icons', 'public');
        $relativePath = ltrim(str_replace('achievement-icons/', '', $path), '/');

        return response()->json([
            'iconUrl' => url('/api/achievements/icons/' . rawurlencode($relativePath)),
        ], 201);
    }

    public function iconFile(string $path)
    {
        $fullPath = 'achievement-icons/' . ltrim($path, '/');

        if (! Storage::disk('public')->exists($fullPath)) {
            return response()->json(['error' => 'Not found'], 404);
        }

        return response()->file(Storage::disk('public')->path($fullPath), [
            'Cache-Control' => 'public, max-age=86400',
        ]);
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

    private function resolveAdminFromToken(Request $request): ?User
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user || ! $user->is_admin) {
            return null;
        }

        return $user;
    }
}
