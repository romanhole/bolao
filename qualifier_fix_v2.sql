-- 1. Atualizar a trigger calculate_earned_points para inferir o qualifier do placar previsto
CREATE OR REPLACE FUNCTION public.calculate_earned_points()
RETURNS trigger AS $$
DECLARE
    pred RECORD;
    match RECORD;
    final_home_90 INT;
    final_away_90 INT;
    pred_diff INT;
    actual_diff INT;
    pred_sign INT;
    actual_sign INT;
    base_points INT := 0;
    zebra_bonus INT := 0;
    qualifier_bonus INT := 0;
    winning_odd NUMERIC;
    actual_qualifier TEXT;
    user_pred_qualifier TEXT;
BEGIN
    SELECT * INTO match FROM public.matches WHERE id = NEW.id;
    
    IF match.status != 'finished' THEN
        RETURN NEW;
    END IF;

    final_home_90 := COALESCE(match.home_score_90, match.home_score);
    final_away_90 := COALESCE(match.away_score_90, match.away_score);

    actual_qualifier := NULL;
    IF final_home_90 = final_away_90 AND match.is_knockout THEN
        IF match.penalty_winner = 'home' THEN
            actual_qualifier := 'home';
        ELSIF match.penalty_winner = 'away' THEN
            actual_qualifier := 'away';
        ELSIF COALESCE(match.home_score_et, 0) > COALESCE(match.away_score_et, 0) THEN
            actual_qualifier := 'home';
        ELSIF COALESCE(match.away_score_et, 0) > COALESCE(match.home_score_et, 0) THEN
            actual_qualifier := 'away';
        END IF;
    END IF;

    FOR pred IN SELECT * FROM public.predictions WHERE match_id = NEW.id LOOP
        pred_diff := pred.predicted_home - pred.predicted_away;
        actual_diff := final_home_90 - final_away_90;

        pred_sign := CASE WHEN pred_diff > 0 THEN 1 WHEN pred_diff < 0 THEN -1 ELSE 0 END;
        actual_sign := CASE WHEN actual_diff > 0 THEN 1 WHEN actual_diff < 0 THEN -1 ELSE 0 END;

        base_points := 0;
        IF pred_sign = actual_sign THEN
            base_points := 1;
            IF pred.predicted_home = final_home_90 THEN base_points := base_points + 2; END IF;
            IF pred.predicted_away = final_away_90 THEN base_points := base_points + 2; END IF;
        END IF;

        zebra_bonus := 0;
        IF pred_sign = actual_sign THEN
            IF actual_sign = 1 THEN winning_odd := match.home_odd;
            ELSIF actual_sign = -1 THEN winning_odd := match.away_odd;
            ELSE winning_odd := match.draw_odd;
            END IF;

            IF winning_odd >= 3.0 THEN
                IF winning_odd >= 9.0 THEN zebra_bonus := 7;
                ELSIF winning_odd >= 5.0 THEN zebra_bonus := 4;
                ELSE zebra_bonus := 2;
                END IF;
            END IF;
        END IF;

        qualifier_bonus := 0;
        IF final_home_90 = final_away_90 AND match.is_knockout AND actual_qualifier IS NOT NULL THEN
            user_pred_qualifier := NULL;
            IF pred.predicted_home > pred.predicted_away THEN
                user_pred_qualifier := 'home';
            ELSIF pred.predicted_away > pred.predicted_home THEN
                user_pred_qualifier := 'away';
            ELSE
                IF pred.predicted_qualifier::text = match.home_team_id::text THEN
                    user_pred_qualifier := 'home';
                ELSIF pred.predicted_qualifier::text = match.away_team_id::text THEN
                    user_pred_qualifier := 'away';
                END IF;
            END IF;

            IF user_pred_qualifier = actual_qualifier THEN
                qualifier_bonus := 2;
            END IF;
        END IF;

        UPDATE public.predictions
        SET points_earned = ROUND(base_points * match.stage_multiplier) + zebra_bonus + qualifier_bonus
        WHERE id = pred.id;
    END LOOP;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

UPDATE matches SET home_score_90 = home_score, away_score_90 = away_score WHERE api_fixture_id IN ('8361', '8362');
UPDATE matches SET penalty_winner = 'home' WHERE api_fixture_id = '8361' AND penalty_winner IS NULL;
UPDATE matches SET status = 'finished' WHERE api_fixture_id IN ('8361', '8362', '8367');
