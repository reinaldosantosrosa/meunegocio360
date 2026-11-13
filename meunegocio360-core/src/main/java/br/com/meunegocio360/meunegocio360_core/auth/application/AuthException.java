package br.com.meunegocio360.meunegocio360_core.auth.application;

public class AuthException extends RuntimeException {
  private final int status;
  private final String code;
  private final Long retryAfter;

  public AuthException(int status, String code, String message) {
    this(status, code, message, null);
  }

  public AuthException(int status, String code, String message, Long retryAfter) {
    super(message);
    this.status = status;
    this.code = code;
    this.retryAfter = retryAfter;
  }

  public int getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public Long getRetryAfter() {
    return retryAfter;
  }
}
