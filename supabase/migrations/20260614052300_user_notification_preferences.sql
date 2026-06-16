-- Migration to add customizable notification timing

ALTER TABLE public.push_tokens
  ADD COLUMN IF NOT EXISTS notification_hours_before INT NOT NULL DEFAULT 1;
