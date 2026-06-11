-- Permite que membros de uma mesma liga vejam os palpites uns dos outros
DROP POLICY IF EXISTS "predictions_select_league_members" ON public.predictions;
CREATE POLICY "predictions_select_league_members" ON public.predictions
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.league_members lm_viewer
      JOIN public.league_members lm_owner ON lm_viewer.league_id = lm_owner.league_id
      WHERE lm_viewer.user_id = auth.uid()::text
        AND lm_owner.user_id = predictions.user_id
    )
  );
