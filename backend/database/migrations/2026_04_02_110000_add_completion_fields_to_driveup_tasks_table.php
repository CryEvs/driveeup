<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasTable('driveup_tasks')) {
            return;
        }

        Schema::table('driveup_tasks', function (Blueprint $table) {
            if (! Schema::hasColumn('driveup_tasks', 'completion_type')) {
                $table->enum('completion_type', ['RIDES', 'RATING', 'REFERRAL'])->default('RIDES')->after('description');
            }
            if (! Schema::hasColumn('driveup_tasks', 'required_rides_count')) {
                $table->unsignedInteger('required_rides_count')->nullable()->after('completion_type');
            }
        });
    }

    public function down(): void
    {
        if (! Schema::hasTable('driveup_tasks')) {
            return;
        }

        Schema::table('driveup_tasks', function (Blueprint $table) {
            if (Schema::hasColumn('driveup_tasks', 'required_rides_count')) {
                $table->dropColumn('required_rides_count');
            }
            if (Schema::hasColumn('driveup_tasks', 'completion_type')) {
                $table->dropColumn('completion_type');
            }
        });
    }
};

