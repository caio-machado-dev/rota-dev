-- Migração inicial (baseline).
--
-- O Flyway cria a tabela flyway_schema_history e registra esta versão como aplicada.
-- As tabelas de domínio entram a partir da Fase 1 (V2__users.sql, ...), cada uma
-- explicada no PR que a introduz.
--
-- Regra: uma migração já aplicada nunca é editada. Para mudar algo, crie V<n+1>.
-- (Esta só está sendo ajustada porque ainda não foi aplicada em nenhum banco real.)

COMMENT ON SCHEMA public IS 'Rota Dev. Schema versionado pelo Flyway (api/src/main/resources/db/migration).';
