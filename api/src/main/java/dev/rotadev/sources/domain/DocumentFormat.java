package dev.rotadev.sources.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Formato real do arquivo, descoberto pelo conteúdo ("magic number"), e não pela extensão nem pelo
 * Content-Type, que o navegador (ou um atacante) pode mandar errado.
 */
public enum DocumentFormat {
    PDF,
    /** DOCX também é um ZIP; a diferença é ter {@code word/document.xml} dentro. */
    DOCX,
    ZIP;

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ZIP_MAGIC = {'P', 'K', 3, 4};

    public static Optional<DocumentFormat> sniff(byte[] content) {
        if (startsWith(content, PDF_MAGIC)) {
            return Optional.of(PDF);
        }
        if (startsWith(content, ZIP_MAGIC)) {
            return Optional.of(hasEntry(content, "word/document.xml") ? DOCX : ZIP);
        }
        return Optional.empty();
    }

    /** Lê só a lista de entradas do ZIP (os nomes), sem descompactar o conteúdo. */
    private static boolean hasEntry(byte[] zip, String name) {
        try (var in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                if (entry.getName().equals(name)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length && Arrays.equals(data, 0, prefix.length, prefix, 0, prefix.length);
    }
}
