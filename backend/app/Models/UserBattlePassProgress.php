<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class UserBattlePassProgress extends Model
{
    protected $table = 'user_battle_pass_progress';

    protected $fillable = [
        'user_id',
        'season_id',
        'drive_coin_earned',
    ];

    protected $casts = [
        'drive_coin_earned' => 'float',
    ];

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    public function season(): BelongsTo
    {
        return $this->belongsTo(BattlePassSeason::class, 'season_id');
    }
}
