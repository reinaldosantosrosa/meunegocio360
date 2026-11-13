package br.com.meunegocio360.meunegocio360_core.common.api;

import br.com.meunegocio360.meunegocio360_core.observability.RequestIdFilter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

public final class ProblemBody {
  private ProblemBody() {}

  public static Map<String, Object> criar(
      int status, String code, String title, String detail, String instance, Object errors) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "https://meunegocio360.com.br/problemas/" + code.toLowerCase());
    body.put("title", title);
    body.put("status", status);
    body.put("detail", detail);
    body.put("instance", instance);
    body.put("code", code);
    body.put("traceId", MDC.get(RequestIdFilter.MDC_KEY));
    if (errors != null) {
      body.put("errors", errors);
    }
    return body;
  }
}
