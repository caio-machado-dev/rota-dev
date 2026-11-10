package dev.rotadev.identity.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.identity.domain.User;

/** Consulta de usuário por id, para o {@code GET /api/me}. */
@Service
public class UserLookup {

    private final UserRepository users;

    public UserLookup(UserRepository users) {
        this.users = users;
    }

    /**
     * {@code readOnly = true} avisa o Hibernate que nada será escrito: ele pula a checagem de
     * mudanças nas entidades no fim da transação, e o Postgres pode otimizar a leitura.
     */
    @Transactional(readOnly = true)
    public Optional<User> byId(UUID id) {
        return users.findById(id);
    }
}
