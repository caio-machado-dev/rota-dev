package dev.rotadev.identity.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Regras de segurança HTTP da API inteira (ADR 0005).
 *
 * <p>O Spring Security é uma cadeia de filtros que roda antes dos controllers, parecido com um
 * middleware do Django. Este bean monta essa cadeia:
 * <ul>
 *   <li><b>Quem pode o quê:</b> só a saúde e as rotas do próprio login são públicas; o resto exige sessão.</li>
 *   <li><b>Login:</b> o navegador vai a {@code /api/oauth2/authorization/github}, o Spring redireciona
 *       para o GitHub, e o GitHub volta em {@code /api/login/oauth2/code/github}. As duas rotas
 *       começam com {@code /api} para passar pelo rewrite do Next e ficar na mesma origem.</li>
 *   <li><b>Sem sessão:</b> a API responde 401 em vez de redirecionar para uma página de login, porque
 *       quem decide o que mostrar é o front.</li>
 *   <li><b>CSRF:</b> {@code spa()} grava o token no cookie {@code XSRF-TOKEN}, legível pelo JavaScript.
 *       O front devolve o valor no cabeçalho {@code X-XSRF-TOKEN} em todo POST, PUT e DELETE.
 *       Um site de terceiros não consegue ler o cookie, então não consegue forjar o cabeçalho.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, GithubOAuth2UserService githubUsers) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/oauth2/**", "/api/login/**").permitAll()
                        // O contrato da API é público: o repositório também é.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/oauth2/authorization"))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo.userService(githubUsers))
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/?login=erro"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .csrf(csrf -> csrf.spa());
        return http.build();
    }
}
