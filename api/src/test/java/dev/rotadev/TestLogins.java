package dev.rotadev;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.identity.domain.User;

/** Atalho dos testes de controller: a requisição sai como se {@code user} estivesse logado. */
public final class TestLogins {

    private TestLogins() {
    }

    public static RequestPostProcessor loggedAs(User user) {
        return oauth2Login().oauth2User(new AuthenticatedUser(user.id(), user.login()));
    }
}
