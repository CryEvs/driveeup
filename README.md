# DriveeUP

Текущий стек проекта:
- Android: Kotlin + Jetpack Compose + MVVM (каркас в `android-app`)
- Backend API: Laravel (PHP 8.3)
- Frontend Web: React (Vite)
- Web server: nginx
- SSL: certbot (Let's Encrypt)
- DB: MySQL + phpMyAdmin

## Почему так для сервера 2 GB RAM / 2 CPU

В `docker-compose.yml` уже добавлены ограничения `mem_limit` и `cpus` для всех сервисов, чтобы снизить риск OOM.

## Быстрый запуск

```bash
cp .env.example .env
docker compose up -d --build
```

Проверка:
- Web: `http://localhost`
- API: `http://localhost/api/auth/register`
- phpMyAdmin: `http://localhost:8081`

## API (Laravel)

- `POST /api/auth/register`
  - body: `{ "email": "...", "password": "...", "role": "PASSENGER|DRIVER" }`
- `POST /api/auth/login`
  - body: `{ "email": "...", "password": "..." }`
- `GET /api/auth/me`
  - header: `Authorization: Bearer <token>`

## Сертификаты для driveeup.ru через certbot

1. Убедиться, что DNS `driveeup.ru` и `www.driveeup.ru` указывают на сервер.
2. Поднять стек с обычным HTTP конфигом (уже по умолчанию):

```bash
docker compose up -d nginx backend frontend mysql
```

3. Выпустить сертификат:

```bash
docker compose run --rm certbot certonly --webroot -w /var/www/certbot -d driveeup.ru -d www.driveeup.ru --email you@driveeup.ru --agree-tos --no-eff-email
```

4. Переключить nginx на SSL конфиг:
- заменить монтирование `infra/nginx/default.conf` на `infra/nginx/default-ssl.conf` в `docker-compose.yml`
- перезапустить nginx:

```bash
docker compose up -d nginx
```
