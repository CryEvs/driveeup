<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\BattlePassSeason;
use App\Models\User;
use App\Models\UserBattlePassProgress;
use Illuminate\Http\Request;

class GameRewardController extends Controller
{
    /**
     * Начисление DriveeCoin за пройденные полосы в игре «Перебеги дорогу».
     * 0.5 монеты за полосу → на сервере округляем до целого.
     */
    public function claimDriveeCoin(Request $request)
    {
        $user = $this->resolveUserFromToken($request);
        if (! $user) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        $validated = $request->validate([
            'lanesPassed' => ['required', 'integer', 'min:0', 'max:100000'],
        ]);

        $lanes = $validated['lanesPassed'];
        $claimed = (int) round($lanes * 0.5);

        $user->drivee_coin = (int) $user->drivee_coin + $claimed;
        $user->total_drive_coin = (int) $user->total_drive_coin + $claimed;
        $user->save();

        $season = BattlePassSeason::query()
            ->where('starts_at', '<=', now())
            ->where('ends_at', '>=', now())
            ->where('finished_early', false)
            ->latest('starts_at')
            ->first();

        if ($season && $claimed > 0) {
            $progress = UserBattlePassProgress::firstOrCreate(
                ['user_id' => $user->id, 'season_id' => $season->id],
                ['drive_coin_earned' => 0]
            );
            $progress->drive_coin_earned = (int) $progress->drive_coin_earned + $claimed;
            $progress->save();
        }

        return response()->json([
            'claimed' => $claimed,
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
}
