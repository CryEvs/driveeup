<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasTable('driveup_store_items')) {
            return;
        }

        Schema::table('driveup_store_items', function (Blueprint $table) {
            if (! Schema::hasColumn('driveup_store_items', 'item_type')) {
                $table->enum('item_type', ['DISCOUNT'])->default('DISCOUNT')->after('allowed_tier');
            }
            if (! Schema::hasColumn('driveup_store_items', 'discount_percent')) {
                $table->unsignedTinyInteger('discount_percent')->nullable()->after('item_type');
            }
        });
    }

    public function down(): void
    {
        if (! Schema::hasTable('driveup_store_items')) {
            return;
        }

        Schema::table('driveup_store_items', function (Blueprint $table) {
            if (Schema::hasColumn('driveup_store_items', 'discount_percent')) {
                $table->dropColumn('discount_percent');
            }
            if (Schema::hasColumn('driveup_store_items', 'item_type')) {
                $table->dropColumn('item_type');
            }
        });
    }
};

