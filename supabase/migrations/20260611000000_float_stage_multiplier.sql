-- Migration: Ajuste do tipo stage_multiplier de INT para DECIMAL(3,1)
-- Motivação: Suportar multiplicadores fracionais (1.5x, 2.0x, 2.5x) para fases do torneio.
-- Pontuação final continua inteira via ROUND() no trigger.

-- 1. Alterar tipo da coluna
ALTER TABLE public.matches
    ALTER COLUMN stage_multiplier TYPE DECIMAL(3,1)
    USING stage_multiplier::DECIMAL(3,1);

ALTER TABLE public.matches
    ALTER COLUMN stage_multiplier SET DEFAULT 1.0;

-- 2. Recriar a função de cálculo de pontos com suporte a multiplicador decimal
--    Diferença: (base_points * stage_multiplier) agora usa ROUND() para manter points_earned como INT
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

            -- ROUND() garante que points_earned continua INT mesmo com multiplicador decimal (ex: 1.5x)
            UPDATE public.predictions
            SET points_earned = ROUND(base_points * COALESCE(NEW.stage_multiplier, 1.0)) + zebra_bonus
            WHERE id = pred.id;
        END LOOP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Recriar o trigger (necessário após recriar a função)
DROP TRIGGER IF EXISTS trigger_calculate_prediction_points ON public.matches;
CREATE TRIGGER trigger_calculate_prediction_points
AFTER UPDATE ON public.matches
FOR EACH ROW
EXECUTE FUNCTION public.calculate_prediction_points();

-- ── Mapeamento oficial de fases → multiplicadores ─────────────────────────────
-- Execute estes UPDATEs manualmente conforme as partidas forem sendo cadastradas,
-- OU configure o campo stage_multiplier ao inserir/sincronizar as partidas:
--
-- Fase de Grupos / Rodadas Normais : stage_multiplier = 1.0
-- Oitavas de Final                 : stage_multiplier = 1.5
-- Quartas de Final                 : stage_multiplier = 2.0
-- Semifinal                        : stage_multiplier = 2.0
-- Final                            : stage_multiplier = 2.5
--
-- Exemplo de UPDATE para uma fase específica:
-- UPDATE public.matches SET stage_multiplier = 2.5 WHERE round ILIKE '%final%' AND round NOT ILIKE '%semi%' AND round NOT ILIKE '%quart%';
