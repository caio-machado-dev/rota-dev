package dev.rotadev.account.infrastructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.account.application.PersonalDataStore;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exporta e apaga por descoberta: as tabelas com coluna {@code user_id} vêm do {@code information_schema}.
 *
 * <p>É uma exceção consciente à regra de que cada módulo cuida das suas tabelas (ADR 0002): para a
 * LGPD, a garantia que importa é a de <b>completude</b>. Uma tabela nova entra no export e na conferência
 * de exclusão sem ninguém lembrar de registrá-la. O preço é que este módulo conhece o banco inteiro;
 * por isso ele só lê nomes de tabela e só apaga a linha de {@code users}.
 */
@Repository
class JdbcPersonalDataStore implements PersonalDataStore {

    /** Colunas que nunca saem no export: segredo cifrado e detalhes internos de armazenamento. */
    static final Set<String> SECRET_COLUMNS = Set.of("access_token_encrypted", "storage_key");

    private static final TypeReference<Map<String, Object>> ROW = new TypeReference<>() { };

    private final JdbcClient jdbc;
    private final JsonMapper json;

    JdbcPersonalDataStore(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Map<String, List<Map<String, Object>>> export(UUID userId) {
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
        out.put("users", rows("SELECT row_to_json(t)::text FROM users t WHERE id = :id", userId));
        for (String table : userTables()) {
            out.put(table, rows("SELECT row_to_json(t)::text FROM " + quote(table) + " t WHERE user_id = :id", userId));
        }
        return out;
    }

    @Override
    public void deleteUser(UUID userId) {
        jdbc.sql("DELETE FROM users WHERE id = :id").param("id", userId).update();
    }

    /** A sessão guarda o principal pelo nome, que é o id do usuário ({@code AuthenticatedUser.getName}). */
    @Override
    public void deleteSessions(UUID userId) {
        jdbc.sql("DELETE FROM spring_session WHERE principal_name = :name").param("name", userId.toString()).update();
    }

    @Override
    public long remainingRows(UUID userId) {
        long total = jdbc.sql("SELECT count(*) FROM users WHERE id = :id").param("id", userId).query(Long.class).single();
        for (String table : userTables()) {
            total += jdbc.sql("SELECT count(*) FROM " + quote(table) + " WHERE user_id = :id").param("id", userId).query(Long.class).single();
        }
        return total;
    }

    /** Tabelas do schema public com coluna user_id. Os nomes vêm do catálogo do Postgres, nunca do usuário. */
    private List<String> userTables() {
        return jdbc.sql("""
                SELECT table_name FROM information_schema.columns
                 WHERE table_schema = 'public' AND column_name = 'user_id'
                 ORDER BY table_name
                """).query(String.class).list();
    }

    private List<Map<String, Object>> rows(String sql, UUID userId) {
        return jdbc.sql(sql).param("id", userId).query(String.class).list().stream()
                .map(text -> {
                    Map<String, Object> row = json.readValue(text, ROW);
                    SECRET_COLUMNS.forEach(row::remove);
                    return row;
                })
                .toList();
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
