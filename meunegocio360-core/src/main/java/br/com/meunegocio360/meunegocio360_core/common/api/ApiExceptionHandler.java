package br.com.meunegocio360.meunegocio360_core.common.api;

import br.com.meunegocio360.meunegocio360_core.auth.application.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  private final boolean cookieSecure;

  public ApiExceptionHandler(@Value("${app.auth.cookie.secure}") boolean cookieSecure) {
    this.cookieSecure = cookieSecure;
  }

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<Map<String, Object>> tratarAuth(
      AuthException exception, HttpServletRequest request) {
    String title =
        switch (exception.getStatus()) {
          case 401 -> "Não autenticado";
          case 403 -> "Acesso proibido";
          case 429 -> "Limite excedido";
          default -> "Requisição inválida";
        };
    Map<String, Object> body =
        ProblemBody.criar(
            exception.getStatus(),
            exception.getCode(),
            title,
            exception.getMessage(),
            request.getRequestURI(),
            null);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.valueOf("application/problem+json"));
    if (exception.getRetryAfter() != null) {
      headers.set(HttpHeaders.RETRY_AFTER, exception.getRetryAfter().toString());
    }
    if ("INVALID_REFRESH_TOKEN".equals(exception.getCode())) {
      headers.add(
          HttpHeaders.SET_COOKIE,
          ResponseCookie.from("mn360_refresh", "")
              .path("/api/v1/auth")
              .httpOnly(true)
              .secure(cookieSecure)
              .sameSite("Strict")
              .maxAge(Duration.ZERO)
              .build()
              .toString());
    }
    if ("UNAUTHORIZED".equals(exception.getCode())) {
      headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    }
    return new ResponseEntity<>(body, headers, HttpStatus.valueOf(exception.getStatus()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> tratarValidacao(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<Map<String, String>> errors =
        exception.getBindingResult().getFieldErrors().stream().map(this::erroCampo).toList();
    return problemaValidacao(request, errors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> tratarJsonInvalido(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return problemaValidacao(request, List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> tratarErroInterno(
      Exception exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.valueOf("application/problem+json"))
        .body(
            ProblemBody.criar(
                500,
                "INTERNAL_ERROR",
                "Erro interno",
                "Não foi possível concluir a solicitação.",
                request.getRequestURI(),
                null));
  }

  private ResponseEntity<Map<String, Object>> problemaValidacao(
      HttpServletRequest request, List<Map<String, String>> errors) {
    return ResponseEntity.badRequest()
        .contentType(MediaType.valueOf("application/problem+json"))
        .body(
            ProblemBody.criar(
                400,
                "VALIDATION_ERROR",
                "Dados inválidos",
                "Um ou mais campos são inválidos.",
                request.getRequestURI(),
                errors));
  }

  private Map<String, String> erroCampo(FieldError error) {
    return Map.of("field", error.getField(), "message", "valor inválido");
  }
}
