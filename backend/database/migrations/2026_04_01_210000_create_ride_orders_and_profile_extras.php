<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            if (! Schema::hasColumn('users', 'first_name')) {
                $table->string('first_name', 120)->nullable()->after('name');
            }
            if (! Schema::hasColumn('users', 'last_name')) {
                $table->string('last_name', 120)->nullable()->after('first_name');
            }
            if (! Schema::hasColumn('users', 'city')) {
                $table->string('city', 120)->nullable()->after('last_name');
            }
            if (! Schema::hasColumn('users', 'rating_avg')) {
                $table->decimal('rating_avg', 3, 2)->default(5.00)->after('city');
            }
            if (! Schema::hasColumn('users', 'rides_count')) {
                $table->unsignedInteger('rides_count')->default(0)->after('rating_avg');
            }
            if (! Schema::hasColumn('users', 'vehicle_model')) {
                $table->string('vehicle_model', 120)->nullable()->after('rides_count');
            }
            if (! Schema::hasColumn('users', 'vehicle_plate')) {
                $table->string('vehicle_plate', 32)->nullable()->after('vehicle_model');
            }
        });

        if (! Schema::hasTable('ride_orders')) {
            Schema::create('ride_orders', function (Blueprint $table) {
                $table->id();
                $table->foreignId('passenger_id')->constrained('users')->cascadeOnDelete();
                $table->foreignId('driver_id')->nullable()->constrained('users')->nullOnDelete();
                $table->double('from_lat', 12, 8);
                $table->double('from_lon', 12, 8);
                $table->text('from_address');
                $table->double('to_lat', 12, 8);
                $table->double('to_lon', 12, 8);
                $table->text('to_address');
                $table->unsignedInteger('price_rub');
                $table->unsignedInteger('agreed_price_rub')->nullable();
                $table->string('status', 32)->default('searching');
                $table->unsignedTinyInteger('driver_eta_minutes')->nullable();
                $table->boolean('passenger_exiting')->default(false);
                $table->unsignedTinyInteger('passenger_rating')->nullable();
                $table->unsignedTinyInteger('driver_rating')->nullable();
                $table->timestamps();
                $table->index(['status', 'created_at']);
            });
        }

        if (! Schema::hasTable('ride_order_skips')) {
            Schema::create('ride_order_skips', function (Blueprint $table) {
                $table->id();
                $table->foreignId('ride_order_id')->constrained('ride_orders')->cascadeOnDelete();
                $table->foreignId('driver_id')->constrained('users')->cascadeOnDelete();
                $table->timestamps();
                $table->unique(['ride_order_id', 'driver_id']);
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('ride_order_skips');
        Schema::dropIfExists('ride_orders');

        Schema::table('users', function (Blueprint $table) {
            foreach (['vehicle_plate', 'vehicle_model', 'rides_count', 'rating_avg', 'city', 'last_name', 'first_name'] as $col) {
                if (Schema::hasColumn('users', $col)) {
                    $table->dropColumn($col);
                }
            }
        });
    }
};
