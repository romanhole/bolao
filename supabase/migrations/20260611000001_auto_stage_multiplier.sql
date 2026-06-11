-- Script para automatizar a atribuição do stage_multiplier baseado na coluna 'round'

-- 1. Atualiza as partidas que já existem no banco de dados
UPDATE public.matches
SET stage_multiplier = CASE
    WHEN round ILIKE '%Final%' AND round NOT ILIKE '%Quarta%' AND round NOT ILIKE '%Oitava%' AND round NOT ILIKE '%Semi%' AND round NOT ILIKE '%avos%' THEN 2.5
    WHEN round ILIKE '%Semi%' THEN 2.0
    WHEN round ILIKE '%Quarta%' THEN 2.0
    WHEN round ILIKE '%Oitava%' OR round ILIKE '%avos%' THEN 1.5
    ELSE 1.0
END;

-- 2. Cria uma função para definir automaticamente o multiplicador de futuras partidas
CREATE OR REPLACE FUNCTION public.auto_set_stage_multiplier()
RETURNS trigger AS $$
BEGIN
    NEW.stage_multiplier := CASE
        WHEN NEW.round ILIKE '%Final%' AND NEW.round NOT ILIKE '%Quarta%' AND NEW.round NOT ILIKE '%Oitava%' AND NEW.round NOT ILIKE '%Semi%' AND NEW.round NOT ILIKE '%avos%' THEN 2.5
        WHEN NEW.round ILIKE '%Semi%' THEN 2.0
        WHEN NEW.round ILIKE '%Quarta%' THEN 2.0
        WHEN NEW.round ILIKE '%Oitava%' OR NEW.round ILIKE '%avos%' THEN 1.5
        ELSE 1.0
    END;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Cria o trigger que vai rodar antes de inserir ou atualizar a rodada de uma partida
DROP TRIGGER IF EXISTS trigger_auto_stage_multiplier ON public.matches;
CREATE TRIGGER trigger_auto_stage_multiplier
BEFORE INSERT OR UPDATE OF round ON public.matches
FOR EACH ROW
EXECUTE FUNCTION public.auto_set_stage_multiplier();
