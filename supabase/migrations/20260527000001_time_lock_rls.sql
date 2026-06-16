-- -----------------------------------------------------------------------------
-- MIGRATION: TIME LOCK RLS (Feature Freeze v1.0)
-- 
-- Bloqueio temporal de palpites: garante que ninguém possa enviar ou 
-- editar um palpite via API após o horário agendado (scheduled_at) do jogo.
-- -----------------------------------------------------------------------------

-- 1. Remove as regras antigas que não tinham trava de tempo (se existirem)
DROP POLICY IF EXISTS "predictions_insert_own" ON public.predictions;
DROP POLICY IF EXISTS "predictions_update_own" ON public.predictions;

-- 2. Recria a regra de INSERÇÃO com a trava do relógio
CREATE POLICY "predictions_insert_own" ON public.predictions 
FOR INSERT WITH CHECK (
    user_id = auth.uid()::text AND 
    EXISTS (
        SELECT 1 FROM public.matches 
        WHERE id = match_id AND scheduled_at > now()
    )
);

-- 3. Recria a regra de ATUALIZAÇÃO com a trava do relógio
CREATE POLICY "predictions_update_own" ON public.predictions 
FOR UPDATE USING (
    user_id = auth.uid()::text AND 
    EXISTS (
        SELECT 1 FROM public.matches 
        WHERE id = match_id AND scheduled_at > now()
    )
) WITH CHECK (
    user_id = auth.uid()::text AND 
    EXISTS (
        SELECT 1 FROM public.matches 
        WHERE id = match_id AND scheduled_at > now()
    )
);
