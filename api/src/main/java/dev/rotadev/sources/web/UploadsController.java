package dev.rotadev.sources.web;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.sources.application.InvalidUploadException;
import dev.rotadev.sources.application.SourceFileRepository;
import dev.rotadev.sources.application.UploadDocument;
import dev.rotadev.sources.domain.UploadKind;

@RestController
@RequestMapping("/api/sources/uploads")
class UploadsController {

    private final UploadDocument upload;
    private final SourceFileRepository files;

    UploadsController(UploadDocument upload, SourceFileRepository files) {
        this.upload = upload;
        this.files = files;
    }

    /**
     * Recebe {@code multipart/form-data} com os campos {@code kind} e {@code file}, como um
     * {@code UploadFile} do FastAPI. O limite de tamanho do Spring fica em application.yml.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    UploadResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam UploadKind kind,
            @RequestParam MultipartFile file) throws IOException {
        return UploadResponse.from(upload.upload(user.id(), kind, file.getOriginalFilename(), file.getBytes()));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<UploadResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return files.findAllByUser(user.id()).stream().map(UploadResponse::from).toList();
    }

    /**
     * {@code @ExceptionHandler} converte a exceção do caso de uso numa resposta 400. O corpo é um
     * {@link ProblemDetail} (RFC 9457), o formato padrão de erro do Spring: {"title", "status", "detail"}.
     */
    @ExceptionHandler(InvalidUploadException.class)
    ResponseEntity<ProblemDetail> invalid(InvalidUploadException e) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()));
    }
}
