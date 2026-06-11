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
| **Linguagem** | Kotlin 2.1.0 |
| **PWA / Web** | WebAssembly (WasmJs) |

## Funcionalidades Principais

- **Autenticação Segura**: Gerenciada nativamente com sessões persistidas via Supabase Auth.
- **Sincronismo em Tempo Real**: Os palpites e placares de ligas privadas são atualizados utilizando o motor de banco de dados e Supabase Realtime (PostgreSQL).
- **Dashboard Ao Vivo (Segunda Tela)**: Telas de liga reativas com carrossel dinâmico das partidas em andamento e mini-rankings instantâneos com a parcial dos apostadores, usando WebSockets de baixa latência (Supabase Channel Unique IDs).
- **Ligas Privadas**: Capacidade de criar ligas exclusivas, gerar links de convite (Share) e visualizar o Ranking (Leaderboard) de membros.
- **Progressive Web App (PWA)**: Compilado 100% em Kotlin para WebAssembly. Permite aos usuários instalarem o app diretamente pelo Safari do iOS, sem necessidade de passar pelas aprovações ou taxas da App Store.
- **Seletor Inteligente de Rodadas**: Aba de listagem dinâmica com Auto-Foco que desliza a tela automaticamente para a próxima partida não-finalizada, poupando tempo de navegação.

## Sistema de Pontuação (Zebra e Multiplicadores)

Toda a lógica central de cálculo de pontuações está **protegida no servidor (PostgreSQL)** através de funções (PL/pgSQL) e Triggers, de forma que as pontuações são atualizadas de forma autônoma sem processamento crítico no app do cliente.

### 1. Pontuação Base
O usuário *só pontua* se acertar primeiramente a **tendência do jogo** (quem venceu ou se foi empate).
- **Placar Exato (5 pts)**: Acertou a tendência (1 pt) + Gols exatos do Mandante (2 pts) + Gols exatos do Visitante (2 pts).
- **Tendência + Saldo (3 pts)**: Acertou a tendência correta + Gols exatos de apenas um dos times (Mandante OU Visitante).
- **Só Tendência (1 pt)**: Acertou apenas quem venceu (ou o empate), mas errou totalmente o placar.
- **Erro (0 pts)**: Errou o vencedor ou empate.

### 2. Multiplicador de Fase
A pontuação base calculada acima é multiplicada por um peso (`stage_multiplier`). Os multiplicadores são calibrados para valorizar as fases eliminatórias sem inflar demais os pontos:

| Fase | Multiplicador |
|------|--------------|
| Fase de Grupos / Rodadas normais | **×1** |
| 16-avos / Oitavas de Final | **×1.5** |
| Quartas de Final | **×2** |
| Semifinal | **×2** |
| Final | **×2.5** |

> O resultado final de pontos é sempre **arredondado para inteiro** (`ROUND`), garantindo que a pontuação exibida seja sempre um número inteiro mesmo com multiplicadores decimais (ex: 1 pt × 1.5 = 2 pts).

### 3. 🔥 Bônus de Zebra (Em Faixas)
O usuário pode receber um bônus de risco caso o seu palpite coincida com uma alta probabilidade de "Zebra" (Calculada usando as Odds Reais). Apenas se acertar a tendência e o evento for uma surpresa, ele ganha pontos extras:
- **Odd entre 3.00 e 4.99**: +2 pontos.
- **Odd entre 5.00 e 8.99**: +4 pontos.
- **Odd acima de 9.00**: +7 pontos.

## Qualidade e Testes

A segurança das regras de negócio do Bolão é garantida em duas camadas:
1. **Testes Unitários**: O projeto utiliza `kotlin-test` para garantir que o motor de cálculo (`PredictionCalculator`) obedeça a todos os cenários matemáticos possíveis.
2. **Automação (Git Hook)**: O repositório está configurado para barrar qualquer `git commit` caso um desenvolvedor quebre a lógica de pontuação. O `pre-commit` hook roda automaticamente a task `./gradlew testDebugUnitTest`.

## Ambientes e Configuração

O projeto é dividido em dois bancos de dados Supabase isolados. Crie um arquivo `local.properties` na raiz do projeto com o seguinte formato:

```properties
# Variáveis de Desenvolvimento (Debug)
SUPABASE_URL_DEV=https://[URL-DEV].supabase.co
SUPABASE_ANON_KEY_DEV=eyJh...

# Variáveis de Produção (Release)
SUPABASE_URL_PROD=https://[URL-PROD].supabase.co
SUPABASE_ANON_KEY_PROD=eyJh...

# Configuração da Keystore para Geração do Android App Bundle (.aab)
RELEASE_STORE_FILE=androidApp/bolao-release-key.keystore
RELEASE_STORE_PASSWORD=sua-senha
RELEASE_KEY_ALIAS=seu-alias
RELEASE_KEY_PASSWORD=sua-senha-da-chave
```

## Arquitetura e Servidor

```text
UI (Compose) ──► ViewModel ──► Repository ──► Supabase (Remote DB / Auth)
```

**Regra de Ouro:** O módulo cliente `shared` (Mobile/Web) **nunca** faz chamadas HTTP diretas às APIs de Futebol. O app fala única e exclusivamente com o nosso banco de dados no Supabase. 
- O sincronismo da base esportiva ocorre em **Edge Functions** escondidas:
  - `update-live-matches`: Atualiza os placares das partidas em andamento.
  - `update-upcoming-odds`: Congela e injeta os multiplicadores (Odds).
  - `sync-world-cup-schedule`: Mantém a agenda limpa, sincronizando o catálogo oficial da API.
- As partidas são **Congeladas no banco 48h antes**, garantindo a proteção contra manipulação das Odds da zebra.

## Compilando e Publicando

### 1. WebAssembly PWA (iOS Distribution sem App Store)
Para empacotar o aplicativo como um Progressive Web App rodando via WebAssembly, execute no terminal:
```bash
./gradlew clean :shared:wasmJsBrowserDistribution
```
Todos os binários, recursos HTML, JS e o ServiceWorker do PWA serão gerados na pasta: `shared/build/dist/wasmJs/productionExecutable/`. Hospede o conteúdo dessa pasta em qualquer provedor de hospedagem web.

### 2. Android App Bundle (Google Play Store)
A variante de release já vem ofuscada com o ProGuard/R8. Com o `local.properties` preenchido com as chaves da sua Keystore, gere o arquivo de produção executando:
```bash
./gradlew clean :androidApp:bundleRelease
```
O pacote `.aab` será gerado pronto para submissão no Google Play Console.

### 3. Deploy de Funções Supabase (Backend em Produção)
Para empacotar e enviar suas Edge Functions para a nuvem do Supabase, certifique-se de que o CLI está autenticado, faça o link do projeto de produção e execute:
```bash
npx supabase functions deploy sync-world-cup-schedule --no-verify-jwt
npx supabase functions deploy update-upcoming-odds --no-verify-jwt
```
