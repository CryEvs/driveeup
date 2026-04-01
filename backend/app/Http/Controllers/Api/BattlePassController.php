<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\BattlePassLevel;
use App\Models\BattlePassSeason;
use App\Models\User;
use App\Models\UserBattlePassProgress;
use Illuminate\Http\Request;
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

        return response()->json([
            'season' => [
                'id' => $season->id,
                'name' => $season->name,
                'startsAt' => optional($season->starts_at)->toIso8601String(),
                'endsAt' => optional($season->ends_at)->toIso8601String(),
            ],
            'levels' => $season->levels->map(function (BattlePassLevel $level) {
                return [
                    'id' => $level->id,
                    'levelNumber' => (int) $level->level_number,
                    'requiredDriveCoin' => (int) $level->required_drive_coin,
                    'iconUrl' => $level->icon_url,
                    'description' => $level->description,
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
        ]);

        $level = BattlePassLevel::create([
            'season_id' => $validated['seasonId'],
            'role' => $validated['role'],
            'level_number' => $validated['levelNumber'],
            'required_drive_coin' => $validated['requiredDriveCoin'],
            'icon_url' => $validated['iconUrl'] ?? null,
            'description' => $validated['description'] ?? null,
        ]);

        return response()->json($level, 201);
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
        ]);

        $level->role = $validated['role'];
        $level->level_number = $validated['levelNumber'];
        $level->required_drive_coin = $validated['requiredDriveCoin'];
        $level->icon_url = $validated['iconUrl'] ?? null;
        $level->description = $validated['description'] ?? null;
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
