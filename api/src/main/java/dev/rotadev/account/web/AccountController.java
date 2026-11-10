package dev.rotadev.account.web;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.rotadev.account.application.AccountService;
import dev.rotadev.identity.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/account")
class AccountController {

    private final AccountService account;

    AccountController(AccountService account) {
        this.account = account;
    }

    /** Todos os dados do usuário em JSON, como download (LGPD art. 18, II e V). */
    @GetMapping(path = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Map<String, Object>> export(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rota-dev-dados.json\"")
                .body(account.export(user.id()));
    }

    /** Confirmação explícita: o corpo precisa repetir o login do GitHub. */
    record DeleteRequest(@NotBlank String confirmLogin) {
    }

    /** Exclusão definitiva da conta (LGPD art. 18, VI). Encerra a sessão atual junto. */
    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProblemDetail> delete(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody DeleteRequest body,
            HttpServletRequest request) {
        if (!body.confirmLogin().equals(user.login())) {
            return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                    "Para confirmar, digite exatamente o seu login do GitHub."));
        }
        account.delete(user.id());
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
