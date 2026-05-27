-- ==============================================================================
-- MIGRATION: CLEANUP TEST DATA (Feature Freeze v1.0)
-- 
-- Objetivo: Remover todos os dados de palpites e partidas de ligas de teste
-- (como Brasileirão e liga 22), mantendo o ambiente limpo apenas para a
-- Copa do Mundo (competition_id = 'copa_do_mundo_2026').
-- ==============================================================================

BEGIN;

-- 1. Deletar os palpites (predictions) associados às partidas que serão deletadas.
-- Fazemos isso primeiro para não violar as restrições de chave estrangeira (FK constraints).
DELETE FROM public.predictions
WHERE match_id IN (
    SELECT id FROM public.matches
    WHERE competition_id != 'copa_do_mundo_2026'
);

-- 2. Deletar as partidas (matches) que não são da Copa do Mundo.
DELETE FROM public.matches
WHERE competition_id != 'copa_do_mundo_2026';

-- Opcional: Não deletamos as equipes (teams) pois elas podem ser reaproveitadas 
-- no futuro ou podem já estar atreladas a algum jogo futuro da Copa (ex: Brasil).
-- Os usuários e ligas privadas são mantidos integralmente.

COMMIT;
