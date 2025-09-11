## Вимоги

- Java 21

- Maven

- Telegram-бот (отримати токен у BotFather)

- OpenAI API ключ

- JSON файл Service Account Google Cloud

- PostgreSQL

## Запуск Telegram бота

Завантажити всі файли. 

Створити базу даних PostgreSQL

В папці resourse створити файл application.properties 

Cкопіювати application.properties.example вставити в application.properties і замінити значення ${______} на власні. 

Видалити application.properties.example

Додати в папку resourse ваш json файл з Google Cloud. 

Замінити ід докумета Google Docs в класі Bot на 68-му рядку (String documentId = "1ZLsi6yRa-IiCbDFsG04wKNpzOdP9sbg_oESyZ5fv00g";). 

Запустити програму з TestTelegramBotApplication. 

Перейти в телеграм і вести ім'я бота. 

Натиснути старт.

# Адмін панель

Щоб використати адмін панель потрібно або додати через міграцію в V2__add_admin.sql 

INSERT INTO users (id, affiliate, position, registration_status, role)
VALUES (YOUR_ID_CHAT, 'KYIV', 'MANAGE', 'ACTIVE', 'ADMIN');

або оновити права до ADMIN через міграцію в V3__update_user_to_admin.sql

UPDATE users
SET role = 'ADMIN'
WHERE id = YOUR_ID_CHAT;

