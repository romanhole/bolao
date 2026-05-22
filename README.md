# Bolão de Futebol — KMP App

Aplicativo de palpites esportivos Premium construído inteiramente com **Kotlin Multiplatform (KMP)** e **Compose Multiplatform**.

## Stack Tecnológico

| Camada | Tecnologia |
|---|---|
| **UI & UX** | Compose Multiplatform 1.7.3 (Material 3) |
| **Backend** | Supabase (PostgreSQL, Realtime, Auth, Edge Functions) |
| **Networking** | Ktor Client 3.0.3 |
| **Imagem & Mídia** | Kamel Image |
| **Serialização** | kotlinx.serialization 1.7.3 |
| **Coroutines** | kotlinx.coroutines 1.9.0 |
| **Injeção de Dep.** | Koin 4.0.0 |
| **Linguagem** | Kotlin 2.0.21 |

## Funcionalidades Principais

- **Autenticação Segura**: Gerenciada nativamente com sessões persistidas via Supabase Auth.
- **Sincronismo em Tempo Real**: Os palpites e placares de ligas privadas são atualizados utilizando o motor de banco de dados e Supabase Realtime (PostgreSQL).
- **Ligas Privadas**: Capacidade de criar ligas exclusivas, gerar links de convite (Share) e visualizar o Ranking (Leaderboard) de membros em tempo real.
- **Interface Rica**: BottomSheets interativos de explicação, cartões dinâmicos de apostas, suporte à edição segura apenas para partidas ativas.
- **Design Premium**: Estilo moderno utilizando Paleta Luxuosa (Gold/Green), fontes legíveis e painéis fluidos (Material 3 adaptado).

## Sistema de Pontuação (Zebra e Multiplicadores)

Toda a lógica central de cálculo de pontuações está **protegida no servidor (PostgreSQL)** através de funções (PL/pgSQL) e Triggers, de forma que as pontuações são atualizadas de forma autônoma sem processamento crítico no app do cliente.

### 1. Pontuação Base
O usuário *só pontua* se acertar primeiramente a **tendência do jogo** (quem venceu ou se foi empate).
- **Placar Exato (5 pts)**: Acertou a tendência (1 pt) + Gols exatos do Mandante (2 pts) + Gols exatos do Visitante (2 pts).
- **Tendência + Saldo (3 pts)**: Acertou a tendência correta + Gols exatos de apenas um dos times (Mandante OU Visitante).
- **Só Tendência (1 pt)**: Acertou apenas quem venceu (ou o empate), mas errou totalmente o placar.
- **Erro (0 pts)**: Errou o vencedor ou empate.

### 2. Multiplicador de Fase
A pontuação base calculada acima é multiplicada por um peso (`stage_multiplier`). Jogos normais valem x1, Quartas de final valem x2, Semi x3, etc.

### 3. 🔥 Bônus de Zebra (Em Faixas)
O usuário pode receber um bônus de risco caso o seu palpite coincida com uma alta probabilidade de "Zebra". Essa probabilidade é calculada a partir do mercado padrão de Odds Reais (1X2).
**Apenas se acertar a tendência (Vencedor ou Empate) e o evento for surpresa**, ele ganha pontos extras:
- **Odd entre 3.00 e 4.99**: +2 pontos.
- **Odd entre 5.00 e 8.99**: +4 pontos.
- **Odd acima de 9.00**: +7 pontos.

*A UI do app (MatchPredictionCard) exibe e calcula no front-end em tempo real (Real-Time Potential) quanto a aposta renderá no máximo se o resultado acontecer, inclusive alterando as cores e mostrando os ícones de zebra!*

## Arquitetura e Regra de Ouro

A arquitetura do App foi desenhada em camadas isoladas e desacopladas:

```text
UI (Compose) ──► ViewModel ──► Repository ──► Supabase (Remote DB / Auth)
```

**Regra de Ouro:** O módulo cliente `shared` (Mobile) **nunca** faz chamadas HTTP diretas às APIs de Futebol (como API-Football). O app fala única e exclusivamente com o nosso banco de dados. 
- O Sincronismo da base esportiva ocorre em **Edge Functions** escondidas que rodam rotinas em segundo plano todo dia.
- As partidas são **"Congeladas" 48h antes** do início do jogo, cravando no banco de dados a versão final da *Odd* que valerá os prêmios da Zebra, blindando o App de alterações suspeitas horas antes da partida começar.

## Como Abrir e Rodar

1. No Android Studio: `File → Open` → selecione a pasta raiz `bolao/`
2. Aguarde a sincronização completa do script Gradle.
3. Certifique-se de configurar a conexão com sua tabela Supabase.
4. Selecione a Run Configuration `androidApp` e faça o deploy no emulador ou aparelho físico.
