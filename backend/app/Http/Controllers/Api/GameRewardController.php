<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
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
        $user->save();

        return response()->json([
            'claimed' => $claimed,
            'driveeCoin' => (int) $user->drivee_coin,
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
