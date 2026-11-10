package dev.rotadev.sources.application;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dev.rotadev.jobs.Job;
import dev.rotadev.jobs.JobHandler;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.ProfileRecorder;
import dev.rotadev.profile.SkillSignal;
import dev.rotadev.shared.FileStorage;
import dev.rotadev.sources.domain.DocumentFormat;
import dev.rotadev.sources.domain.LinkedInAnalysis;
import dev.rotadev.sources.domain.LinkedInData;
import dev.rotadev.sources.domain.SourceFile;
import dev.rotadev.sources.domain.SourceType;
import dev.rotadev.sources.domain.TextSkillMatcher;
import dev.rotadev.sources.domain.UploadKind;
import tools.jackson.databind.json.JsonMapper;

/**
 * Job {@value #TYPE}: lê um arquivo enviado e grava o snapshot de perfil correspondente.
 *
 * <p>Dois tipos de falha, tratados de jeitos diferentes:
 * <ul>
 *   <li>arquivo ilegível (PDF quebrado, ZIP que não é do LinkedIn): marca o arquivo como FAILED e
 *       termina o job, porque tentar de novo daria o mesmo resultado;</li>
 *   <li>falha ao ler o disco ({@link IOException}): deixa a exceção subir, e o worker reagenda.</li>
 * </ul>
 */
@Component
public class DocumentParsing implements JobHandler {

    public static final String TYPE = "document.parse";

    private static final Logger log = LoggerFactory.getLogger(DocumentParsing.class);

    /** Texto bruto guardado no snapshot, para reprocessar sem o arquivo. Currículo raramente passa disso. */
    static final int MAX_RAW_TEXT = 50_000;

    private final SourceFileRepository files;
    private final SourceRepository sources;
    private final FileStorage storage;
    private final DocumentReader reader;
    private final ProfileRecorder profile;
    private final JsonMapper json;
    private final Clock clock;

    public DocumentParsing(SourceFileRepository files, SourceRepository sources, FileStorage storage, DocumentReader reader,
            ProfileRecorder profile, JsonMapper json, Clock clock) {
        this.files = files;
        this.sources = sources;
        this.storage = storage;
        this.reader = reader;
        this.profile = profile;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void handle(Job job) throws IOException {
        UUID fileId = UUID.fromString(json.readTree(job.payload()).get("fileId").asString());
        // userId do job, não do payload: o arquivo só é encontrado se for do mesmo usuário (ADR 0004).
        SourceFile file = files.findById(job.userId(), fileId)
                .orElseThrow(() -> new IllegalStateException("arquivo " + fileId + " não existe"));

        Parsed parsed;
        try (InputStream in = storage.open(file.storageKey())) {
            parsed = parse(file, in);
        } catch (DocumentReader.UnreadableDocumentException e) {
            log.info("Arquivo {} ilegível: {}", file.id(), e.getMessage());
            files.updateStatus(job.userId(), file.id(), "FAILED");
            return;
        }

        SourceType type = file.kind() == UploadKind.LINKEDIN ? SourceType.LINKEDIN : SourceType.RESUME;
        profile.record(job.userId(), type.name(), job.id(), parsed.rawJson(), parsed.skills(), parsed.evidences());
        files.updateStatus(job.userId(), file.id(), "PROCESSED");
        sources.markUploaded(job.userId(), type, clock.instant());
    }

    private Parsed parse(SourceFile file, InputStream in) throws IOException, DocumentReader.UnreadableDocumentException {
        if (file.kind() == UploadKind.LINKEDIN && file.format() == DocumentFormat.ZIP) {
            LinkedInData data = reader.linkedInExport(in);
            var result = LinkedInAnalysis.analyze(data);
            return new Parsed(json.writeValueAsString(Map.of("fileId", file.id(), "linkedin", data)), result.skills(), result.evidences());
        }
        String text = file.format() == DocumentFormat.DOCX ? reader.docxText(in) : reader.pdfText(in);
        String label = file.kind() == UploadKind.LINKEDIN ? "perfil do LinkedIn" : "currículo";
        var result = TextSkillMatcher.match(text, label);
        String raw = text.length() > MAX_RAW_TEXT ? text.substring(0, MAX_RAW_TEXT) : text;
        return new Parsed(json.writeValueAsString(Map.of("fileId", file.id(), "text", raw)), result.skills(), result.evidences());
    }

    private record Parsed(String rawJson, List<SkillSignal> skills, List<Evidence> evidences) {
    }
}
