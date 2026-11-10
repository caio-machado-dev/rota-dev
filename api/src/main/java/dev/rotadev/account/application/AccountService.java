package dev.rotadev.account.application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.shared.FileStorage;

/**
 * Direitos do titular na LGPD (art. 18): acesso e portabilidade (exportar tudo em JSON) e eliminação
 * (apagar a conta e tudo o que depende dela).
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final PersonalDataStore data;
    private final FileStorage files;
    private final Clock clock;

    public AccountService(PersonalDataStore data, FileStorage files, Clock clock) {
        this.data = data;
        this.files = files;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", clock.instant().toString());
        out.put("format", "rota-dev-export-v1");
        out.put("tables", data.export(userId));
        return out;
    }

    /**
     * Exclusão definitiva. Banco primeiro, numa transação: se falhar, nada some. Os arquivos vêm
     * depois do commit implícito do banco; se o disco falhar aí, o erro fica registrado para limpeza
     * manual, e o usuário já não existe mais para ninguém.
     */
    @Transactional
    public void delete(UUID userId) {
        data.deleteSessions(userId);
        data.deleteUser(userId);
        long left = data.remainingRows(userId);
        if (left > 0) {
            // Uma tabela nova sem ON DELETE CASCADE cairia aqui. Melhor desfazer tudo do que apagar pela metade.
            throw new IllegalStateException("exclusão incompleta: " + left + " linhas restantes");
        }
        try {
            files.deleteAllFor(userId);
        } catch (IOException e) {
            log.error("Arquivos do usuário {} não foram apagados; remover à mão do storage", userId, e);
            throw new UncheckedIOException(e);
        }
    }
}
