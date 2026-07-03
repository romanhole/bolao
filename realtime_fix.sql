CREATE OR REPLACE FUNCTION public.calculate_prediction_points()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
AS $function$
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
    final_home_90 INT;
    final_away_90 INT;
    actual_qualifier TEXT;
    user_pred_qualifier TEXT;
BEGIN
    IF NEW.status = 'finished' AND (OLD.status IS DISTINCT FROM 'finished') THEN
        final_home_90 := COALESCE(NEW.home_score_90, NEW.home_score);
        final_away_90 := COALESCE(NEW.away_score_90, NEW.away_score);
        
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
                actual_qualifier := 'home';
            ELSIF NEW.away_score > NEW.home_score THEN
                actual_qualifier := 'away';
            END IF;
        END IF;

        FOR pred IN SELECT id, predicted_home, predicted_away, predicted_qualifier FROM public.predictions WHERE match_id = NEW.id LOOP
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
            
            IF final_home_90 = final_away_90 AND NEW.is_knockout AND actual_qualifier IS NOT NULL THEN
                user_pred_qualifier := NULL;
                
                -- O QUE IMPORTA É QUEM O USUÁRIO SELECIONOU NO CAMPO QUALIFICADOR (SELECTOR DE CONTINGÊNCIA)
                -- IGNORAMOS COMPLETAMENTE O PLACAR QUE ELE PALPITOU.
                IF pred.predicted_qualifier::text = NEW.home_team_id::text THEN
                    user_pred_qualifier := 'home';
                ELSIF pred.predicted_qualifier::text = NEW.away_team_id::text THEN
                    user_pred_qualifier := 'away';
                END IF;

                IF user_pred_qualifier = actual_qualifier THEN
                    qualifier_bonus := 2;
                END IF;
            END IF;
            
            UPDATE public.predictions
            SET points_earned = ROUND(base_points * COALESCE(NEW.stage_multiplier, 1)) + zebra_bonus + qualifier_bonus
            WHERE id = pred.id;
        END LOOP;
    END IF;
    RETURN NEW;
END;
$function$;

-- Recalcular pontos da Bélgica x Senegal para garantir que está certo agora:
UPDATE matches SET status = 'scheduled' WHERE id = 'd8ce6d1c-3777-4bfe-bcf6-ad35f6b3afa3';
UPDATE matches SET status = 'finished' WHERE id = 'd8ce6d1c-3777-4bfe-bcf6-ad35f6b3afa3';
