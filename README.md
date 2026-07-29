# 🚀 Система управления банковскими картами

## Стек
- Java 17, Spring Boot
- Spring Security + JWT (роли: **ADMIN**, **USER**)
- Spring Data JPA
- PostgreSQL
- Liquibase (миграции: `src/main/resources/db/migration/db.changelog-master.yaml`)
- Swagger UI / OpenAPI (`docs/openapi.yaml`)
- Docker Compose (dev)

## Быстрый запуск (Docker Compose)

> Требуется Docker + Docker Compose.

### Настройка переменных окружения

Создайте локальный файл `.env` из шаблона:

```bash
cp .env.example .env
```

Для Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Сгенерируйте два независимых Base64-ключа:

```bash
openssl rand -base64 32
openssl rand -base64 32
```

Добавьте первое значение как `JWT_SECRET`, второе — как
`AES_KEY_BASE64` в файл `.env`.

> Файл `.env` содержит локальные секреты и исключён из Git.
> В репозитории хранится только безопасный шаблон `.env.example`.

### Запуск приложения

```bash
docker compose up --build
```


После старта:
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

База по умолчанию:
- host: `localhost`
- port: `5432`
- db: `bankcards`
- user/pass: `bankcards / bankcards`

## Переменные окружения

- `JWT_SECRET` — обязательный секрет для подписи JWT;
- `AES_KEY_BASE64` — обязательный 256-битный AES-ключ в формате Base64;
- `JWT_EXP_MIN` — время жизни JWT в минутах, по умолчанию `60`;
- `JWT_ISSUER` — издатель JWT, по умолчанию `bankcards-api`;
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — параметры подключения к PostgreSQL.

## Тестовые учетные записи (создаются Liquibase)
- **ADMIN**: `admin / admin`
- **USER**: `user / user`

## Основные требования (реализовано)
- CRUD для карт (ADMIN)
- Просмотр своих карт с поиском и пагинацией (USER)
- Запрос блокировки карты (USER)
- Переводы между своими картами (USER)
- Ролевой доступ и JWT авторизация
- Шифрование номера карты в БД + маскирование в API (`**** **** **** 1234`)
- Валидация входных DTO + единый формат ошибок

## Особенности реализации

### Безопасность данных

- номера банковских карт хранятся в зашифрованном виде с использованием AES-GCM;
- для каждой операции шифрования создаётся отдельный вектор инициализации;
- в REST API возвращаются только маскированные номера карт;
- доступ к эндпоинтам разграничен через Spring Security и роли `USER` / `ADMIN`.

### Конкурентные переводы

Переводы между картами выполняются в рамках транзакции.

Для предотвращения конфликтов при одновременных операциях:

- записи карт получаются с пессимистической блокировкой;
- блокировки захватываются в фиксированном порядке по идентификатору карты;
- перед переводом проверяются владелец, статус карт и доступный баланс;
- изменение обоих балансов и сохранение перевода выполняются атомарно.

Такой подход снижает риск race condition, потери обновлений и взаимных блокировок при конкурентных переводах.

## Короткая справка по API
> Полная схема: `docs/openapi.yaml`

### Аутентификация
- `POST /api/auth/login` → JWT

### Карты (ADMIN)
- `POST /api/admin/cards`
- `GET /api/admin/cards` (фильтры: `search`, `status`, пагинация)
- `GET /api/admin/cards/{id}`
- `PATCH /api/admin/cards/{id}/status`
- `DELETE /api/admin/cards/{id}`

### Пользователи (ADMIN)
- `POST /api/admin/users`
- `GET /api/admin/users` (поиск `q`, пагинация)
- `PATCH /api/admin/users/{id}/role`
- `DELETE /api/admin/users/{id}`

### Карты (USER)
- `GET /api/cards` (фильтры: `search`/`q`, `status`, пагинация)
- `GET /api/cards/{id}`
- `GET /api/cards/{id}/balance`
- `POST /api/cards/{id}/block-request`

### Переводы (USER)
- `POST /api/transfers`

