<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (!Schema::hasTable('battle_pass_seasons')) {
            Schema::create('battle_pass_seasons', function (Blueprint $table) {
                $table->id();
                $table->string('name');
                $table->timestamp('starts_at');
                $table->timestamp('ends_at');
                $table->boolean('finished_early')->default(false);
                $table->timestamp('finished_at')->nullable();
                $table->timestamps();
            });
        }

        if (!Schema::hasTable('battle_pass_levels')) {
            Schema::create('battle_pass_levels', function (Blueprint $table) {
                $table->id();
                $table->foreignId('season_id')->constrained('battle_pass_seasons')->cascadeOnDelete();
                $table->enum('role', ['PASSENGER', 'DRIVER']);
                $table->unsignedInteger('level_number');
                $table->unsignedBigInteger('required_drive_coin');
                $table->text('icon_url')->nullable();
                $table->text('description')->nullable();
                $table->timestamps();

                $table->unique(['season_id', 'role', 'level_number'], 'battle_pass_levels_unique_level');
            });
        }

        if (!Schema::hasTable('user_battle_pass_progress')) {
            Schema::create('user_battle_pass_progress', function (Blueprint $table) {
                $table->id();
                $table->foreignId('user_id')->constrained('users')->cascadeOnDelete();
                $table->foreignId('season_id')->constrained('battle_pass_seasons')->cascadeOnDelete();
                $table->unsignedBigInteger('drive_coin_earned')->default(0);
                $table->timestamps();

                $table->unique(['user_id', 'season_id']);
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('user_battle_pass_progress');
        Schema::dropIfExists('battle_pass_levels');
        Schema::dropIfExists('battle_pass_seasons');
    }
};
