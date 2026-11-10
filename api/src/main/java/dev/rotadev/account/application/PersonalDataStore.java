package dev.rotadev.account.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Acesso transversal aos dados pessoais, para a LGPD (exportar e excluir). */
public interface PersonalDataStore {

    /** Linhas do usuário em cada tabela com {@code user_id}, por nome de tabela, já sem as colunas secretas. */
    Map<String, List<Map<String, Object>>> export(UUID userId);

    /** Apaga o usuário; o {@code ON DELETE CASCADE} das tabelas leva o resto. */
    void deleteUser(UUID userId);

    /** Encerra todas as sessões do usuário (Spring Session JDBC). */
    void deleteSessions(UUID userId);

    /** Quantas linhas ainda existem com este {@code user_id}, somando todas as tabelas. Usado para conferir a exclusão. */
    long remainingRows(UUID userId);
}
