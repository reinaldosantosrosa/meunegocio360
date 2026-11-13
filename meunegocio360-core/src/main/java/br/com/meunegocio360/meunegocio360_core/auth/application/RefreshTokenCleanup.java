package br.com.meunegocio360.meunegocio360_core.auth.application;

import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanup {
  private final AuthService authService;
  private final Clock clock;

  public RefreshTokenCleanup(AuthService authService, Clock clock) {
    this.authService = authService;
    this.clock = clock;
  }

  @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
  public void removerTokensAntigos() {
    authService.removerTokensAntigos(clock.instant().minus(Duration.ofDays(30)));
  }
}
