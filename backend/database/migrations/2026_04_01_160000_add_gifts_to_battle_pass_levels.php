<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasTable('battle_pass_levels')) {
            Schema::table('battle_pass_levels', function (Blueprint $table) {
                if (! Schema::hasColumn('battle_pass_levels', 'gift_name')) {
                    $table->string('gift_name')->nullable()->after('description');
                }
                if (! Schema::hasColumn('battle_pass_levels', 'gift_description')) {
                    $table->text('gift_description')->nullable()->after('gift_name');
                }
                if (! Schema::hasColumn('battle_pass_levels', 'gift_drive_coin')) {
                    $table->unsignedBigInteger('gift_drive_coin')->default(0)->after('gift_description');
                }
            });
        }

        if (! Schema::hasTable('user_battle_pass_level_rewards')) {
            Schema::create('user_battle_pass_level_rewards', function (Blueprint $table) {
                $table->id();
                $table->foreignId('user_id')->constrained('users')->cascadeOnDelete();
                $table->foreignId('season_id')->constrained('battle_pass_seasons')->cascadeOnDelete();
                $table->foreignId('level_id')->constrained('battle_pass_levels')->cascadeOnDelete();
                $table->string('gift_name')->nullable();
                $table->text('gift_description')->nullable();
                $table->unsignedBigInteger('gift_drive_coin')->default(0);
                $table->timestamp('claimed_at');
                $table->timestamps();

                $table->unique(['user_id', 'level_id']);
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('user_battle_pass_level_rewards');

        if (Schema::hasTable('battle_pass_levels')) {
            Schema::table('battle_pass_levels', function (Blueprint $table) {
                if (Schema::hasColumn('battle_pass_levels', 'gift_drive_coin')) {
                    $table->dropColumn('gift_drive_coin');
                }
                if (Schema::hasColumn('battle_pass_levels', 'gift_description')) {
                    $table->dropColumn('gift_description');
                }
                if (Schema::hasColumn('battle_pass_levels', 'gift_name')) {
                    $table->dropColumn('gift_name');
                }
            });
        }
    }
};
