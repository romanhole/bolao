# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

## [1.2.1] - 2026-06-12
### Adicionado (Features)
- **Visualização de Palpites do Grupo:** Nova funcionalidade para visualizar os palpites de todos os participantes do seu grupo/liga em uma partida.
- **Regra de Ocultação Estratégica:** Os palpites de outros usuários permanecem bloqueados (com cadeado) e só são revelados quando a partida começa.
- **Navegação por Rodadas/Fases:** Adicionada navegação por abas na lista de partidas (Ex: Fase de Grupos, 16-avos, Oitavas, etc).

### Corrigido (Bug Fixes)
- Correção no backend (Edge Functions) para mapear corretamente o status de intervalo ("halftime") e de etapas secundárias ("1st_half", "2nd_half", "extratime", etc) que vinham da Bzzoiro API. Isso resolve o problema das partidas ficarem travadas nos minutos finais do primeiro tempo ou perderem o cronômetro.
- Ajuste no componente visual `TopAppBar` que estava apresentando uma margem (testa) muito grande no topo das telas de Palpites e Ligas.
- Correção de compilação da versão Wasm (Web) pela falta da dependência `lifecycle-runtime-compose`.
