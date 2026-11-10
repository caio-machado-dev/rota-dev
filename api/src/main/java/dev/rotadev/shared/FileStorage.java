package dev.rotadev.shared;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Onde ficam os arquivos que o usuário envia (currículo, export do LinkedIn), ADR 0008.
 *
 * <p>Hoje é uma pasta local (um volume Docker em produção); amanhã pode ser um armazenamento
 * compatível com S3, trocando só a implementação. Os arquivos <b>nunca</b> são servidos por URL
 * pública: só a API lê, depois de conferir o dono.
 */
public interface FileStorage {

    /** Grava o conteúdo e devolve a chave para ler depois. A chave começa pelo id do usuário. */
    String save(UUID userId, InputStream content) throws IOException;

    InputStream open(String key) throws IOException;

    void delete(String key) throws IOException;

    /** Apaga todos os arquivos do usuário (exclusão da conta, LGPD). */
    void deleteAllFor(UUID userId) throws IOException;
}
