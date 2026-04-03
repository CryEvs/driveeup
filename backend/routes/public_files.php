<?php

/**
 * Публичные GET файлов (иконки) — регистрируются в bootstrap/app.php через `then`,
 * вне группы middleware `api`, чтобы не запускалась сессия.
 *
 * Если иконки висели в `routes/api.php`, вместе с `api` мог стартовать стек,
 * зависящий от сессии; при отсутствии файлов сессии на диске или cookie у клиента
 * ответ мог быть 404 вместо файла.
 */

use App\Http\Controllers\Api\AchievementController;
use App\Http\Controllers\Api\BattlePassController;
use Illuminate\Support\Facades\Route;

Route::get('/api/achievements/icons/{path}', [AchievementController::class, 'iconFile'])
    ->where('path', '.*');

Route::get('/api/battle-pass/level-icons/{path}', [BattlePassController::class, 'levelIcon'])
    ->where('path', '.*');
