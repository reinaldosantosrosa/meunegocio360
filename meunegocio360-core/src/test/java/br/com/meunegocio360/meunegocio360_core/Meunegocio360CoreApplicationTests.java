package br.com.meunegocio360.meunegocio360_core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.meunegocio360.meunegocio360_core.auth.application.AuthException;
import br.com.meunegocio360.meunegocio360_core.auth.application.AuthService;
import br.com.meunegocio360.meunegocio360_core.auth.domain.Perfil;
import br.com.meunegocio360.meunegocio360_core.auth.domain.RefreshToken;
import br.com.meunegocio360.meunegocio360_core.auth.domain.RefreshTokenStore;
import br.com.meunegocio360.meunegocio360_core.auth.domain.Usuario;
import br.com.meunegocio360.meunegocio360_core.auth.domain.UsuarioStore;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class Meunegocio360CoreApplicationTests {
  @Mock private UsuarioStore usuarios;

  @Mock private RefreshTokenStore refreshTokens;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtEncoder jwtEncoder;

  private AuthService authService;
  private final Instant agora = Instant.parse("2026-11-12T12:00:00Z");

  @BeforeEach
  void configurar() {
    authService =
        new AuthService(
            usuarios,
            refreshTokens,
            passwordEncoder,
            jwtEncoder,
            new SecureRandom(),
            Clock.fixed(agora, ZoneOffset.UTC),
            15,
            7,
            5,
            15);
  }

  @Test
  void emailInexistenteUsaHashFicticioERespostaGenerica() {
    when(usuarios.findByEmailForUpdate("naoexiste@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.matches(eq("senha-incorreta"), anyString())).thenReturn(false);

    AuthException exception =
        assertThrows(
            AuthException.class,
            () -> authService.autenticar(" NaoExiste@Example.com ", "senha-incorreta"));

    assertEquals("INVALID_CREDENTIALS", exception.getCode());
    assertEquals("E-mail ou senha inválidos.", exception.getMessage());
    verify(passwordEncoder).matches(eq("senha-incorreta"), anyString());
  }

  @Test
  void falhasConsecutivasBloqueiamContaAposCincoTentativas() {
    Usuario usuario = new Usuario("Pessoa", "pessoa@example.com", "{bcrypt}hash", Perfil.USUARIO);
    when(usuarios.findByEmailForUpdate(usuario.getEmail())).thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

    for (int tentativa = 0; tentativa < 5; tentativa++) {
      assertThrows(
          AuthException.class, () -> authService.autenticar(usuario.getEmail(), "senha-errada"));
    }

    AuthException bloqueio =
        assertThrows(
            AuthException.class, () -> authService.autenticar(usuario.getEmail(), "senha-correta"));
    assertEquals(429, bloqueio.getStatus());
    assertEquals("ACCOUNT_TEMPORARILY_LOCKED", bloqueio.getCode());
    assertEquals(900L, bloqueio.getRetryAfter());
    assertTrue(usuario.getBloqueadoAte().isAfter(agora));
  }

  @Test
  void loginArmazenaApenasHashDoRefreshToken() {
    Usuario usuario = new Usuario("Pessoa", "pessoa@example.com", "{bcrypt}hash", Perfil.USUARIO);
    Jwt jwt = org.mockito.Mockito.mock(Jwt.class);
    when(usuarios.findByEmailForUpdate(usuario.getEmail())).thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("senha-correta", usuario.getSenhaHash())).thenReturn(true);
    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
    when(jwt.getTokenValue()).thenReturn("jwt-de-acesso");
    when(refreshTokens.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AuthService.Sessao sessao = authService.autenticar(usuario.getEmail(), "senha-correta");
    ArgumentCaptor<RefreshToken> tokenSalvo = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokens).save(tokenSalvo.capture());

    assertEquals("jwt-de-acesso", sessao.accessToken());
    assertEquals(900, sessao.expiresIn());
    assertEquals(64, tokenSalvo.getValue().getTokenHash().length());
    assertNotEquals(sessao.refreshToken(), tokenSalvo.getValue().getTokenHash());
  }
}
