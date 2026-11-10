package dev.rotadev.identity.application;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.identity.Tier;
import dev.rotadev.identity.UserTiers;
import dev.rotadev.identity.UserTimeZones;
import dev.rotadev.identity.domain.User;

/** Consulta de usuário por id, para o {@code GET /api/me}, e o fuso para os outros módulos. */
@Service
public class UserLookup implements UserTimeZones, UserTiers {

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

    @Override
    @Transactional(readOnly = true)
    public ZoneId of(UUID userId) {
        return users.findById(userId).map(User::timeZone).orElse(User.DEFAULT_TIME_ZONE);
    }

    @Override
    @Transactional(readOnly = true)
    public Tier tierOf(UUID userId) {
        return users.findById(userId).map(User::tier).orElse(Tier.FREE);
    }
}
