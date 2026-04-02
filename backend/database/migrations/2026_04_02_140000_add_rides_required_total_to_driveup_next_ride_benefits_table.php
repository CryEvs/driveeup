<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasTable('driveup_next_ride_benefits')) {
            return;
        }

        Schema::table('driveup_next_ride_benefits', function (Blueprint $table) {
            if (! Schema::hasColumn('driveup_next_ride_benefits', 'rides_required_total')) {
                $table->unsignedInteger('rides_required_total')->nullable()->after('level_description');
            }
        });
    }

    public function down(): void
    {
        if (! Schema::hasTable('driveup_next_ride_benefits')) {
            return;
        }

        Schema::table('driveup_next_ride_benefits', function (Blueprint $table) {
            if (Schema::hasColumn('driveup_next_ride_benefits', 'rides_required_total')) {
                $table->dropColumn('rides_required_total');
            }
        });
    }
};

