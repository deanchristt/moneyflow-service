-- Team-shared categories and team budgets

ALTER TABLE categories ADD COLUMN IF NOT EXISTS team_id BIGINT REFERENCES teams (id);
ALTER TABLE budgets    ADD COLUMN IF NOT EXISTS team_id BIGINT REFERENCES teams (id);
