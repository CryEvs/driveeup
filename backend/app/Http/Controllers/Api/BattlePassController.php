<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\BattlePassLevel;
use App\Models\BattlePassSeason;
use App\Models\User;
use App\Models\UserBattlePassProgress;
use App\Models\UserBattlePassLevelReward;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\Rule;

class BattlePassController extends Controller
{
    public function current(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $season = BattlePassSeason::query()
            ->where('starts_at', '<=', now())
            ->where('ends_at', '>=', now())
            ->where('finished_early', false)
            ->latest('starts_at')
            ->with(['levels' => function ($q) use ($user) {
                $q->where('role', $user->role)->orderBy('required_drive_coin');
            }])
            ->first();

        if (! $season) {
            return response()->json([
                'season' => null,
                'levels' => [],
                'seasonDriveCoin' => 0,
                'totalDriveCoin' => (int) $user->total_drive_coin,
            ]);
        }

        $progress = UserBattlePassProgress::query()
            ->where('user_id', $user->id)
            ->where('season_id', $season->id)
            ->first();
        $claimedLevelIds = UserBattlePassLevelReward::query()
            ->where('user_id', $user->id)
            ->where('season_id', $season->id)
            ->pluck('level_id')
            ->all();
        $claimedLookup = array_flip($claimedLevelIds);

        return response()->json([
            'season' => [
                'id' => $season->id,
                'name' => $season->name,
                'startsAt' => optional($season->starts_at)->toIso8601String(),
                'endsAt' => optional($season->ends_at)->toIso8601String(),
            ],
            'levels' => $season->levels->map(function (BattlePassLevel $level) use ($claimedLookup, $progress) {
                $seasonDriveCoin = (int) ($progress?->drive_coin_earned ?? 0);
                $isUnlocked = $seasonDriveCoin >= (int) $level->required_drive_coin;
                return [
                    'id' => $level->id,
                    'levelNumber' => (int) $level->level_number,
                    'requiredDriveCoin' => (int) $level->required_drive_coin,
                    'iconUrl' => $level->icon_url,
                    'description' => $level->description,
                    // Gift details are hidden until level is unlocked.
                    'giftType' => $isUnlocked ? $level->gift_type : null,
                    'giftName' => $isUnlocked ? $level->gift_name : null,
                    'giftDescription' => $isUnlocked ? $level->gift_description : null,
                    'giftDriveCoin' => $isUnlocked ? (int) ($level->gift_drive_coin ?? 0) : null,
                    'giftText' => $isUnlocked ? $level->gift_text : null,
                    'giftHidden' => ! $isUnlocked,
                    'giftClaimed' => isset($claimedLookup[$level->id]),
                    'role' => $level->role,
                ];
            })->values(),
            'seasonDriveCoin' => (int) ($progress?->drive_coin_earned ?? 0),
            'totalDriveCoin' => (int) $user->total_drive_coin,
            'role' => $user->role,
        ]);
    }

    public function adminSeasons(Request $request)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $seasons = BattlePassSeason::query()->latest('starts_at')->with('levels')->get();
        return response()->json($seasons);
    }

    public function createSeason(Request $request)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $validated = $request->validate([
            'name' => ['required', 'string', 'max:255'],
            'startsAt' => ['required', 'date'],
            'endsAt' => ['required', 'date', 'after:startsAt'],
        ]);

        $season = BattlePassSeason::create([
            'name' => $validated['name'],
            'starts_at' => $validated['startsAt'],
            'ends_at' => $validated['endsAt'],
            'finished_early' => false,
        ]);

        return response()->json($season, 201);
    }

    public function updateSeason(Request $request, BattlePassSeason $season)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $validated = $request->validate([
            'name' => ['required', 'string', 'max:255'],
            'startsAt' => ['required', 'date'],
            'endsAt' => ['required', 'date', 'after:startsAt'],
        ]);

        $season->name = $validated['name'];
        $season->starts_at = $validated['startsAt'];
        $season->ends_at = $validated['endsAt'];
        $season->save();

        return response()->json($season);
    }

    public function deleteSeason(Request $request, BattlePassSeason $season)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $season->delete();
        return response()->json(['ok' => true]);
    }

    public function finishSeason(Request $request, BattlePassSeason $season)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $season->finished_early = true;
        $season->finished_at = now();
        $season->save();

        return response()->json(['ok' => true]);
    }

    public function createLevel(Request $request)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $validated = $request->validate([
            'seasonId' => ['required', 'integer', 'exists:battle_pass_seasons,id'],
            'role' => ['required', Rule::in(['PASSENGER', 'DRIVER'])],
            'levelNumber' => ['required', 'integer', 'min:1'],
            'requiredDriveCoin' => ['required', 'integer', 'min:0'],
            'iconUrl' => ['nullable', 'string', 'max:2000000'],
            'description' => ['nullable', 'string', 'max:2000000'],
            'giftName' => ['nullable', 'string', 'max:255'],
            'giftDescription' => ['nullable', 'string', 'max:2000000'],
            'giftDriveCoin' => ['nullable', 'integer', 'min:0'],
            'giftType' => ['required', Rule::in(['DRIVECOIN', 'TEXT'])],
            'giftText' => ['nullable', 'string', 'max:2000000'],
        ]);

        if ($validated['giftType'] === 'TEXT' && empty($validated['giftText'])) {
            return response()->json(['error' => 'Text gift requires giftText'], 422);
        }

        $level = BattlePassLevel::create([
            'season_id' => $validated['seasonId'],
            'role' => $validated['role'],
            'level_number' => $validated['levelNumber'],
            'required_drive_coin' => $validated['requiredDriveCoin'],
            'icon_url' => $validated['iconUrl'] ?? null,
            'description' => $validated['description'] ?? null,
            'gift_name' => $validated['giftName'] ?? null,
            'gift_description' => $validated['giftDescription'] ?? null,
            'gift_type' => $validated['giftType'],
            'gift_drive_coin' => $validated['giftType'] === 'DRIVECOIN' ? (int) ($validated['giftDriveCoin'] ?? 0) : 0,
            'gift_text' => $validated['giftType'] === 'TEXT' ? ($validated['giftText'] ?? null) : null,
        ]);

        return response()->json($level, 201);
    }

    public function uploadLevelIcon(Request $request)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $validated = $request->validate([
            'icon' => ['required', 'file', 'image', 'max:4096'],
        ]);

        $path = $validated['icon']->store('battle-pass-icons', 'public');
        $relativePath = ltrim(str_replace('battle-pass-icons/', '', $path), '/');

        return response()->json([
            'iconUrl' => url('/api/battle-pass/level-icons/' . rawurlencode($relativePath)),
        ], 201);
    }

    public function levelIcon(string $path)
    {
        $fullPath = 'battle-pass-icons/' . ltrim($path, '/');

        if (! Storage::disk('public')->exists($fullPath)) {
            return response()->json(['error' => 'Not found'], 404);
        }

        return response()->file(Storage::disk('public')->path($fullPath), [
            'Cache-Control' => 'public, max-age=86400',
        ]);
    }

    public function updateLevel(Request $request, BattlePassLevel $level)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $validated = $request->validate([
            'role' => ['required', Rule::in(['PASSENGER', 'DRIVER'])],
            'levelNumber' => ['required', 'integer', 'min:1'],
            'requiredDriveCoin' => ['required', 'integer', 'min:0'],
            'iconUrl' => ['nullable', 'string', 'max:2000000'],
            'description' => ['nullable', 'string', 'max:2000000'],
            'giftName' => ['nullable', 'string', 'max:255'],
            'giftDescription' => ['nullable', 'string', 'max:2000000'],
            'giftDriveCoin' => ['nullable', 'integer', 'min:0'],
            'giftType' => ['required', Rule::in(['DRIVECOIN', 'TEXT'])],
            'giftText' => ['nullable', 'string', 'max:2000000'],
        ]);

        if ($validated['giftType'] === 'TEXT' && empty($validated['giftText'])) {
            return response()->json(['error' => 'Text gift requires giftText'], 422);
        }

        $level->role = $validated['role'];
        $level->level_number = $validated['levelNumber'];
        $level->required_drive_coin = $validated['requiredDriveCoin'];
        $level->icon_url = $validated['iconUrl'] ?? null;
        $level->description = $validated['description'] ?? null;
        $level->gift_name = $validated['giftName'] ?? null;
        $level->gift_description = $validated['giftDescription'] ?? null;
        $level->gift_type = $validated['giftType'];
        $level->gift_drive_coin = $validated['giftType'] === 'DRIVECOIN' ? (int) ($validated['giftDriveCoin'] ?? 0) : 0;
        $level->gift_text = $validated['giftType'] === 'TEXT' ? ($validated['giftText'] ?? null) : null;
        $level->save();

        return response()->json($level);
    }

    public function deleteLevel(Request $request, BattlePassLevel $level)
    {
        $admin = $this->resolveAdminFromToken($request);
        if (! $admin) {
            return response()->json(['error' => 'Forbidden'], 403);
        }

        $level->delete();
        return response()->json(['ok' => true]);
    }

    public function claimLevelGift(Request $request, BattlePassLevel $level)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        if ((string) $level->role !== (string) $user->role) {
            return response()->json(['error' => 'Level is not available for your role'], 403);
        }

        $season = BattlePassSeason::query()->find($level->season_id);
        if (! $season) {
            return response()->json(['error' => 'Season not found'], 404);
        }

        $progress = UserBattlePassProgress::query()
            ->where('user_id', $user->id)
            ->where('season_id', $season->id)
            ->first();
        $seasonDriveCoin = (int) ($progress?->drive_coin_earned ?? 0);
        if ($seasonDriveCoin < (int) $level->required_drive_coin) {
            return response()->json(['error' => 'Level is not unlocked yet'], 422);
        }

        $alreadyClaimed = UserBattlePassLevelReward::query()
            ->where('user_id', $user->id)
            ->where('level_id', $level->id)
            ->exists();
        if ($alreadyClaimed) {
            return response()->json(['error' => 'Gift already claimed'], 409);
        }

        $giftType = (string) ($level->gift_type ?: 'DRIVECOIN');
        $giftDriveCoin = $giftType === 'DRIVECOIN' ? (int) ($level->gift_drive_coin ?? 0) : 0;
        UserBattlePassLevelReward::create([
            'user_id' => $user->id,
            'season_id' => $season->id,
            'level_id' => $level->id,
            'gift_name' => $level->gift_name,
            'gift_description' => $level->gift_description,
            'gift_type' => $giftType,
            'gift_drive_coin' => $giftDriveCoin,
            'gift_text' => $giftType === 'TEXT' ? $level->gift_text : null,
            'claimed_at' => now(),
        ]);

        if ($giftDriveCoin > 0) {
            $user->drivee_coin = (int) $user->drivee_coin + $giftDriveCoin;
            $user->total_drive_coin = (int) $user->total_drive_coin + $giftDriveCoin;
            $user->save();
        }

        return response()->json([
            'ok' => true,
            'giftType' => $giftType,
            'giftDriveCoin' => $giftDriveCoin,
            'giftText' => $giftType === 'TEXT' ? $level->gift_text : null,
            'driveCoin' => (int) $user->drivee_coin,
            'totalDriveCoin' => (int) $user->total_drive_coin,
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
