<?php

use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\BattlePassController;
use App\Http\Controllers\Api\DriveupController;
use App\Http\Controllers\Api\GameRewardController;
use App\Http\Controllers\Api\RideController;
use Illuminate\Support\Facades\Route;

Route::post('/auth/register', [AuthController::class, 'register']);
Route::post('/auth/login', [AuthController::class, 'login']);
Route::get('/auth/me', [AuthController::class, 'me']);
Route::put('/auth/avatar', [AuthController::class, 'updateAvatar']);
Route::put('/auth/profile', [AuthController::class, 'updateProfile']);
Route::put('/auth/role', [AuthController::class, 'setRole']);

Route::post('/rides', [RideController::class, 'store']);
Route::get('/rides/passenger/active', [RideController::class, 'passengerActive']);
Route::get('/rides/driver/active', [RideController::class, 'driverActive']);
Route::get('/rides/driver/feed', [RideController::class, 'driverFeed']);
Route::get('/rides/{id}', [RideController::class, 'show'])->whereNumber('id');
Route::post('/rides/{id}/skip', [RideController::class, 'skip'])->whereNumber('id');
Route::post('/rides/{id}/counter', [RideController::class, 'counter'])->whereNumber('id');
Route::post('/rides/{id}/accept', [RideController::class, 'accept'])->whereNumber('id');
Route::post('/rides/{id}/arrived', [RideController::class, 'arrived'])->whereNumber('id');
Route::post('/rides/{id}/start-trip', [RideController::class, 'startTrip'])->whereNumber('id');
Route::post('/rides/{id}/complete', [RideController::class, 'complete'])->whereNumber('id');
Route::post('/rides/{id}/passenger-exit', [RideController::class, 'passengerExit'])->whereNumber('id');
Route::post('/rides/{id}/cancel-passenger', [RideController::class, 'cancelPassenger'])->whereNumber('id');
Route::post('/rides/{id}/cancel-driver', [RideController::class, 'cancelDriver'])->whereNumber('id');
Route::post('/rides/{id}/rate', [RideController::class, 'rate'])->whereNumber('id');

Route::post('/game/claim-drivee-coin', [GameRewardController::class, 'claimDriveeCoin']);
Route::post('/game/claim-drive-coin', [GameRewardController::class, 'claimDriveeCoin']);

Route::get('/battle-pass/current', [BattlePassController::class, 'current']);
Route::get('/battle-pass/level-icons/{path}', [BattlePassController::class, 'levelIcon'])->where('path', '.*');
Route::post('/battle-pass/levels/{level}/claim-gift', [BattlePassController::class, 'claimLevelGift']);

Route::get('/driveup/content', [DriveupController::class, 'content']);
Route::get('/driveup/store/items', [DriveupController::class, 'storeItems']);
Route::post('/driveup/store/items/{item}/purchase', [DriveupController::class, 'purchaseStoreItem'])->whereNumber('item');
Route::get('/driveup/tasks', [DriveupController::class, 'tasks']);
Route::get('/driveup/next-ride-benefits', [DriveupController::class, 'nextRideBenefits']);
Route::get('/driveup/games/availability', [DriveupController::class, 'gamesAvailability']);

Route::get('/admin/battle-pass/seasons', [BattlePassController::class, 'adminSeasons']);
Route::post('/admin/battle-pass/seasons', [BattlePassController::class, 'createSeason']);
Route::put('/admin/battle-pass/seasons/{season}', [BattlePassController::class, 'updateSeason']);
Route::delete('/admin/battle-pass/seasons/{season}', [BattlePassController::class, 'deleteSeason']);
Route::post('/admin/battle-pass/seasons/{season}/finish', [BattlePassController::class, 'finishSeason']);
Route::post('/admin/battle-pass/levels/icon', [BattlePassController::class, 'uploadLevelIcon']);
Route::post('/admin/battle-pass/levels', [BattlePassController::class, 'createLevel']);
Route::put('/admin/battle-pass/levels/{level}', [BattlePassController::class, 'updateLevel']);
Route::delete('/admin/battle-pass/levels/{level}', [BattlePassController::class, 'deleteLevel']);

Route::get('/admin/store/items', [DriveupController::class, 'adminStoreItems']);
Route::post('/admin/store/items', [DriveupController::class, 'adminCreateStoreItem']);
Route::put('/admin/store/items/{item}', [DriveupController::class, 'adminUpdateStoreItem'])->whereNumber('item');
Route::delete('/admin/store/items/{item}', [DriveupController::class, 'adminDeleteStoreItem'])->whereNumber('item');

Route::get('/admin/tasks', [DriveupController::class, 'adminTasks']);
Route::post('/admin/tasks', [DriveupController::class, 'adminCreateTask']);
Route::put('/admin/tasks/{task}', [DriveupController::class, 'adminUpdateTask'])->whereNumber('task');
Route::delete('/admin/tasks/{task}', [DriveupController::class, 'adminDeleteTask'])->whereNumber('task');

Route::get('/admin/loyalty/next-ride-benefits', [DriveupController::class, 'adminNextRideBenefits']);
Route::put('/admin/loyalty/next-ride-benefits', [DriveupController::class, 'adminUpsertNextRideBenefit']);
