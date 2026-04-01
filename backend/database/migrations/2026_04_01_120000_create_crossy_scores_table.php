<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('crossy_scores', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->nullable()->constrained()->nullOnDelete();
            $table->string('display_name', 64);
            $table->unsignedInteger('score');
            $table->timestamps();

            $table->index(['score']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('crossy_scores');
    }
};
