package dev.rotadev.sources.domain;

import java.util.Set;

/** O que o arquivo é, segundo o usuário, e quais formatos aceitamos para cada um. */
public enum UploadKind {
    /** Export de dados do LinkedIn (ZIP com CSVs) ou o perfil salvo em PDF. */
    LINKEDIN(Set.of(DocumentFormat.ZIP, DocumentFormat.PDF)),
    RESUME(Set.of(DocumentFormat.PDF, DocumentFormat.DOCX));

    private final Set<DocumentFormat> accepted;

    UploadKind(Set<DocumentFormat> accepted) {
        this.accepted = accepted;
    }

    public boolean accepts(DocumentFormat format) {
        return accepted.contains(format);
    }
}
