package br.com.meunegocio360.meunegocio360_core.auth.infra;

import br.com.meunegocio360.meunegocio360_core.auth.domain.RefreshToken;
import br.com.meunegocio360.meunegocio360_core.auth.domain.RefreshTokenStore;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository
    extends JpaRepository<RefreshToken, UUID>, RefreshTokenStore {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select token from RefreshToken token where token.tokenHash = :hash")
  Optional<RefreshToken> findByHashForUpdate(@Param("hash") String hash);

  @Override
  @Query(
      "select token from RefreshToken token where token.familiaId = :familiaId and token.revogadoEm is null")
  List<RefreshToken> ativosDaFamilia(@Param("familiaId") UUID familiaId);

  @Override
  @Query(
      "select token from RefreshToken token where token.usuario.id = :usuarioId and token.revogadoEm is null")
  List<RefreshToken> ativosDoUsuario(@Param("usuarioId") UUID usuarioId);

  @Modifying
  @Query(
      "delete from RefreshToken token where token.expiraEm < :limite or token.revogadoEm < :limite")
  int removerAntigos(@Param("limite") Instant limite);
}
