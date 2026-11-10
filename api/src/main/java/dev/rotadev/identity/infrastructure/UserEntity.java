package dev.rotadev.identity.infrastructure;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import dev.rotadev.identity.Tier;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha da tabela {@code users}, criada em {@code V2__users.sql}.
 *
 * <p>A classe não é {@code public}: só a infraestrutura do módulo enxerga a entidade, então
 * ela não tem como vazar para um controller ou para outro módulo (ADR 0003). O Hibernate
 * confere na subida se as colunas batem com a tabela ({@code ddl-auto: validate}).
 *
 * <p>Diferente de um model do Django, a entidade JPA é mutável e precisa de um construtor sem
 * argumentos: o Hibernate cria o objeto vazio e preenche os campos ao ler do banco.
 */
@Entity
@Table(name = "users")
class UserEntity {

    // UUID v7 gerado pelo Hibernate na aplicação antes do INSERT (ADR 0009).
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "github_id", nullable = false, unique = true)
    private long githubId;

    @Column(nullable = false)
    private String login;

    private String name;

    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // insertable = false: a coluna tem DEFAULT 'FREE' no banco (V16) e só muda por fora do login.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, insertable = false, updatable = false)
    private Tier tier;

    /** Exigido pelo JPA. {@code protected} para ninguém usar por engano. */
    protected UserEntity() {
    }

    UserEntity(long githubId, String login, String timeZone, Instant now) {
        this.githubId = githubId;
        this.login = login;
        this.timeZone = timeZone;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void updateProfile(String login, String name, String email, String avatarUrl, Instant now) {
        this.login = login;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.updatedAt = now;
    }

    UUID getId() { return id; }
    long getGithubId() { return githubId; }
    String getLogin() { return login; }
    String getName() { return name; }
    String getEmail() { return email; }
    String getAvatarUrl() { return avatarUrl; }
    String getTimeZone() { return timeZone; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    Tier getTier() { return tier; }
}
