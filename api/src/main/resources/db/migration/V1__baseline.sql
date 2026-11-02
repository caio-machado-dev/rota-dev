-- Migração inicial (baseline).
--
-- O Flyway cria a tabela flyway_schema_history e registra esta versão como aplicada.
-- As tabelas de domínio entram a partir da Fase 1 (V2__users.sql, ...), cada uma
-- explicada no PR que a introduz.
--
-- Regra: uma migração já aplicada nunca é editada. Para mudar algo, crie V<n+1>.

SET TIME ZONE 'UTC';
