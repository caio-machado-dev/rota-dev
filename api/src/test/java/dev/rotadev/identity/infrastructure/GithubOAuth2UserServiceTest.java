package dev.rotadev.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.rotadev.identity.domain.GithubProfile;

class GithubOAuth2UserServiceTest {

    @Test
    void mapsGithubUserJsonToProfile() {
        Map<String, Object> json = Map.of(
                "id", 583231, "login", "octocat", "name", "The Octocat",
                "email", "octo@example.com", "avatar_url", "https://avatars.example/u/583231");

        assertThat(GithubOAuth2UserService.toProfile(json))
                .isEqualTo(new GithubProfile(583231L, "octocat", "The Octocat", "octo@example.com",
                        "https://avatars.example/u/583231"));
    }

    @Test
    void acceptsMissingOptionalFields() {
        // HashMap porque Map.of não aceita valor null, e o GitHub manda "name": null.
        Map<String, Object> json = new HashMap<>();
        json.put("id", 1L);
        json.put("login", "sem-nome");
        json.put("name", null);

        GithubProfile profile = GithubOAuth2UserService.toProfile(json);

        assertThat(profile.name()).isNull();
        assertThat(profile.email()).isNull();
        assertThat(profile.avatarUrl()).isNull();
    }
}
