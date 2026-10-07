# Instruções para agentes de IA

## Regras de negócio: leia antes de alterar

A fonte de verdade das regras e decisões de negócio do Bolão é o arquivo
[`REGRAS_DE_NEGOCIO.md`](REGRAS_DE_NEGOCIO.md). Leia esse arquivo **antes** de qualquer
ajuste ou nova feature que envolva pontuação, palpites, partidas, fases, odds, ligas,
ranking ou notificações.

- As regras desse arquivo prevalecem sobre comentários no código, README, CHANGELOG,
  textos de UI e scripts SQL soltos na raiz.
- Se o pedido conflitar com uma regra, **pare e pergunte** antes de alterar. Não mude
  regras de carona em outra tarefa.
- Ao mudar uma regra (com aprovação), atualize `REGRAS_DE_NEGOCIO.md` no mesmo commit,
  incluindo o "Histórico de decisões", e altere todos os pontos listados no
  "Mapa de implementação" (trigger SQL + `PredictionCalculator` + testes + textos de UI).
- Divergências já conhecidas entre código e regra estão na seção 12. Não as corrija
  sem pedido explícito.
