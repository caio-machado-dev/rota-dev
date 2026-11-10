package dev.rotadev.sources.infrastructure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import dev.rotadev.sources.application.DocumentReader;
import dev.rotadev.sources.domain.LinkedInData;

/**
 * Lê PDF, DOCX e o ZIP do LinkedIn.
 *
 * <p>Arquivo enviado por usuário é entrada não confiável. Por isso:
 * <ul>
 *   <li><b>ZIP bomb:</b> um ZIP de 1 MB pode descompactar para gigabytes. Lemos no máximo
 *       {@value #MAX_ENTRY_BYTES} bytes por entrada e ignoramos o resto.</li>
 *   <li><b>XXE:</b> o XML do DOCX poderia declarar entidades externas e fazer o parser ler arquivos do
 *       servidor. O parser StAX é criado com DTD e entidades externas desligadas.</li>
 * </ul>
 */
@Component
class DocumentReaders implements DocumentReader {

    static final int MAX_ENTRY_BYTES = 10 * 1024 * 1024;

    private final XMLInputFactory xml;

    DocumentReaders() {
        xml = XMLInputFactory.newFactory();
        xml.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xml.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    }

    @Override
    public String pdfText(InputStream pdf) throws IOException, UnreadableDocumentException {
        byte[] bytes = pdf.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            // O PDFBox usa IOException tanto para disco quanto para PDF quebrado. Aqui os bytes
            // já estão em memória, então só pode ser o arquivo.
            throw new UnreadableDocumentException("PDF ilegível", e);
        }
    }

    @Override
    public String docxText(InputStream docx) throws IOException, UnreadableDocumentException {
        byte[] documentXml = entries(docx).get("word/document.xml");
        if (documentXml == null) {
            throw new UnreadableDocumentException("DOCX sem word/document.xml", null);
        }
        try {
            return wordText(documentXml);
        } catch (XMLStreamException e) {
            throw new UnreadableDocumentException("DOCX com XML inválido", e);
        }
    }

    @Override
    public LinkedInData linkedInExport(InputStream zip) throws IOException, UnreadableDocumentException {
        Map<String, byte[]> files = new HashMap<>();
        // O LinkedIn às vezes põe os CSVs numa subpasta; usamos só o nome do arquivo.
        entries(zip).forEach((name, content) -> files.put(name.substring(name.lastIndexOf('/') + 1), content));
        if (!files.containsKey("Positions.csv") && !files.containsKey("Skills.csv")) {
            throw new UnreadableDocumentException("ZIP não parece um export do LinkedIn", null);
        }

        List<LinkedInData.Position> positions = new ArrayList<>();
        for (CSVRecord r : csv(files.get("Positions.csv"))) {
            positions.add(new LinkedInData.Position(
                    get(r, "Company Name"), get(r, "Title"), get(r, "Description"), get(r, "Started On"), get(r, "Finished On")));
        }
        List<String> skills = new ArrayList<>();
        for (CSVRecord r : csv(files.get("Skills.csv"))) {
            String name = get(r, "Name");
            if (name != null && !name.isBlank()) {
                skills.add(name);
            }
        }
        List<LinkedInData.Certification> certifications = new ArrayList<>();
        for (CSVRecord r : csv(files.get("Certifications.csv"))) {
            certifications.add(new LinkedInData.Certification(get(r, "Name"), get(r, "Authority"), get(r, "Started On")));
        }
        return new LinkedInData(positions, skills, certifications);
    }

    /** Entradas do ZIP em memória, cada uma limitada a {@link #MAX_ENTRY_BYTES}. */
    private static Map<String, byte[]> entries(InputStream zip) throws IOException, UnreadableDocumentException {
        Map<String, byte[]> out = new HashMap<>();
        try (var in = new ZipInputStream(zip)) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                if (!entry.isDirectory()) {
                    out.put(entry.getName(), in.readNBytes(MAX_ENTRY_BYTES));
                }
            }
        } catch (java.util.zip.ZipException e) {
            throw new UnreadableDocumentException("ZIP corrompido", e);
        }
        return out;
    }

    /** Texto dos elementos {@code <w:t>}, com quebra de linha a cada parágrafo {@code <w:p>}. */
    private String wordText(byte[] documentXml) throws XMLStreamException {
        StringBuilder text = new StringBuilder();
        XMLStreamReader reader = xml.createXMLStreamReader(new ByteArrayInputStream(documentXml));
        try {
            boolean inText = false;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamReader.START_ELEMENT && reader.getLocalName().equals("t")) {
                    inText = true;
                } else if (event == XMLStreamReader.END_ELEMENT && reader.getLocalName().equals("t")) {
                    inText = false;
                } else if (event == XMLStreamReader.END_ELEMENT && reader.getLocalName().equals("p")) {
                    text.append('\n');
                } else if (event == XMLStreamReader.CHARACTERS && inText) {
                    text.append(reader.getText());
                }
            }
        } finally {
            reader.close();
        }
        return text.toString();
    }

    private static List<CSVRecord> csv(byte[] content) throws IOException {
        if (content == null) {
            return List.of();
        }
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).get();
        try (var parser = format.parse(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            return parser.getRecords();
        }
    }

    private static String get(CSVRecord record, String column) {
        return record.isMapped(column) ? record.get(column) : null;
    }
}
