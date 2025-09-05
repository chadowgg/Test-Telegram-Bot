CREATE TABLE user_feedback (
    id SERIAL PRIMARY KEY,
    feedback VARCHAR(1000) NOT NULL,
    criticality INT NOT NULL,
    recommendation VARCHAR(1000)
);