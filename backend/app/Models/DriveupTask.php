<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class DriveupTask extends Model
{
    protected $table = 'driveup_tasks';

    protected $fillable = [
        'title',
        'description',
        'completion_type',
        'required_rides_count',
        'reward_drive_coin',
        'is_active',
        'sort_order',
    ];

    protected $casts = [
        'reward_drive_coin' => 'integer',
        'required_rides_count' => 'integer',
        'is_active' => 'boolean',
        'sort_order' => 'integer',
    ];
}

