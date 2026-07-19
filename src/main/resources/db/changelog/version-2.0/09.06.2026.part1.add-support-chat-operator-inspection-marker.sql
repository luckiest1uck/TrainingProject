-- liquibase formatted sql

-- changeset example:add-support-chat-operator-inspection-marker
ALTER TABLE public.support_messages
    ADD COLUMN operator_inspection_required BOOLEAN NOT NULL DEFAULT FALSE;
