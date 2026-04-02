<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasTable('achievements')) {
            return;
        }

        Schema::table('achievements', function (Blueprint $table) {
            if (! Schema::hasColumn('achievements', 'award_type')) {
                $table->string('award_type', 32)->default('INITIAL')->after('description');
            }
            if (! Schema::hasColumn('achievements', 'rides_required')) {
                $table->unsignedInteger('rides_required')->nullable()->after('award_type');
            }
        });
    }

    public function down(): void
    {
        if (! Schema::hasTable('achievements')) {
            return;
        }

        Schema::table('achievements', function (Blueprint $table) {
            if (Schema::hasColumn('achievements', 'rides_required')) {
                $table->dropColumn('rides_required');
            }
            if (Schema::hasColumn('achievements', 'award_type')) {
                $table->dropColumn('award_type');
            }
        });
    }
};
