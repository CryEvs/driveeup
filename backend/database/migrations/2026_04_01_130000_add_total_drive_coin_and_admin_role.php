<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->unsignedBigInteger('total_drive_coin')->default(0)->after('drivee_coin');
        });

        DB::statement("ALTER TABLE users MODIFY role ENUM('PASSENGER','DRIVER','ADMIN') NOT NULL DEFAULT 'PASSENGER'");
    }

    public function down(): void
    {
        DB::statement("ALTER TABLE users MODIFY role ENUM('PASSENGER','DRIVER') NOT NULL DEFAULT 'PASSENGER'");

        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn('total_drive_coin');
        });
    }
};
