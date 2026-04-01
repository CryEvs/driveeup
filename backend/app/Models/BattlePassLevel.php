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
        'required_drive_coin',
        'icon_url',
        'description',
    ];

    protected $casts = [
        'level_number' => 'integer',
        'required_drive_coin' => 'integer',
    ];

    public function season(): BelongsTo
    {
        return $this->belongsTo(BattlePassSeason::class, 'season_id');
    }
}
