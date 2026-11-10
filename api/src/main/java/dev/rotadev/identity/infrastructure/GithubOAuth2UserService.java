package dev.rotadev.identity.infrastructure;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.identity.GithubAccountLinked;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

/**
 * Ponte entre o Spring Security e o caso de uso {@link GithubSignIn}.
 *
 * <p>Depois que o GitHub devolve o código de autorização, o Spring troca o código por um token
 * e chama {@link #loadUser}. O {@code super.loadUser} busca {@code GET https://api.github.com/user};
 * aqui convertemos a resposta num {@link GithubProfile}, gravamos o usuário e devolvemos o
 * {@link AuthenticatedUser} que vai para a sessão.
 */
@Component
class GithubOAuth2UserService extends DefaultOAuth2UserService {

    private final GithubSignIn signIn;
    private final ApplicationEventPublisher events;

    GithubOAuth2UserService(GithubSignIn signIn, ApplicationEventPublisher events) {
        this.signIn = signIn;
        this.events = events;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User github = super.loadUser(request);
        User user = signIn.signIn(toProfile(github.getAttributes()));
        // O módulo sources escuta este evento e guarda o token cifrado (ADR 0008).
        events.publishEvent(new GithubAccountLinked(
                user.id(), user.login(), request.getAccessToken().getTokenValue(), request.getAccessToken().getScopes()));
        return new AuthenticatedUser(user.id(), user.login());
    }

    /** Campos da API do GitHub: https://docs.github.com/rest/users/users#get-the-authenticated-user */
    static GithubProfile toProfile(Map<String, Object> attributes) {
        // O JSON chega como Map<String, Object>; o id é um número que pode vir como Integer ou Long.
        long id = ((Number) attributes.get("id")).longValue();
        return new GithubProfile(
                id,
                (String) attributes.get("login"),
                (String) attributes.get("name"),
                (String) attributes.get("email"),
                (String) attributes.get("avatar_url"));
    }
}
