package dev.rotadev.ai.application;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.rotadev.ai.AiQuotaExceededException;

/**
 * {@code @RestControllerAdvice} trata uma exceção para todos os controllers de uma vez, como um
 * exception handler global do FastAPI. Assim diagnóstico, plano e reajustes respondem igual ao limite.
 */
@RestControllerAdvice
class AiQuotaAdvice {

    @ExceptionHandler(AiQuotaExceededException.class)
    ResponseEntity<ProblemDetail> quota(AiQuotaExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
    }
}
