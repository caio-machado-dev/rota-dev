package dev.rotadev.sources.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import dev.rotadev.sources.application.DocumentReader.UnreadableDocumentException;

class DocumentReadersTest {

    private final DocumentReaders readers = new DocumentReaders();

    @Test
    void extractsTextFromPdf() throws Exception {
        assertThat(readers.pdfText(new ByteArrayInputStream(pdf("Experiencia com Java e Docker")))).contains("Java e Docker");
    }

    @Test
    void brokenPdfIsUnreadable() {
        assertThatThrownBy(() -> readers.pdfText(new ByteArrayInputStream("%PDF-1.7 quebrado".getBytes())))
                .isInstanceOf(UnreadableDocumentException.class);
    }

    @Test
    void extractsParagraphsFromDocx() throws Exception {
        String xml = """
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>
                <w:p><w:r><w:t>Caio Machado</w:t></w:r></w:p>
                <w:p><w:r><w:t>Python, </w:t></w:r><w:r><w:t>FastAPI</w:t></w:r></w:p>
                </w:body></w:document>""";

        String text = readers.docxText(new ByteArrayInputStream(zip(Map.of("word/document.xml", xml))));

        assertThat(text).isEqualTo("Caio Machado\nPython, FastAPI\n");
    }

    /** XXE: um DOCX malicioso declara uma entidade que lê um arquivo do servidor. O parser recusa. */
    @Test
    void docxWithExternalEntityIsRejected() throws Exception {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE d [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:p><w:r><w:t>&xxe;</w:t></w:r></w:p></w:document>""";

        assertThatThrownBy(() -> readers.docxText(new ByteArrayInputStream(zip(Map.of("word/document.xml", xml)))))
                .isInstanceOf(UnreadableDocumentException.class);
    }

    @Test
    void readsLinkedInCsvsIncludingQuotedCommas() throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("Basic_LinkedInDataExport/Positions.csv", """
                Company Name,Title,Description,Location,Started On,Finished On
                Acme,Engenheiro Backend,"APIs em Python, FastAPI e PostgreSQL",Aracaju,Mar 2023,
                """);
        files.put("Skills.csv", "Name\nPython\nDocker\n");

        var data = readers.linkedInExport(new ByteArrayInputStream(zip(files)));

        assertThat(data.positions()).singleElement().satisfies(p -> {
            assertThat(p.company()).isEqualTo("Acme");
            assertThat(p.description()).isEqualTo("APIs em Python, FastAPI e PostgreSQL");
            assertThat(p.finishedOn()).isEmpty();
        });
        assertThat(data.skills()).containsExactly("Python", "Docker");
        assertThat(data.certifications()).isEmpty();
    }

    @Test
    void zipWithoutLinkedInFilesIsUnreadable() {
        assertThatThrownBy(() -> readers.linkedInExport(new ByteArrayInputStream(zip(Map.of("foto.jpg", "x")))))
                .isInstanceOf(UnreadableDocumentException.class);
    }

    /** Gera um PDF de verdade com o próprio PDFBox. Fonte Helvetica: sem acentos no texto. */
    static byte[] pdf(String text) throws IOException {
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            var page = new PDPage();
            doc.addPage(page);
            try (var content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    static byte[] zip(Map<String, String> entries) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (var e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
