<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class RideOrder extends Model
{
    protected $fillable = [
        'passenger_id',
        'driver_id',
        'from_lat',
        'from_lon',
        'from_address',
        'to_lat',
        'to_lon',
        'to_address',
        'price_rub',
        'agreed_price_rub',
        'status',
        'driver_eta_minutes',
        'passenger_exiting',
        'passenger_rating',
        'driver_rating',
        'ride_coin_credited',
        'driver_ride_coin_credited',
    ];

    protected function casts(): array
    {
        return [
            'from_lat' => 'float',
            'from_lon' => 'float',
            'to_lat' => 'float',
            'to_lon' => 'float',
            'passenger_exiting' => 'boolean',
            'ride_coin_credited' => 'boolean',
            'driver_ride_coin_credited' => 'boolean',
        ];
    }

    public function passenger(): BelongsTo
    {
        return $this->belongsTo(User::class, 'passenger_id');
    }

    public function driver(): BelongsTo
    {
        return $this->belongsTo(User::class, 'driver_id');
    }
}
