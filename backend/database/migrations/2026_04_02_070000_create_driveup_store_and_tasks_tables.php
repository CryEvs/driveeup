<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasTable('driveup_store_items')) {
            Schema::create('driveup_store_items', function (Blueprint $table) {
                $table->id();
                $table->string('name', 255);
                $table->text('icon_url')->nullable();
                $table->text('short_description')->nullable();
                $table->enum('allowed_tier', ['ANY', 'SILVER', 'GOLD'])->default('ANY');
                $table->text('description')->nullable();
                $table->text('usage_terms')->nullable();
                $table->text('validity_text')->nullable();
                $table->unsignedBigInteger('price_drive_coin')->default(0);
                $table->boolean('is_active')->default(true);
                $table->unsignedInteger('sort_order')->default(0);
                $table->timestamps();
            });
        }

        if (! Schema::hasTable('driveup_tasks')) {
            Schema::create('driveup_tasks', function (Blueprint $table) {
                $table->id();
                $table->string('title', 255);
                $table->text('description')->nullable();
                $table->unsignedBigInteger('reward_drive_coin')->default(0);
                $table->boolean('is_active')->default(true);
                $table->unsignedInteger('sort_order')->default(0);
                $table->timestamps();
            });
        }

        if (! Schema::hasTable('driveup_next_ride_benefits')) {
            Schema::create('driveup_next_ride_benefits', function (Blueprint $table) {
                $table->id();
                $table->enum('tier', ['BRONZE', 'SILVER', 'GOLD'])->unique();
                $table->text('benefit_text');
                $table->foreignId('updated_by_user_id')->nullable()->constrained('users')->nullOnDelete();
                $table->timestamps();
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('driveup_next_ride_benefits');
        Schema::dropIfExists('driveup_tasks');
        Schema::dropIfExists('driveup_store_items');
    }
};

