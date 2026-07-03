-- liquibase formatted sql

-- changeset trainingproject:add-oauth-user-column
ALTER TABLE user_details
    ADD COLUMN IF NOT EXISTS oauth_user BOOLEAN NOT NULL DEFAULT FALSE;
