CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    affiliate VARCHAR(300),
    position VARCHAR(300),
    registration_status VARCHAR(30)
);

CREATE TABLE user_feedback (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    feedback VARCHAR(1000),
    criticality INT,
    recommendation VARCHAR(1000),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO users (id, affiliate, position, registration_status) VALUES
     (10001, 'KYIV', 'MECHANIK', 'ACTIVE'),
     (10002, 'LVIV', 'ELECTRIC', 'ACTIVE'),
     (10003, 'DNIPRO', 'MANAGER', 'ACTIVE'),
     (10004, 'UZHHOROD', 'MECHANIK', 'ACTIVE'),
     (10005, 'ZAPORIZHIA', 'ELECTRIC', 'ACTIVE'),
     (10006, 'KYIV', 'MANAGER', 'ACTIVE'),
     (10007, 'LVIV', 'MECHANIK', 'ACTIVE'),
     (10008, 'DNIPRO', 'ELECTRIC', 'ACTIVE'),
     (10009, 'UZHHOROD', 'MANAGER', 'ACTIVE'),
     (10010, 'ZAPORIZHIA', 'MECHANIK', 'ACTIVE');

INSERT INTO user_feedback (user_id, feedback, criticality, recommendation) VALUES
    (10001, 'Проблеми з обладнанням на виробництві.', 2, 'Перевірити стан машин.'),
    (10001, 'Не вистачає інструментів.', 3, 'Закупити додаткові інструменти.'),
    (10002, 'Електропроводка потребує модернізації.', 4, 'Звернутися до інженера.'),
    (10003, 'Низька продуктивність команди.', 5, 'Провести тренінги з менеджменту.'),
    (10004, 'Затримка з постачанням запчастин.', 3, 'Скласти новий графік поставок.'),
    (10005, 'Старе обладнання часто ламається.', 4, 'Запланувати оновлення.'),
    (10006, 'Комунікація між відділами слабка.', 2, 'Впровадити щотижневі наради.'),
    (10007, 'Не вистачає навчальних матеріалів.', 3, 'Підготувати інструкції.'),
    (10008, 'Перенавантаження електромережі.', 5, 'Залучити фахівців для перевірки.'),
    (10009, 'Складна система мотивації.', 4, 'Переглянути бонусну систему.'),
    (10010, 'Відсутність документації на процеси.', 3, 'Скласти внутрішні регламенти.');