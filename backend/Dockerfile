FROM composer:2 AS vendor
WORKDIR /app
COPY composer.json composer.lock ./
RUN composer install --no-dev --no-interaction --prefer-dist --optimize-autoloader --no-scripts

FROM php:8.4-cli-alpine
WORKDIR /var/www/html
RUN apk add --no-cache icu-dev oniguruma-dev libzip-dev && docker-php-ext-install pdo_mysql mbstring intl
COPY --from=vendor /app/vendor ./vendor
COPY . .
RUN cp .env.example .env || true && chmod -R 775 storage bootstrap/cache
EXPOSE 8000
CMD ["sh", "-lc", "php artisan config:cache && php artisan route:cache && php artisan migrate --force && php artisan serve --host=0.0.0.0 --port=8000"]
