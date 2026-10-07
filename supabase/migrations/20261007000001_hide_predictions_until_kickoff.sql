-- -----------------------------------------------------------------------------
-- MIGRATION: Palpites de terceiros só visíveis após o início (REGRAS_DE_NEGOCIO.md DIV-04)
--
-- PAL-07: os palpites dos outros membros da liga ficam ocultos até a partida
-- começar. Até aqui isso só existia na UI; pela API qualquer membro conseguia
-- ler os palpites alheios antes do jogo.
--
-- O limite é o mesmo da trava de palpites (time_lock_rls): enquanto
-- scheduled_at > now() o dono pode editar e ninguém mais vê; a partir de
-- scheduled_at <= now() o palpite fica travado e visível para a liga.
--
-- O próprio usuário continua vendo os seus palpites pela policy
-- predictions_select_own. A view league_leaderboard não é afetada.
-- -----------------------------------------------------------------------------

DROP POLICY IF EXISTS "predictions_select_league_members" ON public.predictions;
CREATE POLICY "predictions_select_league_members" ON public.predictions
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.matches m
      WHERE m.id = predictions.match_id
        AND m.scheduled_at <= now()
    )
    AND EXISTS (
      SELECT 1 FROM public.league_members lm_viewer
      JOIN public.league_members lm_owner ON lm_viewer.league_id = lm_owner.league_id
      WHERE lm_viewer.user_id = auth.uid()::text
        AND lm_owner.user_id = predictions.user_id
    )
  );
