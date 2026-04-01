<?php

use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\GameRewardController;
use Illuminate\Support\Facades\Route;

Route::post('/auth/register', [AuthController::class, 'register']);
Route::post('/auth/login', [AuthController::class, 'login']);
Route::get('/auth/me', [AuthController::class, 'me']);
Route::put('/auth/avatar', [AuthController::class, 'updateAvatar']);

Route::post('/game/claim-drivee-coin', [GameRewardController::class, 'claimDriveeCoin']);
