package dev.rotadev.sources.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.identity.TierLimits;
import dev.rotadev.identity.UserTiers;
import dev.rotadev.jobs.JobQueue;
import dev.rotadev.shared.FileStorage;
import dev.rotadev.shared.UuidV7;
import dev.rotadev.sources.domain.DocumentFormat;
import dev.rotadev.sources.domain.SourceFile;
import dev.rotadev.sources.domain.UploadKind;

/**
 * Caso de uso: o usuário enviou o export do LinkedIn ou o currículo.
 *
 * <p>Confere o formato pelo conteúdo, grava o arquivo no {@link FileStorage} e os metadados no
 * banco, e enfileira a leitura do conteúdo ({@link DocumentParsing}). O mesmo arquivo (mesmo
 * SHA-256) enviado de novo devolve o registro que já existe, sem novo job.
 */
@Service
public class UploadDocument {

    /** 10 MB: um currículo tem uns 200 KB, e o export do LinkedIn, poucos MB. */
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private final FileStorage storage;
    private final SourceFileRepository files;
    private final JobQueue jobs;
    private final UserTiers tiers;
    private final TierLimits limits;
    private final Clock clock;

    public UploadDocument(FileStorage storage, SourceFileRepository files, JobQueue jobs, UserTiers tiers, TierLimits limits, Clock clock) {
        this.storage = storage;
        this.files = files;
        this.jobs = jobs;
        this.tiers = tiers;
        this.limits = limits;
        this.clock = clock;
    }

    @Transactional
    public SourceFile upload(UUID userId, UploadKind kind, String originalName, byte[] content) {
        if (content.length == 0) {
            throw new InvalidUploadException("Arquivo vazio.");
        }
        if (content.length > MAX_BYTES) {
            throw new InvalidUploadException("Arquivo maior que 10 MB.");
        }
        DocumentFormat format = DocumentFormat.sniff(content)
                .orElseThrow(() -> new InvalidUploadException("Formato não reconhecido. Envie PDF, DOCX ou o ZIP do LinkedIn."));
        if (!kind.accepts(format)) {
            throw new InvalidUploadException("Para " + kind + " aceitamos outro formato, não " + format + ".");
        }

        String sha256 = sha256(content);
        var existing = files.findBySha256(userId, sha256);
        if (existing.isPresent()) {
            return existing.get();
        }

        int limit = limits.uploads(tiers.tierOf(userId));
        if (files.findAllByUser(userId).size() >= limit) {
            throw new InvalidUploadException("Você já tem " + limit + " arquivos enviados, o limite do seu plano.");
        }

        Instant now = clock.instant();
        String key;
        try {
            key = storage.save(userId, new ByteArrayInputStream(content));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var file = new SourceFile(UuidV7.generate(now), userId, kind, format, safeName(originalName), content.length,
                sha256, key, "RECEIVED", now);
        files.insert(file);
        jobs.enqueue(userId, DocumentParsing.TYPE, "{\"fileId\": \"" + file.id() + "\"}", DocumentParsing.TYPE + ":" + file.id());
        return file;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** O nome só é exibido, mas mesmo assim cortamos o caminho e limitamos o tamanho. */
    private static String safeName(String name) {
        if (name == null || name.isBlank()) {
            return "arquivo";
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.length() > 200 ? base.substring(0, 200) : base;
    }
}
