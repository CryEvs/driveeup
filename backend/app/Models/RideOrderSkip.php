<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class RideOrderSkip extends Model
{
    protected $fillable = [
        'ride_order_id',
        'driver_id',
    ];

    public function rideOrder(): BelongsTo
    {
        return $this->belongsTo(RideOrder::class, 'ride_order_id');
    }

    public function driver(): BelongsTo
    {
        return $this->belongsTo(User::class, 'driver_id');
    }
}
