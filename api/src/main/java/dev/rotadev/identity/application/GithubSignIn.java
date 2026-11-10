package dev.rotadev.identity.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

/**
 * Caso de uso: alguém terminou o login com o GitHub. Se o usuário já existe, os dados do
 * GitHub são atualizados; se não, ele é criado. É o {@code update_or_create} do Django.
 *
 * <p>Quem chama é o fluxo OAuth (próximo PR da Fase 1).
 */
@Service
public class GithubSignIn {

    private final UserRepository users;
    private final Clock clock;

    // Injeção pelo construtor: o Spring vê que só há um construtor e passa os beans sozinho.
    // Os campos ficam final, e no teste basta chamar new GithubSignIn(mock, relógioFixo).
    public GithubSignIn(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    /**
     * {@code @Transactional} abre uma transação antes do método e faz commit no fim (ou
     * rollback se sair uma exceção). Assim a busca e a escrita acontecem juntas: é o
     * {@code with transaction.atomic():} do Django, só que declarado na assinatura.
     * Ele fica na camada de aplicação porque é aqui que se sabe o que forma uma unidade de trabalho.
     */
    @Transactional
    public User signIn(GithubProfile profile) {
        Instant now = clock.instant();
        return users.findByGithubId(profile.githubId())
                .map(existing -> users.updateProfile(existing.id(), profile, now))
                .orElseGet(() -> users.create(profile, now));
    }
}
