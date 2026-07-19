-- liquibase formatted sql

-- changeset example:add-oauth-user-column
ALTER TABLE user_details
    ADD COLUMN IF NOT EXISTS oauth_user BOOLEAN NOT NULL DEFAULT FALSE;
