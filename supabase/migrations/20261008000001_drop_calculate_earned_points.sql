-- -----------------------------------------------------------------------------
-- MIGRATION: Remove calculate_earned_points (lógica revogada)
--
-- Função criada à mão em produção pelo antigo qualifier_fix_v2.sql. Ela deduz o
-- classificado pelo placar palpitado, regra revogada em 02/07/2026 (PON-08), e
-- nenhum trigger a usa. Removida para não ser reaproveitada por engano.
-- -----------------------------------------------------------------------------

DROP FUNCTION IF EXISTS public.calculate_earned_points();
