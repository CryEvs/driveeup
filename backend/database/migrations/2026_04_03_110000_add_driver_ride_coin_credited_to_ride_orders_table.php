<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasTable('ride_orders')) {
            return;
        }
        if (! Schema::hasColumn('ride_orders', 'driver_ride_coin_credited')) {
            Schema::table('ride_orders', function (Blueprint $table) {
                $table->boolean('driver_ride_coin_credited')->default(false);
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('ride_orders') && Schema::hasColumn('ride_orders', 'driver_ride_coin_credited')) {
            Schema::table('ride_orders', function (Blueprint $table) {
                $table->dropColumn('driver_ride_coin_credited');
            });
        }
    }
};
