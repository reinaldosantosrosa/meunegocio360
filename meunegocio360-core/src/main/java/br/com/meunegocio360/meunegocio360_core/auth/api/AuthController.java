package br.com.meunegocio360.meunegocio360_core.auth.api;

import br.com.meunegocio360.meunegocio360_core.auth.application.AuthService;
import br.com.meunegocio360.meunegocio360_core.auth.application.AuthService.Sessao;
import br.com.meunegocio360.meunegocio360_core.auth.application.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private static final String COOKIE_NAME = "mn360_refresh";
  private static final String COOKIE_PATH = "/api/v1/auth";
  private final AuthService authService;
  private final boolean cookieSecure;

  public AuthController(
      AuthService authService, @Value("${app.auth.cookie.secure}") boolean cookieSecure) {
    this.authService = authService;
    this.cookieSecure = cookieSecure;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return respostaComCookie(authService.autenticar(request.email(), request.senha()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @CookieValue(name = COOKIE_NAME, required = false) String refreshToken) {
    return respostaComCookie(authService.renovar(refreshToken));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = COOKIE_NAME, required = false) String refreshToken) {
    authService.logout(refreshToken);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
        .build();
  }

  @GetMapping("/me")
  public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
    return respostaUsuario(authService.usuarioAutenticado(jwt.getSubject()));
  }

  private ResponseEntity<AuthResponse> respostaComCookie(Sessao sessao) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            cookie(sessao.refreshToken(), Duration.ofSeconds(sessao.maxAge())).toString())
        .body(
            new AuthResponse(
                "Bearer",
                sessao.accessToken(),
                sessao.expiresIn(),
                respostaUsuario(sessao.usuario())));
  }

  private UsuarioResponse respostaUsuario(UsuarioAutenticado usuario) {
    return new UsuarioResponse(usuario.id(), usuario.nome(), usuario.email(), usuario.perfil());
  }

  private ResponseCookie cookie(String valor, Duration maxAge) {
    return ResponseCookie.from(COOKIE_NAME, valor)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Strict")
        .path(COOKIE_PATH)
        .maxAge(maxAge)
        .build();
  }
}
