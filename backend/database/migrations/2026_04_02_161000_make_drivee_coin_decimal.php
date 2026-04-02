<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasColumn('users', 'drivee_coin')) {
            DB::statement('ALTER TABLE users MODIFY drivee_coin DECIMAL(14,2) NOT NULL DEFAULT 0');
        }
        if (Schema::hasColumn('users', 'total_drive_coin')) {
            DB::statement('ALTER TABLE users MODIFY total_drive_coin DECIMAL(14,2) NOT NULL DEFAULT 0');
        }
        if (Schema::hasColumn('user_battle_pass_progress', 'drive_coin_earned')) {
            DB::statement('ALTER TABLE user_battle_pass_progress MODIFY drive_coin_earned DECIMAL(14,2) NOT NULL DEFAULT 0');
        }
    }

    public function down(): void
    {
        if (Schema::hasColumn('users', 'drivee_coin')) {
            DB::statement('ALTER TABLE users MODIFY drivee_coin BIGINT UNSIGNED NOT NULL DEFAULT 0');
        }
        if (Schema::hasColumn('users', 'total_drive_coin')) {
            DB::statement('ALTER TABLE users MODIFY total_drive_coin BIGINT UNSIGNED NOT NULL DEFAULT 0');
        }
        if (Schema::hasColumn('user_battle_pass_progress', 'drive_coin_earned')) {
            DB::statement('ALTER TABLE user_battle_pass_progress MODIFY drive_coin_earned BIGINT UNSIGNED NOT NULL DEFAULT 0');
        }
    }
};

