<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class BattlePassLevel extends Model
{
    protected $fillable = [
        'season_id',
        'role',
        'level_number',
        'title',
        'required_drive_coin',
        'icon_url',
        'description',
        'gift_name',
        'gift_description',
        'gift_type',
        'gift_drive_coin',
        'gift_text',
        'gift_promo_code',
    ];

    protected $casts = [
        'level_number' => 'integer',
        'required_drive_coin' => 'integer',
        'gift_drive_coin' => 'integer',
    ];

    public function season(): BelongsTo
    {
        return $this->belongsTo(BattlePassSeason::class, 'season_id');
    }
}
