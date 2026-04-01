<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class UserBattlePassLevelReward extends Model
{
    protected $fillable = [
        'user_id',
        'season_id',
        'level_id',
        'gift_name',
        'gift_description',
        'gift_type',
        'gift_drive_coin',
        'gift_text',
        'claimed_at',
    ];

    protected $casts = [
        'gift_drive_coin' => 'integer',
        'claimed_at' => 'datetime',
    ];

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    public function season(): BelongsTo
    {
        return $this->belongsTo(BattlePassSeason::class, 'season_id');
    }

    public function level(): BelongsTo
    {
        return $this->belongsTo(BattlePassLevel::class, 'level_id');
    }
}
