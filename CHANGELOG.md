# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

## [1.2.1] - 2026-06-12
### Corrigido (Bug Fixes)
- Correção no backend (Edge Functions) para mapear corretamente o status de intervalo ("halftime") e de etapas secundárias ("1st_half", "2nd_half", "extratime", etc) que vinham da Bzzoiro API. Isso resolve o problema das partidas ficarem travadas nos minutos finais do primeiro tempo ou perderem o cronômetro.
- Ajuste no componente visual `TopAppBar` que estava apresentando uma margem (testa) muito grande no topo das telas de Palpites e Ligas.
- Correção de compilação da versão Wasm (Web) pela falta da dependência `lifecycle-runtime-compose`.
