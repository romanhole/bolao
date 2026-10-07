# Regras de Negócio do Bolão — Fonte de Verdade

> **Para qualquer IA ou pessoa que for alterar este projeto: leia este arquivo ANTES de mexer em pontuação, palpites, partidas, ligas, ranking, odds ou notificações.**
>
> 1. As regras aqui **prevalecem** sobre comentários no código, o `README.md`, o `CHANGELOG.md`, os textos de UI e os scripts SQL soltos na raiz.
> 2. Se um pedido de ajuste **conflitar** com uma regra deste arquivo, **pare e pergunte** ao dono do projeto antes de alterar. Nunca mude uma regra "de carona" em outra tarefa.
> 3. Quando uma regra for alterada por decisão do dono, **atualize este arquivo no mesmo commit** e registre a mudança no [Histórico de decisões](#13-histórico-de-decisões).
> 4. Várias regras estão implementadas em **mais de um lugar** (trigger SQL + calculadora Kotlin + tela de regras + Edge Function). Ao mudar uma, mude **todas** — veja o [Mapa de implementação](#11-mapa-de-implementação-onde-cada-regra-vive).
> 5. Se o código atual diverge deste arquivo, a divergência está listada em [Divergências conhecidas](#12-divergências-conhecidas-e-decisões-pendentes). Não "conserte" o arquivo para bater com o código: a regra escrita é a intenção.

Os identificadores de regra (ex.: `PON-03`) servem para referenciar em commits, PRs e conversas com a IA.

---

## 1. Escopo do produto

| ID | Regra |
|---|---|
| ESC-01 | O bolão cobre **apenas a Copa do Mundo 2026** (`competition_id = 'copa_do_mundo_2026'`, liga `27` na API Bzzoiro). Partidas de outras competições foram removidas (migration `20260527000000_cleanup_test_data`). |
| ESC-02 | Plataformas: Android (Google Play) e Web/PWA (WasmJs, usado também por quem tem iOS). Não há app na App Store. |
| ESC-03 | Todo o conteúdo voltado ao usuário é em **português do Brasil**. |

## 2. Princípios que também são regra de negócio

| ID | Regra |
|---|---|
| ARQ-01 | **Regra de Ouro:** o app (módulo `shared`) **nunca** chama APIs de futebol diretamente. Ele fala só com o Supabase. Dados esportivos entram exclusivamente pelas Edge Functions. |
| ARQ-02 | A **pontuação oficial** é calculada **no servidor** (trigger `calculate_prediction_points` no PostgreSQL) e gravada em `predictions.points_earned`. O cliente nunca escreve `points_earned`. |
| ARQ-03 | O cálculo no cliente (`PredictionCalculator`) existe só para **exibição** (potencial de pontos e parciais ao vivo) e deve ser **idêntico** ao do trigger. Qualquer mudança de pontuação exige alterar os dois e os testes de `PredictionCalculatorTest`. |
| ARQ-04 | O **relógio do servidor** é a autoridade para travar palpites (RLS). A checagem por relógio local no app é só conveniência de UI. |

---

## 3. Partidas

### 3.1 Fases (campo `matches.round`)

Nomes canônicos gravados pela Edge Function `sync-world-cup-schedule` e ordem das abas no app:

| Ordem | `round` | Mata-mata (`is_knockout`) | Multiplicador (`stage_multiplier`) |
|---|---|---|---|
| 1 | Fase de Grupos | não | ×1.0 |
| 2–4 | Rodada 1 / Rodada 2 / Rodada 3 | não | ×1.0 |
| 5 | 16-avos de final | **sim** | ×1.5 |
| 6 | Oitavas de final | **sim** | ×1.5 |
| 7 | Quartas de final | **sim** | ×2.0 |
| 8 | Semifinais | **sim** | ×2.0 |
| 9 | Terceiro Lugar | **sim** | ×1.0 (ver [DIV-07](#12-divergências-conhecidas-e-decisões-pendentes)) |
| 10 | Final | **sim** | ×2.5 |

| ID | Regra |
|---|---|
| PAR-01 | `stage_multiplier` e `is_knockout` são **derivados automaticamente do nome da fase** por triggers `BEFORE INSERT OR UPDATE OF round` (`auto_set_stage_multiplier`, `auto_set_is_knockout`). Não defina esses campos manualmente: o trigger sobrescreve. |
| PAR-02 | `stage_multiplier` é `DECIMAL(3,1)` (aceita 1.5, 2.5). |
| PAR-03 | Se a API não informar a fase, ela é deduzida pela **data do jogo** com o calendário oficial da Copa 2026 (`getPhaseByDate`). `group_name` no formato "Group X" sempre significa Fase de Grupos. |
| PAR-04 | Ao abrir a tela de palpites, a aba selecionada automaticamente é a da **primeira partida agendada ou ao vivo**; se não houver, a primeira fase da lista. |

### 3.2 Status

Valores válidos em `matches.status`: `scheduled`, `live`, `halftime`, `extratime`, `et_halftime`, `penalties`, `finished`, `cancelled`, `interrupted`.

| ID | Regra |
|---|---|
| PAR-05 | Uma partida **nunca** vira `live` antes do `scheduled_at` (trava de horário na `update-live-matches`): se a API disser que está em andamento antes da hora, o status fica `scheduled`. |
| PAR-06 | `cancelled`/`postponed` da API viram `interrupted` na `update-live-matches` (e `cancelled` na `sync-world-cup-schedule`). Partidas interrompidas não pontuam. |
| PAR-07 | A pontuação só é calculada para partidas `finished` (ver PON-09). |

### 3.3 Semântica dos campos de placar (definida em 29/09/2026)

| Campo | Significado |
|---|---|
| `home_score_90` / `away_score_90` | Placar do **tempo regulamentar (90 min + acréscimos)**. É o placar usado para pontuar. Vem de `event.home_score` da Bzzoiro. |
| `home_score_et` / `away_score_et` | Placar **acumulado ao final da prorrogação** (90 min + gols da prorrogação), **não** só os gols da prorrogação. Nulo se não houve prorrogação. |
| `home_score` / `away_score` | Placar "corrente/final" exibido: 90 min + gols da prorrogação. **Pênaltis nunca entram no placar.** |
| `penalty_winner` | `'home'`, `'away'` ou nulo. Derivado de `event.penalty_shootout`. |

| ID | Regra |
|---|---|
| PAR-08 | Se `home_score_90` for nulo, usa-se `home_score` como placar dos 90 min (idem visitante). |
| PAR-09 | **Quem se classificou** (só existe em mata-mata empatado nos 90 min), nesta ordem de prioridade: 1) `penalty_winner`; 2) maior placar de prorrogação (`*_score_et`); 3) maior placar final (`*_score`). |
| PAR-10 | No card, o placar exibido é o da prorrogação se existir; caso contrário o placar corrente. Em mata-mata empatado nos 90 min mostra-se "✓ TIME avançou". |

---

## 4. Palpites

| ID | Regra |
|---|---|
| PAL-01 | Um palpite por usuário por partida (`UNIQUE(match_id, user_id)`). Salvar de novo **atualiza** o palpite (upsert). |
| PAL-02 | Gols palpitados são inteiros **≥ 0** (constraint no banco e botões −/+ no app). |
| PAL-03 | **Janela de palpite:** só é possível criar ou editar **antes do `scheduled_at`** da partida. Garantido por RLS (`scheduled_at > now()`, migration `time_lock_rls`). No app, a edição também exige status `scheduled`. |
| PAL-04 | O palpite só existe depois que o usuário toca em **"Confirmar Palpite"**. Se ele confirmar sem mexer nos contadores, o palpite salvo é **0 x 0**. Sem palpite confirmado = **0 pontos** (não existe palpite automático). |
| PAL-05 | Em partidas de **mata-mata**, o card sempre mostra o seletor **"Em caso de empate, quem avança?"**, independentemente do placar palpitado. |
| PAL-06 | O seletor grava o **UUID do time** (`teams.id`) em `predictions.predicted_qualifier` — não `'home'`/`'away'`. O banco rejeita qualquer valor que não seja um dos dois times da partida (trigger `validate_predicted_qualifier`). |
| PAL-07 | **Visibilidade:** os palpites dos outros membros da liga ficam **ocultos até a partida começar**. Garantido no banco pela RLS `predictions_select_league_members`: palpites de terceiros só são legíveis quando `scheduled_at <= now()`, o mesmo instante em que o palpite trava (PAL-03). Na UI, o botão "Ver palpites da galera" só aparece quando a partida não está mais aberta a palpites. |
| PAL-08 | Um usuário só vê palpites de pessoas com quem compartilha **ao menos uma liga** (RLS `predictions_select_league_members`). |

---

## 5. Pontuação

### 5.1 Fórmula

```
pontos = ROUND(base × multiplicador_da_fase) + bônus_zebra + bônus_classificação
```

- O **multiplicador de fase incide só sobre a base**. Zebra e classificação são somados **sem multiplicar**.
- O resultado é sempre **inteiro**.

### 5.2 Base (PON-01 a PON-04)

A base é calculada **sempre com o placar dos 90 minutos** (PAR-08). Prorrogação e pênaltis não contam para a base.

| ID | Situação | Base |
|---|---|---|
| PON-01 | Errou a tendência (vitória mandante / empate / vitória visitante) | **0** — e não ganha zebra |
| PON-02 | Acertou a tendência | **1** |
| PON-03 | + acertou os gols do mandante | **+2** |
| PON-04 | + acertou os gols do visitante | **+2** |

Resultados possíveis da base: **0, 1, 3 ou 5**. Placar exato = 5.

### 5.3 Multiplicador de fase (PON-05)

Ver tabela da seção 3.1. Aplicado sobre a base e arredondado.

| ID | Regra |
|---|---|
| PON-06 | **Arredondamento: metade para cima** (`ROUND` do PostgreSQL / `roundToInt` do Kotlin). Ex.: 1×1.5 = 1.5 → **2**; 3×1.5 = 4.5 → **5**; 5×1.5 = 7.5 → **8**; 1×2.5 = 2.5 → **3**; 3×2.5 = 7.5 → **8**; 5×2.5 = 12.5 → **13**. |

### 5.4 Bônus de Zebra (PON-07)

Só se o usuário **acertou a tendência**. Usa a odd do resultado que aconteceu (que é o mesmo que o usuário palpitou):

| Odd do resultado | Bônus |
|---|---|
| < 3.00 | +0 |
| **≥ 3.00** e < 5.00 | **+2** |
| **≥ 5.00** e < 9.00 | **+4** |
| **≥ 9.00** | **+7** |

- Limites são **inclusivos** (odd 3.00 já é zebra; odd 9.00 já vale +7).
- Odd nula = sem bônus.
- O bônus é fixo: **não** é multiplicado pela fase.

### 5.5 Bônus de Classificação — mata-mata (PON-08) — decidido em 02/07/2026

| Regra | Detalhe |
|---|---|
| Valor | **+2 pontos**, fixos, **não** multiplicados pela fase. |
| Quando existe | Somente se a partida é de mata-mata **e** o placar dos **90 min terminou empatado** e o classificado é conhecido (PAR-09). Se alguém venceu nos 90 min, ninguém ganha esse bônus. |
| Critério | **Vale apenas o que o usuário escolheu no seletor** "quem avança" (`predicted_qualifier`). O placar palpitado é **completamente ignorado** para este bônus. |
| Independência | É **independente da tendência**: o usuário pode errar o placar/tendência e ainda ganhar os +2. |
| Sem seleção | `predicted_qualifier` nulo = sem bônus. |

> ⚠️ **Decisão revogada — não reintroduzir:** em 30/06/2026 existiu uma versão (`qualifier_fix_v2.sql`, hoje só no histórico do git) que **deduzia o classificado a partir do placar palpitado** (se palpitou 2x1 para o mandante, considerava o mandante como escolhido) e só usava o seletor em palpites de empate. Isso foi **substituído** pela regra acima ("prioridade do seletor").

### 5.6 Quando a pontuação é gravada (PON-09)

| ID | Regra |
|---|---|
| PON-09 | O trigger `trigger_calculate_prediction_points` (AFTER UPDATE em `matches`) recalcula **todos os palpites da partida** quando o `status` passa a ser `finished` **e também**, com a partida já encerrada, sempre que muda algum dado que entra no cálculo: placares (90', prorrogação, final), `penalty_winner`, odds, `stage_multiplier`, `is_knockout` ou times. Correções de placar feitas pela API depois do apito final atualizam os pontos automaticamente. |
| PON-10 | Recálculo manual, quando necessário: `SELECT public.recalculate_match_points('<match_id>');` (só backend/dashboard; usuários não podem executar). Não use mais o truque de trocar o status para `scheduled` e voltar para `finished`. Para listar palpites com pontos desatualizados, veja o comentário no topo da migration `20261007000000_scoring_trigger_consolidation.sql`. |

### 5.7 Casos de referência (use como teste)

| # | Fase (mult.) | Palpite | Seletor | Real 90' | Prorr./Pên. | Odds (M/E/V) | Pontos | Por quê |
|---|---|---|---|---|---|---|---|---|
| 1 | Grupos ×1 | 2x1 | — | 2x1 | — | 1.5/3.5/5.0 | **5** | exato |
| 2 | Grupos ×1 | 3x1 | — | 2x1 | — | 1.5/3.5/5.0 | **3** | tendência + gols visitante |
| 3 | Grupos ×1 | 3x0 | — | 2x1 | — | 1.5/3.5/5.0 | **1** | só tendência |
| 4 | Grupos ×1 | 1x2 | — | 2x1 | — | 1.5/3.5/5.0 | **0** | errou tendência |
| 5 | Grupos ×1 | 1x1 | — | 1x1 | — | 2.0/3.5/4.0 | **7** | 5 + zebra empate (3.5 → +2) |
| 6 | Grupos ×1 | 1x0 | — | 1x0 | — | 10.0/4.0/1.1 | **12** | 5 + zebra (≥9 → +7) |
| 7 | Oitavas ×1.5 | 3x0 | — | 2x1 | — | 1.5/3.5/5.0 | **2** | ROUND(1×1.5) |
| 8 | Final ×2.5 | 0x1 | — | 0x1 | — | 1.3/4.0/6.0 | **17** | ROUND(12.5)=13 + 4 |
| 9 | Oitavas ×1.5 | 1x1 | Mandante | 1x1 | Mandante nos pênaltis | 2.0/3.4/3.0 | **12** | 8 + zebra 2 + classif. 2 |
| 10 | Oitavas ×1.5 | 2x1 | Mandante | 1x1 | Mandante nos pênaltis | 2.0/3.4/3.0 | **2** | errou tendência (0), mas acertou o seletor (+2) |
| 11 | Oitavas ×1.5 | 2x1 | Visitante | 1x1 | Mandante nos pênaltis | 2.0/3.4/3.0 | **0** | o placar 2x1 **não** conta como escolher o mandante |
| 12 | Oitavas ×1.5 | 1x1 | Mandante | 2x1 | — | 2.0/3.4/3.0 | **0** | errou tendência; sem empate nos 90' não há bônus |
| 13 | Quartas ×2 | 1x1 | Visitante | 1x1 | 1x2 na prorrogação | 2.0/3.0/4.0 | **14** | 10 + zebra 2 + classif. 2 (base usa 1x1, não 1x2) |

### 5.8 Potencial exibido antes do jogo (PON-11)

No card de palpite aberto, o app mostra o **potencial máximo** = `ROUND(5 × multiplicador) + zebra da tendência palpitada`. Se as odds ainda não existem, mostra o aviso de que os multiplicadores de zebra ainda não foram liberados. Quando há zebra, o texto destaca "ZEBRA!".

---

## 6. Odds

| ID | Regra |
|---|---|
| ODD-01 | Odds (`home_odd`, `draw_odd`, `away_odd`) vêm da Bzzoiro via Edge Function `update-upcoming-odds`, para partidas `scheduled` nos **próximos 7 dias**. |
| ODD-02 | **Congelamento de 48 h:** a menos de 48 h do jogo, odds já gravadas **não são mais atualizadas**. Isso protege o bônus de zebra contra manipulação. |
| ODD-03 | Exceção: se a partida ainda **não tem odd** a menos de 48 h, ela **é buscada** mesmo assim (para não zerar a zebra). |
| ODD-04 | Só grava se as **três** odds vierem preenchidas. |
| ODD-05 | A zebra usa as odds **gravadas no momento do encerramento** da partida — não as do momento em que o usuário palpitou. |

---

## 7. Ligas e ranking

### 7.1 Ligas

| ID | Regra |
|---|---|
| LIG-01 | Qualquer usuário logado pode **criar** uma liga. O criador vira `owner_id` e é inserido automaticamente como membro, com o apelido informado. |
| LIG-02 | Código de convite: **6 caracteres**, letras maiúsculas A–Z e dígitos 0–9, único. Ao entrar, o código digitado é convertido para maiúsculas e sem espaços nas pontas. |
| LIG-03 | Para entrar ou criar é obrigatório informar **nome da liga/código** e **apelido**, ambos não vazios. |
| LIG-04 | O **apelido é por liga** e único dentro da liga (`uq_league_nickname`). Mensagem: "Este apelido já está em uso nesta liga." |
| LIG-05 | Um usuário só pode estar uma vez em cada liga (`uq_league_user`). Mensagem: "Você já participa desta liga." |
| LIG-06 | Não há limite de ligas por usuário nem de membros por liga. |
| LIG-07 | O convite é compartilhado pelo botão de compartilhar da liga (código de convite). |

### 7.2 Ranking da liga

| ID | Regra |
|---|---|
| RAN-01 | **Um palpite vale para todas as ligas.** O usuário palpita uma vez por partida e esses pontos contam em todas as ligas de que participa. |
| RAN-02 | Os pontos da liga são a **soma de todos os palpites** do usuário na competição, inclusive de partidas anteriores à sua entrada na liga (view `league_leaderboard`). |
| RAN-03 | **Ordenação:** total de pontos (desc); **desempate:** quantidade de placares exatos (desc). |
| RAN-04 | Placar exato, para desempate, conta apenas partidas `finished` em que o palpite bate com o placar (ver [DIV-06](#12-divergências-conhecidas-e-decisões-pendentes)). |
| RAN-05 | O usuário logado aparece **destacado** no ranking e nas listas de palpites. |

### 7.3 Ao vivo

| ID | Regra |
|---|---|
| VIV-01 | Enquanto houver partida ao vivo, a tela da liga mostra um carrossel "AO VIVO" com a **pontuação parcial** de cada membro naquela partida, calculada no cliente com as mesmas regras da seção 5 usando o placar atual. |
| VIV-02 | O ranking da liga **soma as parciais ao vivo** ao total oficial, reordenando em tempo real. Quando a partida encerra, a parcial é substituída pelo `points_earned` oficial. |
| VIV-03 | Na lista "palpites da galera" de uma partida, a ordem é: pontos parciais (desc) e, no empate, quem chegou mais perto do placar (menor soma das diferenças de gols). |

---

## 8. Notificações push

| ID | Regra |
|---|---|
| NOT-01 | O lembrete é enviado só para quem: (a) participa de **ao menos uma liga**, (b) **ainda não palpitou** naquela partida e (c) **ainda não foi lembrado** daquela partida. |
| NOT-02 | **No máximo um lembrete por usuário por partida** (tabela `notifications_sent`). |
| NOT-03 | Antecedência configurável pelo usuário: **1, 2, 3, 6, 12 ou 24 horas** antes do jogo. Padrão: **1 hora**. |
| NOT-04 | Texto: título "Vai esquecer de palpitar?", corpo "O jogo X x Y está quase começando! Corra para não perder seus pontos." |
| NOT-05 | Tokens FCM inválidos (`UNREGISTERED`/`NOT_FOUND`) são apagados. |

---

## 9. Conta e app

| ID | Regra |
|---|---|
| APP-01 | Login por e-mail e senha (Supabase Auth). Senha com **no mínimo 6 caracteres**; no cadastro a confirmação precisa ser igual. |
| APP-02 | Recuperação de senha por **código OTP** enviado por e-mail (não magic link). |
| APP-03 | **Atualização forçada:** `app_settings.min_version_code` > versão instalada → diálogo **bloqueante**. `latest_version_code` > versão instalada → sugestão **dispensável**. Checado também ao voltar o app para frente. |
| APP-04 | Tutorial de onboarding mostrado uma única vez por dispositivo. |

---

## 10. Glossário

- **Tendência:** quem venceu ou se houve empate (1X2).
- **Zebra:** resultado improvável, medido pela odd do resultado (≥ 3.00).
- **Mata-mata:** partida com `is_knockout = true` (16-avos em diante, incluindo terceiro lugar).
- **Classificado:** quem avança num mata-mata empatado nos 90 minutos.
- **Seletor:** campo "Em caso de empate, quem avança?" do card de mata-mata.

---

## 11. Mapa de implementação (onde cada regra vive)

Ao mudar uma regra de pontuação, **todos** os itens da linha precisam mudar juntos.

| Regra | Onde está |
|---|---|
| Fórmula de pontos (base, zebra, multiplicador, classificação) | **Oficial:** função SQL `public.prediction_points(match, prediction)`, chamada pelo trigger `calculate_prediction_points` via `recalculate_match_points` (migration `20261007000000_scoring_trigger_consolidation.sql`). Os casos da seção 5.7 foram validados contra ela. **Espelho:** `shared/src/commonMain/kotlin/com/bolao/domain/usecase/PredictionCalculator.kt`. **Testes:** `shared/src/commonTest/kotlin/com/bolao/domain/usecase/PredictionCalculatorTest.kt`. **Texto ao usuário:** `RulesBottomSheet.kt`, `README.md`, `OnboardingScreen.kt`. |
| Quem se classificou (PAR-09) | Trigger SQL; `Match.actualQualifier` (`domain/model/Match.kt`); bloco `calculatedQualifier` duplicado em `MatchListViewModel.kt` e `LeagueDetailViewModel.kt`. |
| Multiplicador e mata-mata por fase | Migrations `20260611000001_auto_stage_multiplier.sql` e `20260629000000_knockout_stage_points.sql`; nomes das fases em `supabase/functions/sync-world-cup-schedule/index.ts`; ordem das abas em `MatchListViewModel.kt`; badge em `MatchPredictionCard.kt`. |
| Placar 90'/prorrogação/pênaltis | `supabase/functions/update-live-matches/index.ts` e `sync-world-cup-schedule/index.ts`. |
| Trava de horário | RLS em `20260527000001_time_lock_rls.sql`; `Match.isPredictionAllowed`; trava de "live" em `update-live-matches`. |
| Ocultar palpites até o início (PAL-07) | RLS em `20261007000001_hide_predictions_until_kickoff.sql`; botão "Ver palpites da galera" em `MatchPredictionCard.kt`. |
| Odds e congelamento | `supabase/functions/update-upcoming-odds/index.ts`. |
| Ranking | View `league_leaderboard`; ordenação em `LeagueDetailViewModel.kt`. |
| Notificações | `supabase/functions/notify-upcoming-matches/index.ts`; opções em `SettingsScreen.kt`. |

---

## 12. Divergências conhecidas e decisões pendentes

Itens em que o código atual **não** está alinhado com as regras acima, ou em que a regra ainda precisa de confirmação do dono. **Não corrija silenciosamente** — trate cada um como tarefa própria, com aprovação.

| ID | Divergência | Impacto |
|---|---|---|
| DIV-03 | A tela de regras diz "todo palpite exige a escolha de quem avança", mas o app **permite salvar sem escolher** o classificado. | **Decisão pendente:** obrigar a escolha no mata-mata ou ajustar o texto. |
| DIV-06 | A view `league_leaderboard` conta "placar exato" comparando com `home_score`/`away_score` (que incluem a prorrogação), enquanto a pontuação usa o placar dos 90'. | Em mata-mata com prorrogação, o desempate pode divergir dos pontos. **Decisão pendente:** usar o placar dos 90' na view. |
| DIV-07 | "Terceiro Lugar" é mata-mata, mas não casa com nenhuma regra de multiplicador (fica ×1.0). Não há decisão registrada. | **Decisão pendente:** confirmar ×1.0 ou definir outro multiplicador. |
| DIV-08 | O ranking ao vivo (VIV-01/02) só considera status `live` e `halftime`. Em `extratime`, `et_halftime` e `penalties` a parcial **some** do ranking até a partida encerrar. | Ranking "pisca" na prorrogação. |
| DIV-09 | O app trata status `cancelled` como `scheduled` (`MatchMapper.toGameStatus`). | Partida cancelada com data futura aparece como aberta para palpite. |
| DIV-10 | O texto do onboarding diz que, na zebra, "a pontuação do jogo é multiplicada"; a fórmula da tela de regras omite o bônus de classificação; o potencial máximo (PON-11) não inclui os +2 de classificação. | Só texto/UX, mas confunde o usuário. |
| DIV-11 | `removeMember` e `renewInviteCode` existem no ViewModel/repositório, mas não estão expostos na UI. `renewInviteCode` grava o código fixo `"NEWCODE"` e não existem políticas RLS de UPDATE/DELETE para ligas e membros. | Funcionalidade **não implementada**: não trate como regra vigente. Sair de liga, remover membro e renovar código ainda não existem. |
| DIV-12 | O `README.md` não menciona o bônus de classificação nem o terceiro lugar, e descreve a faixa máxima de zebra como "acima de 9.00" (a regra é ≥ 9.00). | Documentação incompleta; este arquivo prevalece. |

---

### Resolvidas

Os IDs não são reaproveitados.

| ID | Resolução |
|---|---|
| DIV-01 | Em 2026-10-07 o cálculo vigente (bônus de classificação só pelo seletor) virou a migration `20261007000000_scoring_trigger_consolidation.sql`. Os scripts soltos `realtime_fix.sql`, `qualifier_fix_v2.sql` e `qualifier_fix_v3.sql` foram removidos (continuam no histórico do git). |
| DIV-02 | Em 2026-10-07 o `CHECK ('home','away')` foi removido e trocado pela validação de que `predicted_qualifier` é um dos times da partida. Valores legados `'home'`/`'away'`, se houver, foram convertidos para o UUID do time. |
| DIV-04 | Em 2026-10-07 a RLS passou a esconder palpites de terceiros até `scheduled_at` (migration `20261007000001_hide_predictions_until_kickoff.sql`). |
| DIV-05 | Em 2026-10-07 o trigger passou a recalcular partidas já encerradas quando placar, odds ou fase mudam (PON-09). Partidas encerradas antes disso **não** foram recalculadas automaticamente. |

## 13. Histórico de decisões

Registre aqui toda mudança de regra (data, decisão, commit/PR). Mais recentes no topo.

| Data | Decisão | Referência |
|---|---|---|
| 2026-10-07 | Pontos recalculados automaticamente quando dados de uma partida encerrada são corrigidos; recálculo manual por `recalculate_match_points`. Palpites de terceiros ocultos no banco até o início da partida. `predicted_qualifier` validado como UUID de um dos times. | migrations `20261007000000`, `20261007000001` |
| 2026-09-29 | Semântica dos placares: `*_score_90` = tempo regulamentar (vem da API), `*_score_et` = acumulado com prorrogação, `*_score` = 90' + prorrogação (sem pênaltis), `penalty_winner` a partir da disputa de pênaltis. | `bf44c77` (PR #7) |
| 2026-07-02 | **Bônus de classificação usa só o seletor** ("prioridade do seletor"); o placar palpitado é ignorado. Classificado real: pênaltis > prorrogação > placar final. | `f1b93a5` (PR #4), `realtime_fix.sql` |
| 2026-06-30 | (Revogada em 02/07) Classificado deduzido do placar palpitado, seletor só para palpites de empate. | `9a9d18a` (PR #3), `qualifier_fix_v2.sql` |
| 2026-06-29 | Mata-mata: pontuação pelo placar dos 90'; seletor "quem avança"; bônus de +2 pelo classificado quando há empate nos 90'. | `5259035` |
| 2026-06-12 | Palpites de outros membros ficam ocultos até a partida começar. | `CHANGELOG` v1.2.1 |
| 2026-06-11 | Desempate do ranking por placares exatos. | `c736347` |
| 2026-06-11 | Multiplicadores decimais (×1.5, ×2, ×2.5) atribuídos automaticamente pelo nome da fase; resultado arredondado para inteiro. | migrations `20260611000000`, `20260611000001` |
| 2026-06-11 | Membros de uma mesma liga podem ver os palpites uns dos outros. | migration `20260611000002` |
| 2026-05-27 | Palpites travados no horário de início (RLS). Escopo reduzido à Copa do Mundo 2026. | migrations `20260527000000`, `20260527000001` |
| 2026-05-22 | Pontuação base 1/+2/+2, bônus de zebra em faixas (+2/+4/+7), cálculo no servidor via trigger. | migration `20260522000000` |
