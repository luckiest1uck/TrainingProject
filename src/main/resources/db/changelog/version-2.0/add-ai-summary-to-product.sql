-- liquibase formatted sql

-- changeset example:add-ai-summary-to-product
ALTER TABLE product
    ADD COLUMN IF NOT EXISTS ai_summary TEXT;
