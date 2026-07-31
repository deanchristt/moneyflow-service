-- Optimistic-locking version column on every entity (from BaseEntity.@Version)
-- and the budget alert-notification bookkeeping column.

ALTER TABLE users                  ADD COLUMN IF NOT EXISTS version BIGINT;
ALTER TABLE teams                  ADD COLUMN IF NOT EXISTS version BIGINT;
ALTER TABLE accounts               ADD COLUMN IF NOT EXISTS version BIGINT;
ALTER TABLE categories             ADD COLUMN IF NOT EXISTS version BIGINT;
ALTER TABLE recurring_transactions ADD COLUMN IF NOT EXISTS version BIGINT;
ALTER TABLE transactions           ADD COLUMN IF NOT EXISTS version BIGINT;
ALTER TABLE budgets                ADD COLUMN IF NOT EXISTS version BIGINT;
ALTER TABLE team_members           ADD COLUMN IF NOT EXISTS version BIGINT;

ALTER TABLE budgets ADD COLUMN IF NOT EXISTS alert_sent_at TIMESTAMP;
