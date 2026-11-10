package dev.rotadev.identity.domain;

import java.util.Objects;

/**
 * O que o GitHub diz sobre o usuário no momento do login.
 *
 * <p>Um {@code record} é uma classe imutável com construtor, getters ({@code login()}),
 * {@code equals} e {@code toString} gerados, parecido com um {@code @dataclass(frozen=True)}.
 * O bloco sem parênteses abaixo é o "construtor compacto": roda antes de atribuir os campos
 * e é o lugar das validações, como o {@code __post_init__} de uma dataclass.
 *
 * @param githubId  id numérico e estável do GitHub (o login pode mudar, o id não)
 * @param login     nome de usuário no GitHub
 * @param name      nome de exibição, pode ser {@code null}
 * @param email     e-mail público, pode ser {@code null}
 * @param avatarUrl URL da foto, pode ser {@code null}
 */
public record GithubProfile(long githubId, String login, String name, String email, String avatarUrl) {

    public GithubProfile {
        if (githubId <= 0) {
            throw new IllegalArgumentException("githubId deve ser positivo");
        }
        Objects.requireNonNull(login, "login");
        if (login.isBlank()) {
            throw new IllegalArgumentException("login não pode ser vazio");
        }
    }
}
