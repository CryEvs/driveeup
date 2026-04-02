<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class DriveupNextRideBenefit extends Model
{
    protected $table = 'driveup_next_ride_benefits';

    protected $fillable = [
        'tier',
        'benefit_text',
        'level_description',
        'updated_by_user_id',
    ];

    public function updatedBy(): BelongsTo
    {
        return $this->belongsTo(User::class, 'updated_by_user_id');
    }
}

