<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class BattlePassSeason extends Model
{
    protected $fillable = [
        'name',
        'starts_at',
        'ends_at',
        'finished_early',
        'finished_at',
    ];

    protected $casts = [
        'starts_at' => 'datetime',
        'ends_at' => 'datetime',
        'finished_early' => 'boolean',
        'finished_at' => 'datetime',
    ];

    public function levels(): HasMany
    {
        return $this->hasMany(BattlePassLevel::class, 'season_id');
    }
}
