<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Achievement;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\Rule;

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
            'iconUrl' => $this->absoluteAchievementIconUrl($a->icon_url),
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
            'awardType' => ['required', Rule::in(['RIDES', 'INITIAL', 'EVERYONE'])],
            'ridesRequired' => ['nullable', 'integer', 'min:1'],
            'iconUrl' => ['nullable', 'string', 'max:2000000'],
            'sortOrder' => ['nullable', 'integer', 'min:0'],
            'isActive' => ['nullable', 'boolean'],
        ]);

        $err = $this->validateAwardFields($validated);
        if ($err !== null) {
            return response()->json(['error' => $err], 422);
        }

        $row = Achievement::create([
            'title' => $validated['title'],
            'description' => $validated['description'] ?? null,
            'award_type' => $validated['awardType'],
            'rides_required' => $validated['awardType'] === 'RIDES' ? (int) $validated['ridesRequired'] : null,
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
            'awardType' => ['required', Rule::in(['RIDES', 'INITIAL', 'EVERYONE'])],
            'ridesRequired' => ['nullable', 'integer', 'min:1'],
            'iconUrl' => ['nullable', 'string', 'max:2000000'],
            'sortOrder' => ['nullable', 'integer', 'min:0'],
            'isActive' => ['nullable', 'boolean'],
        ]);

        $err = $this->validateAwardFields($validated);
        if ($err !== null) {
            return response()->json(['error' => $err], 422);
        }

        $achievement->title = $validated['title'];
        $achievement->description = $validated['description'] ?? null;
        $achievement->award_type = $validated['awardType'];
        $achievement->rides_required = $validated['awardType'] === 'RIDES' ? (int) $validated['ridesRequired'] : null;
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

    /**
     * Публичный URL иконки для клиентов (относительные пути и старые записи без схемы).
     */
    private function absoluteAchievementIconUrl(?string $raw): ?string
    {
        if ($raw === null || trim($raw) === '') {
            return null;
        }
        $raw = trim($raw);
        if (preg_match('#^https?://#i', $raw)) {
            return $raw;
        }
        if (str_starts_with($raw, '/')) {
            return url($raw);
        }

        return url('/'.ltrim($raw, '/'));
    }

    /**
     * @param  array<string, mixed>  $validated
     */
    private function validateAwardFields(array $validated): ?string
    {
        if (($validated['awardType'] ?? '') === 'RIDES') {
            $n = (int) ($validated['ridesRequired'] ?? 0);
            if ($n < 1) {
                return 'Для условия «Проехать N раз» укажите количество поездок (не меньше 1).';
            }
        }

        return null;
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
