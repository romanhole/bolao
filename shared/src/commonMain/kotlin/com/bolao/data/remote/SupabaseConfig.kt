package com.bolao.data.remote

/**
 * Configuração de acesso ao projeto Supabase.
 *
 * ## Como obter os valores
 * 1. Acesse o painel em https://supabase.com/dashboard
 * 2. Selecione seu projeto → **Settings → API**
 * 3. Copie os valores abaixo:
 *    - **Project URL** → [URL]
 *    - **Project API Keys → anon (public)** → [ANON_KEY]
 *
 * ## Segurança
 * A `anon key` é pública e pode ficar no código cliente.
 * Proteja os dados sensíveis configurando **Row Level Security (RLS)**
 * nas tabelas do Supabase — o DDL já inclui as políticas básicas.
 *
 * ## Produção
 * Para não expor as chaves no repositório, use um arquivo local
 * não versionado (ex: `local.properties`) e injete via `BuildConfig`.
 */
object SupabaseConfig {

    /**
     * URL do projeto Supabase.
     * Formato: "https://<PROJECT_REF>.supabase.co"
     *
     * Exemplo: "https://abcdefghijklmnop.supabase.co"
     */
    const val URL = "https://aecvtidljkwydmopbsgd.supabase.co"

    /**
     * Chave pública (anon) do Supabase.
     * Esta chave identifica o projeto e aplica as políticas de RLS.
     * NÃO use a `service_role` key no cliente — ela bypassa o RLS.
     */
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFlY3Z0aWRsamt3eWRtb3Bic2dkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkyMTg5OTUsImV4cCI6MjA5NDc5NDk5NX0.0k6bELat4llEYjcxuSP51K1F1UNb5rq4KQuzycOajbs"
}
