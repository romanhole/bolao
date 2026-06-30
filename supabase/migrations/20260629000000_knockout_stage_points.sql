-- Migration: Knockout Stage Points
-- Adicionar campos de placar no tempo regulamentar, prorrogação e flag is_knockout
ALTER TABLE public.matches
  ADD COLUMN IF NOT EXISTS home_score_90 INT,
  ADD COLUMN IF NOT EXISTS away_score_90 INT,
  ADD COLUMN IF NOT EXISTS home_score_et INT,
  ADD COLUMN IF NOT EXISTS away_score_et INT,
  ADD COLUMN IF NOT EXISTS penalty_winner TEXT CHECK (penalty_winner IN ('home', 'away', NULL)),
  ADD COLUMN IF NOT EXISTS is_knockout BOOLEAN NOT NULL DEFAULT FALSE;

-- Atualizar o check constraint do status para suportar novos períodos
-- 'extratime' (prorrogação rolando), 'et_halftime' (intervalo da prorrogação), 'penalties' (pênaltis)
ALTER TABLE public.matches DROP CONSTRAINT IF EXISTS matches_status_check;
ALTER TABLE public.matches ADD CONSTRAINT matches_status_check 
  CHECK (status IN ('scheduled', 'live', 'halftime', 'finished', 'cancelled', 'interrupted', 'extratime', 'et_halftime', 'penalties'));

-- Atualizar restrições de minutos jogados para permitir minutos durante a prorrogação
-- e aumentar o limite máximo para 150 (considerando acréscimos na prorrogação)
ALTER TABLE public.matches DROP CONSTRAINT IF EXISTS chk_minute_played;
ALTER TABLE public.matches ADD CONSTRAINT chk_minute_played 
  CHECK (minute_played IS NULL OR status IN ('live', 'extratime', 'et_halftime'));

ALTER TABLE public.matches DROP CONSTRAINT IF EXISTS matches_minute_played_check;
ALTER TABLE public.matches ADD CONSTRAINT matches_minute_played_check 
  CHECK (minute_played >= 0 AND minute_played <= 150);

-- Adicionar predicted_qualifier na tabela predictions
ALTER TABLE public.predictions
  ADD COLUMN IF NOT EXISTS predicted_qualifier TEXT CHECK (predicted_qualifier IN ('home', 'away', NULL));

-- Trigger para definir automaticamente se é mata-mata (is_knockout)
CREATE OR REPLACE FUNCTION public.auto_set_is_knockout()
RETURNS trigger AS $$
BEGIN
    NEW.is_knockout := (
        NEW.round ILIKE '%Final%' OR
        NEW.round ILIKE '%Semi%' OR
        NEW.round ILIKE '%Quarta%' OR
        NEW.round ILIKE '%Oitava%' OR
        NEW.round ILIKE '%avos%' OR
        NEW.round ILIKE '%Terceiro%' OR
        NEW.round ILIKE '%Third%' OR
        NEW.round ILIKE '%3rd%'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_auto_is_knockout ON public.matches;
CREATE TRIGGER trigger_auto_is_knockout
BEFORE INSERT OR UPDATE OF round ON public.matches
FOR EACH ROW
EXECUTE FUNCTION public.auto_set_is_knockout();

-- Atualizar o matches existentes com base no round atual
UPDATE public.matches
SET is_knockout = (
    round ILIKE '%Final%' OR
    round ILIKE '%Semi%' OR
    round ILIKE '%Quarta%' OR
    round ILIKE '%Oitava%' OR
    round ILIKE '%avos%' OR
    round ILIKE '%Terceiro%' OR
    round ILIKE '%Third%' OR
    round ILIKE '%3rd%'
);

-- Atualizar função calculate_prediction_points
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
    qualifier_bonus INT;
    winning_odd DECIMAL(5,2);
    
    -- Usar score_90 se existir, senão usar o score final
    final_home_90 INT;
    final_away_90 INT;
    
    actual_qualifier TEXT;
BEGIN
    IF NEW.status = 'finished' AND (OLD.status IS DISTINCT FROM 'finished') THEN
        
        -- Definir qual placar usar para os 90 min (se não teve prorrogação, o 90 = final)
        final_home_90 := COALESCE(NEW.home_score_90, NEW.home_score);
        final_away_90 := COALESCE(NEW.away_score_90, NEW.away_score);
        
        -- Descobrir quem realmente classificou (caso empate nos 90 min)
        actual_qualifier := NULL;
        IF final_home_90 = final_away_90 AND NEW.is_knockout THEN
            IF NEW.penalty_winner = 'home' THEN
                actual_qualifier := 'home';
            ELSIF NEW.penalty_winner = 'away' THEN
                actual_qualifier := 'away';
            ELSIF COALESCE(NEW.home_score_et, 0) > COALESCE(NEW.away_score_et, 0) THEN
                actual_qualifier := 'home';
            ELSIF COALESCE(NEW.away_score_et, 0) > COALESCE(NEW.home_score_et, 0) THEN
                actual_qualifier := 'away';
            ELSIF NEW.home_score > NEW.away_score THEN 
                -- Fallback para home_score vs away_score (talvez API não preencheu _et separadamente, mas mudou o total)
                actual_qualifier := 'home';
            ELSIF NEW.away_score > NEW.home_score THEN
                actual_qualifier := 'away';
            END IF;
        END IF;

        FOR pred IN
            SELECT id, predicted_home, predicted_away, predicted_qualifier
            FROM public.predictions
            WHERE match_id = NEW.id
        LOOP
            pred_diff := pred.predicted_home - pred.predicted_away;
            actual_diff := final_home_90 - final_away_90;
            
            IF pred_diff > 0 THEN pred_sign := 1; ELSIF pred_diff < 0 THEN pred_sign := -1; ELSE pred_sign := 0; END IF;
            IF actual_diff > 0 THEN actual_sign := 1; ELSIF actual_diff < 0 THEN actual_sign := -1; ELSE actual_sign := 0; END IF;
            
            base_points := 0;
            zebra_bonus := 0;
            qualifier_bonus := 0;
            
            IF pred_sign = actual_sign THEN
                base_points := 1;
                IF pred.predicted_home = final_home_90 THEN base_points := base_points + 2; END IF;
                IF pred.predicted_away = final_away_90 THEN base_points := base_points + 2; END IF;
                
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
            
            -- Calcular Bônus de Classificado (independente se acertou a tendência)
            -- Nota: O bônus só conta se de fato o jogo terminou empatado nos 90min
            IF final_home_90 = final_away_90 AND NEW.is_knockout AND pred.predicted_qualifier IS NOT NULL AND actual_qualifier IS NOT NULL THEN
                IF pred.predicted_qualifier = actual_qualifier THEN
                    qualifier_bonus := 2;
                END IF;
            END IF;
            
            UPDATE public.predictions
            SET points_earned = (base_points * COALESCE(NEW.stage_multiplier, 1)) + zebra_bonus + qualifier_bonus
            WHERE id = pred.id;
        END LOOP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
