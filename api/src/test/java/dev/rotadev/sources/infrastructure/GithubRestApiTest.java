package dev.rotadev.sources.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * {@link MockRestServiceServer} intercepta as chamadas do RestClient e responde o que mandarmos,
 * sem rede. É o papel do {@code responses} (ou {@code respx}) no Python.
 */
class GithubRestApiTest {

    @Test
    void listsReposWithLanguagesAndCi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GithubRestApi api = new GithubRestApi(builder, "https://api.github.test");

        server.expect(requestTo("https://api.github.test/user/repos?affiliation=owner&sort=pushed&per_page=30"))
                .andExpect(header("Authorization", "Bearer gho_x"))
                .andRespond(withSuccess("""
                        [{"name": "rota-dev", "full_name": "caio/rota-dev", "html_url": "https://github.com/caio/rota-dev",
                          "description": "Plano de carreira", "fork": false, "archived": false, "stargazers_count": 3,
                          "pushed_at": "2026-11-09T10:00:00Z", "campo_ignorado": true}]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.test/repos/caio/rota-dev/languages"))
                .andRespond(withSuccess("{\"Java\": 1200, \"TypeScript\": 800}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.test/repos/caio/rota-dev/actions/workflows"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        var repos = api.recentOwnedRepos("gho_x", 30);

        assertThat(repos).singleElement().satisfies(r -> {
            assertThat(r.fullName()).isEqualTo("caio/rota-dev");
            assertThat(r.stars()).isEqualTo(3);
            assertThat(r.languages()).containsEntry("Java", 1200L);
            assertThat(r.hasCi()).isFalse();
        });
        server.verify();
    }
}
