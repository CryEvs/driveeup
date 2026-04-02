<?php

namespace App\Models;

// use Illuminate\Contracts\Auth\MustVerifyEmail;
use Database\Factories\UserFactory;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;

#[Fillable([
    'name', 'email', 'password', 'role', 'is_admin', 'drivee_coin', 'total_drive_coin', 'premium', 'api_token', 'avatar_url',
    'first_name', 'last_name', 'city', 'rating_avg', 'rides_count', 'vehicle_model', 'vehicle_plate',
    'next_ride_store_item_name',
])]
#[Hidden(['password', 'remember_token', 'api_token'])]
class User extends Authenticatable
{
    /** @use HasFactory<UserFactory> */
    use HasFactory, Notifiable;

    /**
     * Get the attributes that should be cast.
     *
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
            'premium' => 'boolean',
            'is_admin' => 'boolean',
            'drivee_coin' => 'float',
            'total_drive_coin' => 'float',
            'rating_avg' => 'float',
            'rides_count' => 'integer',
        ];
    }
}
