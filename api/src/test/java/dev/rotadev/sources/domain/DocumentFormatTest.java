package dev.rotadev.sources.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class DocumentFormatTest {

    @Test
    void recognizesPdfByMagicNumber() {
        assertThat(DocumentFormat.sniff("%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII))).contains(DocumentFormat.PDF);
    }

    @Test
    void tellsDocxFromPlainZip() throws IOException {
        assertThat(DocumentFormat.sniff(zip("[Content_Types].xml", "word/document.xml"))).contains(DocumentFormat.DOCX);
        assertThat(DocumentFormat.sniff(zip("Positions.csv", "Skills.csv"))).contains(DocumentFormat.ZIP);
    }

    @Test
    void rejectsAnythingElseEvenWithAPdfName() {
        assertThat(DocumentFormat.sniff("<html>não sou pdf</html>".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(DocumentFormat.sniff(new byte[0])).isEmpty();
    }

    /** Monta um ZIP em memória com as entradas pedidas (conteúdo vazio). */
    static byte[] zip(String... entries) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (String entry : entries) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write("x".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
