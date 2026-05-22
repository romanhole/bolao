-- Migration: Baseline (Marco Zero)
-- Gerada autonomamente para espelhar o ambiente de Dev.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_cron";
CREATE EXTENSION IF NOT EXISTS "pg_net";

CREATE TABLE IF NOT EXISTS public.teams (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    api_team_id TEXT        UNIQUE,
    name        TEXT        NOT NULL UNIQUE,
    short_name  TEXT        NOT NULL UNIQUE,
    logo_url    TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.matches (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    api_fixture_id   TEXT        UNIQUE,
    home_team_id     UUID        NOT NULL REFERENCES public.teams(id) ON DELETE RESTRICT,
    away_team_id     UUID        NOT NULL REFERENCES public.teams(id) ON DELETE RESTRICT,
    home_score       INT,
    away_score       INT,
    status           TEXT        NOT NULL DEFAULT 'scheduled' CHECK (status IN ('scheduled', 'live', 'finished', 'cancelled')),
    minute_played    INT,
    scheduled_at     TIMESTAMPTZ NOT NULL,
    competition_id   TEXT        NOT NULL,
    competition      TEXT        NOT NULL,
    round            TEXT        NOT NULL,
    home_odd         DECIMAL(5,2),
    draw_odd         DECIMAL(5,2),
    away_odd         DECIMAL(5,2),
    stage_multiplier INT         DEFAULT 1,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_teams_different CHECK (home_team_id != away_team_id)
);

CREATE TABLE IF NOT EXISTS public.predictions (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id       UUID        NOT NULL REFERENCES public.matches(id) ON DELETE CASCADE,
    user_id        TEXT        NOT NULL,
    predicted_home INT         NOT NULL CHECK (predicted_home >= 0),
    predicted_away INT         NOT NULL CHECK (predicted_away >= 0),
    points_earned  INT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_match_user UNIQUE (match_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.leagues (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    invite_code TEXT        NOT NULL UNIQUE,
    owner_id    TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.league_members (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id   UUID        NOT NULL REFERENCES public.leagues(id) ON DELETE CASCADE,
    user_id     TEXT        NOT NULL,
    nickname    TEXT        NOT NULL,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_league_user UNIQUE (league_id, user_id),
    CONSTRAINT uq_league_nickname UNIQUE (league_id, nickname)
);

CREATE OR REPLACE FUNCTION public.calculate_prediction_points()
RETURNS trigger AS $$
DECLARE
    pred RECORD;
    pred_diff INT;
    actual_diff INT;
    pred_sign INT;
    actual_sign INT;
    base_points INT;
    zebra_bonus INT;
    winning_odd DECIMAL(5,2);
BEGIN
    IF NEW.status = 'finished' AND (OLD.status IS DISTINCT FROM 'finished') THEN
        FOR pred IN
            SELECT id, predicted_home, predicted_away
            FROM public.predictions
            WHERE match_id = NEW.id
        LOOP
            pred_diff := pred.predicted_home - pred.predicted_away;
            actual_diff := NEW.home_score - NEW.away_score;
            
            IF pred_diff > 0 THEN pred_sign := 1; ELSIF pred_diff < 0 THEN pred_sign := -1; ELSE pred_sign := 0; END IF;
            IF actual_diff > 0 THEN actual_sign := 1; ELSIF actual_diff < 0 THEN actual_sign := -1; ELSE actual_sign := 0; END IF;
            
            base_points := 0;
            zebra_bonus := 0;
            
            IF pred_sign = actual_sign THEN
                base_points := 1;
                IF pred.predicted_home = NEW.home_score THEN base_points := base_points + 2; END IF;
                IF pred.predicted_away = NEW.away_score THEN base_points := base_points + 2; END IF;
                
                IF actual_sign = 1 THEN winning_odd := NEW.home_odd;
                ELSIF actual_sign = -1 THEN winning_odd := NEW.away_odd;
                ELSE winning_odd := NEW.draw_odd; END IF;
                
                IF winning_odd IS NOT NULL AND winning_odd >= 3.00 THEN
                    IF winning_odd >= 9.00 THEN zebra_bonus := 7;
                    ELSIF winning_odd >= 5.00 THEN zebra_bonus := 4;
                    ELSE zebra_bonus := 2;
                    END IF;
                END IF;
            END IF;
            
            UPDATE public.predictions
            SET points_earned = (base_points * COALESCE(NEW.stage_multiplier, 1)) + zebra_bonus
            WHERE id = pred.id;
        END LOOP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_calculate_prediction_points ON public.matches;
CREATE TRIGGER trigger_calculate_prediction_points
AFTER UPDATE ON public.matches
FOR EACH ROW
EXECUTE FUNCTION public.calculate_prediction_points();

CREATE OR REPLACE VIEW public.league_leaderboard AS
SELECT
    lm.league_id,
    lm.user_id,
    lm.nickname,
    COALESCE(SUM(p.points_earned), 0) AS total_points,
    COUNT(p.id) AS total_predictions_made,
    COUNT(CASE WHEN p.points_earned >= 3 THEN 1 END) AS exact_matches
FROM public.league_members lm
LEFT JOIN public.predictions p ON p.user_id = lm.user_id
GROUP BY lm.league_id, lm.user_id, lm.nickname;

ALTER TABLE public.teams          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.matches        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.predictions    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leagues        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.league_members ENABLE ROW LEVEL SECURITY;

CREATE POLICY "teams_readable_by_all" ON public.teams FOR SELECT USING (true);
CREATE POLICY "matches_readable_by_all" ON public.matches FOR SELECT USING (true);
CREATE POLICY "leagues_select_all" ON public.leagues FOR SELECT USING (true);
CREATE POLICY "leagues_insert_own" ON public.leagues FOR INSERT WITH CHECK (owner_id = auth.uid()::text);
CREATE POLICY "league_members_select_all" ON public.league_members FOR SELECT USING (true);
CREATE POLICY "league_members_insert_own" ON public.league_members FOR INSERT WITH CHECK (user_id = auth.uid()::text);
CREATE POLICY "predictions_select_own" ON public.predictions FOR SELECT USING (user_id = auth.uid()::text);
CREATE POLICY "predictions_insert_own" ON public.predictions FOR INSERT WITH CHECK (user_id = auth.uid()::text);
CREATE POLICY "predictions_update_own" ON public.predictions FOR UPDATE USING (user_id = auth.uid()::text) WITH CHECK (user_id = auth.uid()::text);
