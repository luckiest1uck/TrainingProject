-- liquibase formatted sql

-- changeset example:add-ai-summary-to-product-reviews
ALTER TABLE product_reviews
    ADD COLUMN IF NOT EXISTS ai_summary TEXT;
