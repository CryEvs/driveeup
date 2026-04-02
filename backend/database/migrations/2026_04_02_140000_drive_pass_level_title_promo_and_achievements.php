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
                if (! Schema::hasColumn('battle_pass_levels', 'title')) {
                    $table->string('title')->nullable()->after('level_number');
                }
                if (! Schema::hasColumn('battle_pass_levels', 'gift_promo_code')) {
                    $table->string('gift_promo_code', 512)->nullable()->after('gift_text');
                }
            });
        }

        if (Schema::hasTable('user_battle_pass_level_rewards')) {
            Schema::table('user_battle_pass_level_rewards', function (Blueprint $table) {
                if (! Schema::hasColumn('user_battle_pass_level_rewards', 'gift_promo_code')) {
                    $table->string('gift_promo_code', 512)->nullable()->after('gift_text');
                }
            });
        }

        if (! Schema::hasTable('achievements')) {
            Schema::create('achievements', function (Blueprint $table) {
                $table->id();
                $table->string('title');
                $table->text('description')->nullable();
                $table->text('icon_url')->nullable();
                $table->unsignedInteger('sort_order')->default(0);
                $table->boolean('is_active')->default(true);
                $table->timestamps();
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('achievements');

        if (Schema::hasTable('user_battle_pass_level_rewards')) {
            Schema::table('user_battle_pass_level_rewards', function (Blueprint $table) {
                if (Schema::hasColumn('user_battle_pass_level_rewards', 'gift_promo_code')) {
                    $table->dropColumn('gift_promo_code');
                }
            });
        }

        if (Schema::hasTable('battle_pass_levels')) {
            Schema::table('battle_pass_levels', function (Blueprint $table) {
                if (Schema::hasColumn('battle_pass_levels', 'gift_promo_code')) {
                    $table->dropColumn('gift_promo_code');
                }
                if (Schema::hasColumn('battle_pass_levels', 'title')) {
                    $table->dropColumn('title');
                }
            });
        }
    }
};
