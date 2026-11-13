package br.com.meunegocio360.meunegocio360_core.auth.application;

import br.com.meunegocio360.meunegocio360_core.auth.domain.RefreshToken;
import br.com.meunegocio360.meunegocio360_core.auth.domain.RefreshTokenStore;
import br.com.meunegocio360.meunegocio360_core.auth.domain.Usuario;
import br.com.meunegocio360.meunegocio360_core.auth.domain.UsuarioStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
  private static final String HASH_FICTICIO =
      "{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

  private final UsuarioStore usuarios;
  private final RefreshTokenStore refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;
  private final SecureRandom secureRandom;
  private final Clock clock;
  private final long expiracaoJwtMinutos;
  private final long expiracaoRefreshDias;
  private final int maxTentativas;
  private final int bloqueioMinutos;

  public AuthService(
      UsuarioStore usuarios,
      RefreshTokenStore refreshTokens,
      PasswordEncoder passwordEncoder,
      JwtEncoder jwtEncoder,
      SecureRandom secureRandom,
      Clock clock,
      @Value("${app.auth.jwt.expiracao-minutos}") long expiracaoJwtMinutos,
      @Value("${app.auth.refresh.expiracao-dias}") long expiracaoRefreshDias,
      @Value("${app.auth.max-tentativas}") int maxTentativas,
      @Value("${app.auth.bloqueio-minutos}") int bloqueioMinutos) {
    this.usuarios = usuarios;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
    this.secureRandom = secureRandom;
    this.clock = clock;
    this.expiracaoJwtMinutos = expiracaoJwtMinutos;
    this.expiracaoRefreshDias = expiracaoRefreshDias;
    this.maxTentativas = maxTentativas;
    this.bloqueioMinutos = bloqueioMinutos;
  }

  @Transactional(noRollbackFor = AuthException.class)
  public Sessao autenticar(String emailInformado, String senha) {
    String email = emailInformado.trim().toLowerCase(Locale.ROOT);
    Usuario usuario = usuarios.findByEmailForUpdate(email).orElse(null);
    if (usuario == null) {
      passwordEncoder.matches(senha, HASH_FICTICIO);
      LOGGER.warn("Evento de segurança: auth_login_falha");
      throw credenciaisInvalidas();
    }

    Instant agora = clock.instant();
    if (usuario.isAtivo()
        && usuario.getBloqueadoAte() != null
        && usuario.getBloqueadoAte().isAfter(agora)) {
      long segundos = Math.max(1, Duration.between(agora, usuario.getBloqueadoAte()).toSeconds());
      LOGGER.warn("Evento de segurança: auth_login_bloqueado");
      throw new AuthException(
          429, "ACCOUNT_TEMPORARILY_LOCKED", "Conta temporariamente bloqueada.", segundos);
    }
    if (usuario.getBloqueadoAte() != null) {
      usuario.limparBloqueioExpirado(agora);
    }

    boolean senhaValida = passwordEncoder.matches(senha, usuario.getSenhaHash());
    if (!usuario.isAtivo() || !senhaValida) {
      if (usuario.isAtivo()) {
        usuario.registrarFalha(agora, maxTentativas, bloqueioMinutos);
      }
      LOGGER.warn("Evento de segurança: auth_login_falha");
      throw credenciaisInvalidas();
    }

    usuario.registrarLogin(agora);
    LOGGER.info("Evento de segurança: auth_login_sucesso perfil={}", usuario.getPerfil());
    UUID familiaId = UUID.randomUUID();
    String refreshToken = novoRefreshToken();
    refreshTokens.save(
        new RefreshToken(
            usuario,
            familiaId,
            hashRefreshToken(refreshToken),
            agora.plus(Duration.ofDays(expiracaoRefreshDias)),
            agora));
    return criarSessao(usuario, refreshToken);
  }

  @Transactional(noRollbackFor = AuthException.class)
  public Sessao renovar(String valorRefreshToken) {
    if (valorRefreshToken == null || valorRefreshToken.isBlank()) {
      throw refreshInvalido();
    }
    Instant agora = clock.instant();
    RefreshToken token =
        refreshTokens
            .findByHashForUpdate(hashRefreshToken(valorRefreshToken))
            .orElseThrow(AuthService::refreshInvalido);

    if (token.getRevogadoEm() != null) {
      revogarFamilia(token.getFamiliaId(), agora);
      throw refreshInvalido();
    }
    if (!token.getExpiraEm().isAfter(agora)) {
      token.revogar(agora);
      throw refreshInvalido();
    }

    Usuario usuario = token.getUsuario();
    if (!usuario.isAtivo()
        || (usuario.getBloqueadoAte() != null && usuario.getBloqueadoAte().isAfter(agora))) {
      revogarUsuario(usuario.getId(), agora);
      throw refreshInvalido();
    }

    token.revogar(agora);
    String novoValor = novoRefreshToken();
    refreshTokens.save(
        new RefreshToken(
            usuario,
            token.getFamiliaId(),
            hashRefreshToken(novoValor),
            agora.plus(Duration.ofDays(expiracaoRefreshDias)),
            agora));
    LOGGER.info("Evento de segurança: auth_refresh_sucesso");
    return criarSessao(usuario, novoValor);
  }

  @Transactional
  public void logout(String valorRefreshToken) {
    if (valorRefreshToken == null || valorRefreshToken.isBlank()) {
      return;
    }
    refreshTokens
        .findByHashForUpdate(hashRefreshToken(valorRefreshToken))
        .ifPresent(token -> revogarFamilia(token.getFamiliaId(), clock.instant()));
  }

  @Transactional(readOnly = true)
  public UsuarioAutenticado usuarioAutenticado(String id) {
    try {
      Usuario usuario =
          usuarios.findById(UUID.fromString(id)).orElseThrow(AuthService::naoAutorizado);
      if (!usuario.isAtivo()) {
        throw naoAutorizado();
      }
      return usuarioAutenticado(usuario);
    } catch (IllegalArgumentException exception) {
      throw naoAutorizado();
    }
  }

  @Transactional
  public int removerTokensAntigos(Instant limite) {
    return refreshTokens.removerAntigos(limite);
  }

  private Sessao criarSessao(Usuario usuario, String refreshToken) {
    Instant agora = clock.instant();
    Instant expiracao = agora.plus(Duration.ofMinutes(expiracaoJwtMinutos));
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("meunegocio360-core")
            .subject(usuario.getId().toString())
            .issuedAt(agora)
            .expiresAt(expiracao)
            .id(UUID.randomUUID().toString())
            .claim("perfil", usuario.getPerfil().name())
            .build();
    String accessToken =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256)
                        .build(),
                    claims))
            .getTokenValue();
    return new Sessao(
        accessToken,
        Duration.between(agora, expiracao).toSeconds(),
        usuarioAutenticado(usuario),
        refreshToken,
        Duration.ofDays(expiracaoRefreshDias).toSeconds());
  }

  private UsuarioAutenticado usuarioAutenticado(Usuario usuario) {
    return new UsuarioAutenticado(
        usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil().name());
  }

  private void revogarFamilia(UUID familiaId, Instant agora) {
    refreshTokens.ativosDaFamilia(familiaId).forEach(token -> token.revogar(agora));
  }

  private void revogarUsuario(UUID usuarioId, Instant agora) {
    refreshTokens.ativosDoUsuario(usuarioId).forEach(token -> token.revogar(agora));
  }

  private String novoRefreshToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashRefreshToken(String token) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 indisponível", exception);
    }
  }

  private static AuthException credenciaisInvalidas() {
    return new AuthException(401, "INVALID_CREDENTIALS", "E-mail ou senha inválidos.");
  }

  private static AuthException refreshInvalido() {
    return new AuthException(401, "INVALID_REFRESH_TOKEN", "Refresh token inválido.");
  }

  private static AuthException naoAutorizado() {
    return new AuthException(401, "UNAUTHORIZED", "Autenticação necessária.");
  }

  public record Sessao(
      String accessToken,
      long expiresIn,
      UsuarioAutenticado usuario,
      String refreshToken,
      long maxAge) {}
}
