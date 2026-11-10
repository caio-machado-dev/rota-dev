package dev.rotadev.sources.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.sources.application.SourceFileRepository;
import dev.rotadev.sources.domain.DocumentFormat;
import dev.rotadev.sources.domain.SourceFile;
import dev.rotadev.sources.domain.UploadKind;

@Repository
class JdbcSourceFileRepository implements SourceFileRepository {

    private static final String COLUMNS =
            "id, user_id, kind, format, original_name, size_bytes, sha256, storage_key, status, created_at";

    private final JdbcClient jdbc;

    JdbcSourceFileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(SourceFile f) {
        jdbc.sql("INSERT INTO source_files (" + COLUMNS + ") VALUES "
                        + "(:id, :userId, :kind, :format, :name, :size, :sha, :key, :status, :createdAt)")
                .param("id", f.id())
                .param("userId", f.userId())
                .param("kind", f.kind().name())
                .param("format", f.format().name())
                .param("name", f.originalName())
                .param("size", f.sizeBytes())
                .param("sha", f.sha256())
                .param("key", f.storageKey())
                .param("status", f.status())
                .param("createdAt", Timestamp.from(f.createdAt()))
                .update();
    }

    @Override
    public Optional<SourceFile> findBySha256(UUID userId, String sha256) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM source_files WHERE user_id = :userId AND sha256 = :sha")
                .param("userId", userId).param("sha", sha256)
                .query(JdbcSourceFileRepository::map).optional();
    }

    @Override
    public List<SourceFile> findAllByUser(UUID userId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM source_files WHERE user_id = :userId ORDER BY created_at DESC")
                .param("userId", userId)
                .query(JdbcSourceFileRepository::map).list();
    }

    @Override
    public Optional<SourceFile> findById(UUID userId, UUID id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM source_files WHERE user_id = :userId AND id = :id")
                .param("userId", userId).param("id", id)
                .query(JdbcSourceFileRepository::map).optional();
    }

    @Override
    public void updateStatus(UUID userId, UUID id, String status) {
        jdbc.sql("UPDATE source_files SET status = :status WHERE user_id = :userId AND id = :id")
                .param("status", status).param("userId", userId).param("id", id)
                .update();
    }

    private static SourceFile map(ResultSet rs, int row) throws SQLException {
        return new SourceFile(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                UploadKind.valueOf(rs.getString("kind")),
                DocumentFormat.valueOf(rs.getString("format")),
                rs.getString("original_name"),
                rs.getLong("size_bytes"),
                rs.getString("sha256"),
                rs.getString("storage_key"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant());
    }
}
