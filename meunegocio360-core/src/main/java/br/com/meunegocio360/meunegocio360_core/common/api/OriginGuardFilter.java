package br.com.meunegocio360.meunegocio360_core.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OriginGuardFilter extends OncePerRequestFilter {
  private final List<String> allowedOrigins;
  private final ObjectMapper objectMapper;

  public OriginGuardFilter(
      @Value("${app.cors.allowed-origins}") String origins, ObjectMapper objectMapper) {
    this.allowedOrigins =
        Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    String origin = request.getHeader(HttpHeaders.ORIGIN);
    boolean refreshOrLogout =
        path.equals("/api/v1/auth/refresh") || path.equals("/api/v1/auth/logout");
    if (refreshOrLogout
        && "POST".equals(request.getMethod())
        && origin != null
        && !allowedOrigins.contains(origin)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/problem+json");
      objectMapper.writeValue(
          response.getOutputStream(),
          ProblemBody.criar(
              403, "ORIGIN_NOT_ALLOWED", "Acesso proibido", "Origem não permitida.", path, null));
      return;
    }
    filterChain.doFilter(request, response);
  }
}
