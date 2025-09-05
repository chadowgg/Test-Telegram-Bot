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
