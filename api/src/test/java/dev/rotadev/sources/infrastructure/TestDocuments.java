package dev.rotadev.sources.infrastructure;

import java.io.IOException;

/** Expõe os geradores de arquivo de teste para testes de outros pacotes. */
public final class TestDocuments {

    private TestDocuments() {
    }

    public static byte[] pdf(String text) throws IOException {
        return DocumentReadersTest.pdf(text);
    }
}
