<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class DriveupStoreItem extends Model
{
    protected $table = 'driveup_store_items';

    protected $fillable = [
        'name',
        'icon_url',
        'short_description',
        'allowed_tier',
        'description',
        'usage_terms',
        'validity_text',
        'price_drive_coin',
        'is_active',
        'sort_order',
    ];

    protected $casts = [
        'price_drive_coin' => 'integer',
        'is_active' => 'boolean',
        'sort_order' => 'integer',
    ];
}

