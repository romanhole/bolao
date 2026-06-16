-- Migration: Push Notifications support

-- Tabela para armazenar tokens FCM dos dispositivos dos usuários
CREATE TABLE IF NOT EXISTS public.push_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    TEXT        NOT NULL,
    token      TEXT        NOT NULL,
    platform   TEXT        NOT NULL DEFAULT 'android',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_token UNIQUE (user_id, token)
);

-- Habilitar RLS
ALTER TABLE public.push_tokens ENABLE ROW LEVEL SECURITY;

-- Políticas de RLS para push_tokens
-- 1. O próprio usuário pode inserir/atualizar seus tokens
CREATE POLICY "Users can manage their own push tokens"
    ON public.push_tokens
    FOR ALL
    USING (auth.uid()::text = user_id)
    WITH CHECK (auth.uid()::text = user_id);

-- 2. Service Role pode acessar tudo (necessário para a Edge Function ler os tokens e enviar push)
-- Nota: Service Role bypasses RLS by default, mas é bom deixar documentado ou ter regras caso algo mude.

-- Tabela para evitar envio duplicado de notificações para uma mesma partida/usuário
CREATE TABLE IF NOT EXISTS public.notifications_sent (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    TEXT        NOT NULL,
    match_id   UUID        NOT NULL REFERENCES public.matches(id) ON DELETE CASCADE,
    sent_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_match_notification UNIQUE (user_id, match_id)
);

-- RLS para notifications_sent
ALTER TABLE public.notifications_sent ENABLE ROW LEVEL SECURITY;

-- Somente o Service Role (Edge Function) deve inserir aqui. 
-- Como Service Role ignora RLS, não precisamos criar políticas explícitas, 
-- garantindo que usuários normais não consigam inserir ou alterar esses registros.

-- Ativar pg_cron para chamar a edge function a cada 30 minutos
-- OBS: Substitua <PROJECT_REF> se precisar hardcodar. Aqui tentaremos usar pg_net.
-- No entanto, o `pg_cron` com `pg_net` para chamar cloud functions requer a URL do projeto.
-- Sugestão: O próprio usuário pode registrar isso via dashboard caso a URL mude.
