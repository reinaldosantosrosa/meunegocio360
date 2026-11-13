package br.com.meunegocio360.meunegocio360_core.config;

import br.com.meunegocio360.meunegocio360_core.common.api.OriginGuardFilter;
import br.com.meunegocio360.meunegocio360_core.common.api.ProblemAuthenticationEntryPoint;
import br.com.meunegocio360.meunegocio360_core.observability.RequestIdFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfiguration {
  private final RequestIdFilter requestIdFilter;
  private final OriginGuardFilter originGuardFilter;
  private final ProblemAuthenticationEntryPoint problemAuthenticationEntryPoint;

  public SecurityConfiguration(
      RequestIdFilter requestIdFilter,
      OriginGuardFilter originGuardFilter,
      ProblemAuthenticationEntryPoint problemAuthenticationEntryPoint) {
    this.requestIdFilter = requestIdFilter;
    this.originGuardFilter = originGuardFilter;
    this.problemAuthenticationEntryPoint = problemAuthenticationEntryPoint;
  }

  @Bean
  FilterRegistrationBean<RequestIdFilter> requestIdServletRegistration(RequestIdFilter filter) {
    FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  FilterRegistrationBean<OriginGuardFilter> originGuardServletRegistration(
      OriginGuardFilter filter) {
    FilterRegistrationBean<OriginGuardFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout")
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            resource ->
                resource
                    .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(problemAuthenticationEntryPoint))
        .exceptionHandling(
            errors -> errors.authenticationEntryPoint(problemAuthenticationEntryPoint))
        .addFilterBefore(requestIdFilter, CorsFilter.class)
        .addFilterAfter(originGuardFilter, RequestIdFilter.class)
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") String origins) {
    CorsConfiguration configuration = new CorsConfiguration();
    List<String> allowedOrigins =
        Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowCredentials(true);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
    configuration.setExposedHeaders(List.of("X-Request-Id", "Retry-After", "WWW-Authenticate"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
