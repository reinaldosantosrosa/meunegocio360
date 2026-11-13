package br.com.meunegocio360.meunegocio360_core.auth.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {
  Optional<RefreshToken> findByHashForUpdate(String hash);

  List<RefreshToken> ativosDaFamilia(UUID familiaId);

  List<RefreshToken> ativosDoUsuario(UUID usuarioId);

  RefreshToken save(RefreshToken token);

  int removerAntigos(Instant limite);
}
