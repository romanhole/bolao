-- -----------------------------------------------------------------------------
-- MIGRATION: Seletor "quem avança" acompanha o lado quando o time da partida muda
-- (REGRAS_DE_NEGOCIO.md PAL-09 / DIV-13)
--
-- Partidas de mata-mata nascem com times provisórios (ex.: "W95" = vencedor do
-- jogo 95) e a sync-world-cup-schedule troca pelos times reais depois. Quem já
-- tinha palpitado escolheu o time provisório; sem este trigger a escolha fica
-- apontando para um time que não está mais na partida e o bônus de
-- classificação nunca é concedido.
--
-- Regra: se o time escolhido SAIU da partida, a escolha passa para o time que
-- entrou no mesmo lado (mandante → mandante, visitante → visitante). Se o time
-- escolhido continua na partida (ex.: mandante e visitante só trocaram de
-- lugar), a escolha não muda.
--
-- Se a partida já estiver encerrada, os pontos são recalculados depois do
-- remapeamento.
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.remap_predicted_qualifier_on_team_change()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    old_home TEXT := OLD.home_team_id::text;
    old_away TEXT := OLD.away_team_id::text;
    new_teams TEXT[] := ARRAY[NEW.home_team_id::text, NEW.away_team_id::text];
BEGIN
    UPDATE public.predictions p
    SET predicted_qualifier = CASE
            WHEN p.predicted_qualifier = old_home THEN NEW.home_team_id::text
            ELSE NEW.away_team_id::text
        END
    WHERE p.match_id = NEW.id
      AND (
            (p.predicted_qualifier = old_home AND NOT old_home = ANY (new_teams))
         OR (p.predicted_qualifier = old_away AND NOT old_away = ANY (new_teams))
      );

    -- O trigger de pontuação pode ter rodado antes deste (ordem alfabética),
    -- ainda com a escolha antiga; recalcula com a escolha remapeada.
    IF NEW.status = 'finished' THEN
        PERFORM public.recalculate_match_points(NEW.id);
    END IF;

    RETURN NEW;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.remap_predicted_qualifier_on_team_change() FROM PUBLIC;

CREATE OR REPLACE TRIGGER trigger_remap_predicted_qualifier
AFTER UPDATE OF home_team_id, away_team_id ON public.matches
FOR EACH ROW
WHEN (OLD.home_team_id IS DISTINCT FROM NEW.home_team_id
   OR OLD.away_team_id IS DISTINCT FROM NEW.away_team_id)
EXECUTE FUNCTION public.remap_predicted_qualifier_on_team_change();
