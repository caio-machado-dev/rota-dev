package dev.rotadev.identity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Quem está logado. É o que o Spring Security guarda na sessão depois do login com o GitHub.
 *
 * <p>Este tipo fica na raiz do módulo ({@code dev.rotadev.identity}), e não num subpacote,
 * porque é a <b>API pública</b> do módulo para o Spring Modulith (ADR 0002): os controllers dos
 * outros módulos recebem o usuário assim, com {@code @AuthenticationPrincipal AuthenticatedUser user},
 * e usam {@code user.id()} como o {@code userId} de toda consulta (ADR 0004).
 *
 * <p>{@code Serializable} porque a sessão vai para o Postgres em bytes (Spring Session JDBC).
 * Por isso aqui só há o id e o login, e nenhum token.
 */
public record AuthenticatedUser(UUID id, String login) implements OAuth2User, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Nome do principal no Spring Security. Vai para a coluna PRINCIPAL_NAME da sessão. */
    @Override
    public String getName() {
        return id.toString();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of("id", id.toString(), "login", login);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
