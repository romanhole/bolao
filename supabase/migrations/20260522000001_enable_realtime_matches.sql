-- Habilita a replicação lógica (Realtime WebSockets) apenas para a tabela matches
ALTER PUBLICATION supabase_realtime ADD TABLE matches;
