# 🚀 Система управления банковскими картами (Bank Cards REST)

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
Можно переопределять через environment (см. `docker-compose.yml`):

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET` — секрет для подписи JWT (Base64 или обычная строка)
- `AES_KEY_BASE64` — AES-ключ для шифрования номера карты (Base64, 32 байта)

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

