package dev.rotadev.sources.application;

import java.io.IOException;
import java.io.InputStream;

import dev.rotadev.sources.domain.LinkedInData;

/**
 * Extrai conteúdo dos arquivos enviados. A implementação ({@code infrastructure/}) usa PDFBox,
 * StAX e Commons CSV; o caso de uso só vê texto e records.
 */
public interface DocumentReader {

    String pdfText(InputStream pdf) throws IOException, UnreadableDocumentException;

    String docxText(InputStream docx) throws IOException, UnreadableDocumentException;

    LinkedInData linkedInExport(InputStream zip) throws IOException, UnreadableDocumentException;

    /**
     * O arquivo está corrompido ou não é o que diz ser. Tentar de novo não resolve, então o job
     * não deve ser reagendado (diferente de uma {@link IOException} ao ler o disco).
     */
    class UnreadableDocumentException extends Exception {

        public UnreadableDocumentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
