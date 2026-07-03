-- liquibase formatted sql

-- changeset trainingproject:add-ai-summary-to-product
ALTER TABLE product
    ADD COLUMN IF NOT EXISTS ai_summary TEXT;
