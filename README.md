# DriveeUP

Монорепозиторий сервиса такси/попутчиков: REST API на **Laravel**, веб-клиент на **React (Vite)**, мобильное приложение на **Android (Kotlin, Jetpack Compose)**. Инфраструктура поднимается через **Docker Compose** (MySQL, nginx, PHP backend, статический фронт, Certbot).

---

## Состав репозитория

| Каталог | Назначение |
|--------|------------|
| `backend/` | Laravel 13, PHP 8.3+. API `/api/*`, миграции, модели |
| `frontend/` | SPA: React 19, Vite, React Router, Zustand |
| `android-app/` | Клиент для водителя и пассажира (`ru.driveeup.mobile`) |
| `infra/nginx/` | Конфиг reverse proxy: HTTPS, `/api` → backend, остальное → фронт |

Подробности по отдельным частям: см. `backend/README.md`, `frontend/README.md`, `android-app/README.md` (локальные заметки; корневой README — точка входа по всему проекту).

---

## Быстрый старт (Docker)

Из **корня репозитория**:

```bash
cp .env.example .env   # при необходимости поправьте пароли БД
docker compose up -d --build
```

- **HTTP/HTTPS** — порты `80` и `443` (см. `docker-compose.yml` и `infra/nginx/default-ssl.conf`; для продакшена нужны сертификаты Let’s Encrypt).
- **API** с хоста: `https://<домен>/api/...` (через nginx проксируется на контейнер `backend`).
- **phpMyAdmin**: в типовой конфигурации nginx есть редирект; прямой доступ к сервису в compose: порт **8081** → контейнер `phpmyadmin`.

После первого подъёма при необходимости выполните миграции внутри контейнера backend:

```bash
docker compose exec backend php artisan migrate --force
```

---

## Переменные окружения

Корневой `.env.example` задаёт учётные данные MySQL для Compose (`DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_ROOT_PASSWORD`).  

В **`backend/.env`** (создаётся из `backend/.env.example` при сборке образа или вручную) настраиваются `APP_KEY`, параметры БД и URL приложения.

---

## API (кратко)

Базовый префикс: **`/api`**. Примеры маршрутов (см. `backend/routes/api.php`):

- **Авторизация**: регистрация, вход, профиль, смена роли, аватар  
- **Поездки**: создание заказа, лента/активные для водителя и пассажира, принятие, контрпредложение, этапы «прибыл / начало / завершение», выход пассажира, отмены, оценка  
- **Игры / Battle Pass**: начисления, сезоны и уровни (в т.ч. админ-эндпоинты)

Для запросов к защищённым методам используется токен (механизм — см. `AuthController` и клиент в Android).

---

## Веб-фронтенд (локальная разработка)

```bash
cd frontend
npm install
npm run dev
```

Сборка продакшена: `npm run build` — артефакты попадают в образ `frontend` (nginx раздаёт статику).

---

## Android

1. Откройте каталог **`android-app`** в Android Studio как проект (или импортируйте модуль `app`, если структура Gradle лежит у вас локально).
2. Укажите **базовый URL API** (например `https://driveeup.ru/api` для продакшена или `http://10.0.2.2/...` для эмулятора при локальном backend).
3. Стек: Jetpack Compose, MVVM, репозитории для auth и поездок, режимы водитель/пассажир.

Детали и исторические шаги импорта — в `android-app/README.md`.

---

## Полезные команды

| Задача | Команда |
|--------|---------|
| Пересобрать и поднять стек | `docker compose up -d --build` |
| Логи backend | `docker compose logs -f backend` |
| Artisan внутри backend | `docker compose exec backend php artisan ...` |

---

## Лицензия и вклад

Уточните лицензию в репозитории при публикации. Вопросы по безопасности API и секретам — через приватный канал владельца проекта.
