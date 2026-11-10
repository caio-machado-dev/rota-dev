package dev.rotadev.sources.infrastructure;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import dev.rotadev.sources.domain.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Linha de {@code connected_sources} (V5). */
@Entity
@Table(name = "connected_sources")
class ConnectedSourceEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    // STRING grava o nome ("GITHUB"); o padrão, ORDINAL, gravaria a posição no enum e quebraria
    // se alguém reordenasse os valores.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SourceType type;

    @Column(name = "external_login")
    private String externalLogin;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "access_token_encrypted")
    private String accessToken;

    private String scopes;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected ConnectedSourceEntity() {
    }

    ConnectedSourceEntity(UUID userId, SourceType type, Instant now) {
        this.userId = userId;
        this.type = type;
        this.connectedAt = now;
        this.updatedAt = now;
    }

    void updateCredentials(String externalLogin, String accessToken, String scopes, Instant now) {
        this.externalLogin = externalLogin;
        this.accessToken = accessToken;
        this.scopes = scopes;
        this.updatedAt = now;
    }

    void markSynced(Instant now) {
        this.lastSyncedAt = now;
        this.updatedAt = now;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    SourceType getType() { return type; }
    String getExternalLogin() { return externalLogin; }
    String getAccessToken() { return accessToken; }
    Instant getConnectedAt() { return connectedAt; }
    Instant getLastSyncedAt() { return lastSyncedAt; }
}
