-- -----------------------------------------------------------------------------
-- MIGRATION: Consolidação do cálculo de pontos (REGRAS_DE_NEGOCIO.md DIV-01, DIV-02, DIV-05)
--
-- 1. predicted_qualifier guarda o UUID do time escolhido no seletor (PAL-06).
--    Remove o CHECK ('home'/'away') da migration 20260629000000 e o substitui por
--    uma validação de que o valor é um dos dois times da partida.
-- 2. Traz para as migrations a regra vigente do bônus de classificação, que até
--    agora só existia no script solto realtime_fix.sql: vale apenas o seletor,
--    o placar palpitado é ignorado (PON-08).
-- 3. Os pontos passam a ser recalculados também quando placar, odds ou fase de
--    uma partida já encerrada são corrigidos (PON-09/PON-10). Antes, só a
--    transição para 'finished' recalculava.
--
-- Não recalcula partidas antigas. Para conferir pontos desatualizados:
--   SELECT p.id, p.points_earned, public.prediction_points(m, p) AS correto
--   FROM public.predictions p JOIN public.matches m ON m.id = p.match_id
--   WHERE m.status = 'finished'
--     AND p.points_earned IS DISTINCT FROM public.prediction_points(m, p);
-- E para corrigir uma partida:
--   SELECT public.recalculate_match_points('<match_id>');
-- -----------------------------------------------------------------------------

-- ── 1. predicted_qualifier = UUID do time ─────────────────────────────────────

ALTER TABLE public.predictions DROP CONSTRAINT IF EXISTS predictions_predicted_qualifier_check;

-- Converte valores legados 'home'/'away', se existirem, para o UUID do time.
UPDATE public.predictions p
SET predicted_qualifier = CASE p.predicted_qualifier
        WHEN 'home' THEN m.home_team_id::text
        WHEN 'away' THEN m.away_team_id::text
    END
FROM public.matches m
WHERE m.id = p.match_id
  AND p.predicted_qualifier IN ('home', 'away');

CREATE OR REPLACE FUNCTION public.validate_predicted_qualifier()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
    IF NEW.predicted_qualifier IS NULL THEN
        RETURN NEW;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.matches m
        WHERE m.id = NEW.match_id
          AND NEW.predicted_qualifier IN (m.home_team_id::text, m.away_team_id::text)
    ) THEN
        RAISE EXCEPTION 'predicted_qualifier deve ser o id de um dos times da partida'
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trigger_validate_predicted_qualifier ON public.predictions;
CREATE TRIGGER trigger_validate_predicted_qualifier
BEFORE INSERT OR UPDATE OF predicted_qualifier, match_id ON public.predictions
FOR EACH ROW
EXECUTE FUNCTION public.validate_predicted_qualifier();

-- ── 2. Cálculo de pontos de um palpite ────────────────────────────────────────
-- Espelho exato de PredictionCalculator.calculateEarnedPoints (Kotlin).
-- pontos = ROUND(base × multiplicador) + zebra + classificação

CREATE OR REPLACE FUNCTION public.prediction_points(m public.matches, p public.predictions)
RETURNS INT
LANGUAGE plpgsql
STABLE
SET search_path = public
AS $$
DECLARE
    final_home_90 INT;
    final_away_90 INT;
    pred_sign INT;
    actual_sign INT;
    base_points INT := 0;
    zebra_bonus INT := 0;
    qualifier_bonus INT := 0;
    winning_odd DECIMAL(5,2);
    actual_qualifier TEXT;
    user_pred_qualifier TEXT;
BEGIN
    -- Base e zebra usam o placar dos 90 min (PAR-08)
    final_home_90 := COALESCE(m.home_score_90, m.home_score);
    final_away_90 := COALESCE(m.away_score_90, m.away_score);

    IF final_home_90 IS NULL OR final_away_90 IS NULL THEN
        RETURN 0;
    END IF;

    pred_sign := sign(p.predicted_home - p.predicted_away);
    actual_sign := sign(final_home_90 - final_away_90);

    -- Base (PON-01 a PON-04) e zebra (PON-07): só com a tendência certa
    IF pred_sign = actual_sign THEN
        base_points := 1;
        IF p.predicted_home = final_home_90 THEN base_points := base_points + 2; END IF;
        IF p.predicted_away = final_away_90 THEN base_points := base_points + 2; END IF;

        IF actual_sign = 1 THEN winning_odd := m.home_odd;
        ELSIF actual_sign = -1 THEN winning_odd := m.away_odd;
        ELSE winning_odd := m.draw_odd; END IF;

        IF winning_odd IS NOT NULL AND winning_odd >= 3.00 THEN
            IF winning_odd >= 9.00 THEN zebra_bonus := 7;
            ELSIF winning_odd >= 5.00 THEN zebra_bonus := 4;
            ELSE zebra_bonus := 2;
            END IF;
        END IF;
    END IF;

    -- Bônus de classificação (PON-08): mata-mata empatado nos 90 min.
    -- Vale apenas o seletor; o placar palpitado é ignorado.
    IF m.is_knockout AND final_home_90 = final_away_90 THEN
        -- Quem avançou (PAR-09): pênaltis > prorrogação > placar final
        IF m.penalty_winner = 'home' THEN actual_qualifier := 'home';
        ELSIF m.penalty_winner = 'away' THEN actual_qualifier := 'away';
        ELSIF COALESCE(m.home_score_et, 0) > COALESCE(m.away_score_et, 0) THEN actual_qualifier := 'home';
        ELSIF COALESCE(m.away_score_et, 0) > COALESCE(m.home_score_et, 0) THEN actual_qualifier := 'away';
        ELSIF m.home_score > m.away_score THEN actual_qualifier := 'home';
        ELSIF m.away_score > m.home_score THEN actual_qualifier := 'away';
        END IF;

        IF p.predicted_qualifier = m.home_team_id::text THEN user_pred_qualifier := 'home';
        ELSIF p.predicted_qualifier = m.away_team_id::text THEN user_pred_qualifier := 'away';
        END IF;

        IF actual_qualifier IS NOT NULL AND user_pred_qualifier = actual_qualifier THEN
            qualifier_bonus := 2;
        END IF;
    END IF;

    -- ROUND() arredonda metade para cima (PON-06); bônus não são multiplicados
    RETURN ROUND(base_points * COALESCE(m.stage_multiplier, 1.0))::INT + zebra_bonus + qualifier_bonus;
END;
$$;

-- ── 3. Recalcular todos os palpites de uma partida encerrada ──────────────────

CREATE OR REPLACE FUNCTION public.recalculate_match_points(p_match_id UUID)
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    updated_count INT;
BEGIN
    UPDATE public.predictions p
    SET points_earned = public.prediction_points(m, p)
    FROM public.matches m
    WHERE m.id = p_match_id
      AND m.status = 'finished'
      AND p.match_id = m.id
      AND p.points_earned IS DISTINCT FROM public.prediction_points(m, p);

    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$;

-- Só o backend (service role / dashboard) pode disparar recálculo manual
REVOKE EXECUTE ON FUNCTION public.recalculate_match_points(UUID) FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE EXECUTE ON FUNCTION public.recalculate_match_points(UUID) FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE EXECUTE ON FUNCTION public.recalculate_match_points(UUID) FROM authenticated;
    END IF;
END $$;

-- ── 4. Trigger de pontuação ───────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.calculate_prediction_points()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF NEW.status = 'finished' THEN
        PERFORM public.recalculate_match_points(NEW.id);
    END IF;
    RETURN NEW;
END;
$$;

-- Dispara ao encerrar a partida e, depois de encerrada, sempre que mudar algum
-- dado que entra no cálculo. A update-live-matches regrava os mesmos valores a
-- cada execução; o IS DISTINCT FROM evita recalcular quando nada mudou.
DROP TRIGGER IF EXISTS trigger_calculate_prediction_points ON public.matches;
CREATE TRIGGER trigger_calculate_prediction_points
AFTER UPDATE ON public.matches
FOR EACH ROW
WHEN (
    NEW.status = 'finished' AND (
        OLD.status IS DISTINCT FROM 'finished'
        OR (OLD.home_score, OLD.away_score, OLD.home_score_90, OLD.away_score_90,
            OLD.home_score_et, OLD.away_score_et, OLD.penalty_winner,
            OLD.home_odd, OLD.draw_odd, OLD.away_odd,
            OLD.stage_multiplier, OLD.is_knockout, OLD.home_team_id, OLD.away_team_id)
           IS DISTINCT FROM
           (NEW.home_score, NEW.away_score, NEW.home_score_90, NEW.away_score_90,
            NEW.home_score_et, NEW.away_score_et, NEW.penalty_winner,
            NEW.home_odd, NEW.draw_odd, NEW.away_odd,
            NEW.stage_multiplier, NEW.is_knockout, NEW.home_team_id, NEW.away_team_id)
    )
)
EXECUTE FUNCTION public.calculate_prediction_points();
