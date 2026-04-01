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
                if (! Schema::hasColumn('battle_pass_levels', 'gift_type')) {
                    $table->string('gift_type', 32)->default('DRIVECOIN')->after('gift_description');
                }
                if (! Schema::hasColumn('battle_pass_levels', 'gift_text')) {
                    $table->text('gift_text')->nullable()->after('gift_drive_coin');
                }
            });
        }

        if (Schema::hasTable('user_battle_pass_level_rewards')) {
            Schema::table('user_battle_pass_level_rewards', function (Blueprint $table) {
                if (! Schema::hasColumn('user_battle_pass_level_rewards', 'gift_type')) {
                    $table->string('gift_type', 32)->default('DRIVECOIN')->after('gift_description');
                }
                if (! Schema::hasColumn('user_battle_pass_level_rewards', 'gift_text')) {
                    $table->text('gift_text')->nullable()->after('gift_drive_coin');
                }
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('user_battle_pass_level_rewards')) {
            Schema::table('user_battle_pass_level_rewards', function (Blueprint $table) {
                if (Schema::hasColumn('user_battle_pass_level_rewards', 'gift_text')) {
                    $table->dropColumn('gift_text');
                }
                if (Schema::hasColumn('user_battle_pass_level_rewards', 'gift_type')) {
                    $table->dropColumn('gift_type');
                }
            });
        }

        if (Schema::hasTable('battle_pass_levels')) {
            Schema::table('battle_pass_levels', function (Blueprint $table) {
                if (Schema::hasColumn('battle_pass_levels', 'gift_text')) {
                    $table->dropColumn('gift_text');
                }
                if (Schema::hasColumn('battle_pass_levels', 'gift_type')) {
                    $table->dropColumn('gift_type');
                }
            });
        }
    }
};
