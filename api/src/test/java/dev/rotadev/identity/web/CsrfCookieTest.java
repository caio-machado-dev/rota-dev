package dev.rotadev.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

/**
 * O front lê o cookie {@code XSRF-TOKEN} para mandar o cabeçalho {@code X-XSRF-TOKEN}
 * (web/src/lib/api/client.ts). Este teste confere a configuração real do {@code csrf.spa()}.
 *
 * <p><b>Por que numa classe separada, com contexto próprio:</b> o {@code csrf()} do spring-security-test
 * troca o repositório de tokens do {@code CsrfFilter} por um de sessão, só para teste. O filtro é um bean
 * compartilhado, então a troca vale para todas as requisições seguintes no mesmo contexto do Spring. Se
 * qualquer classe com o mesmo contexto usar {@code .with(csrf())} antes, este teste deixa de ver o cookie.
 * A propriedade abaixo não muda nada na aplicação: só dá a esta classe um contexto que ninguém mais usa.
 * (O CI pegou isso: o teste passava ou falhava conforme a ordem das classes.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "rota.test.context=csrf-cookie")
class CsrfCookieTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Test
    void sessionRequestHandsOutCsrfCookie() {
        User erin = signIn.signIn(new GithubProfile(1005L, "erin", null, null, null));

        assertThat(mvc.get().uri("/api/me").with(oauth2Login().oauth2User(new AuthenticatedUser(erin.id(), erin.login()))))
                .hasStatusOk()
                .cookies().containsCookie("XSRF-TOKEN");
    }
}
