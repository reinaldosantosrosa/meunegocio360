package br.com.meunegocio360.meunegocio360_core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "app.auth.jwt.secret=integration-test-only-secret-with-at-least-32-bytes",
      "app.auth.cookie.secure=false"
    })
@Testcontainers(disabledWithoutDocker = true)
class AuthDatabaseIntegrationTest {
  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.4")
          .withDatabaseName("meunegocio360_core")
          .withUsername("test")
          .withPassword("test");

  @Autowired private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
  }

  @Test
  void migrationsCriamAsTabelasComIndicesDeAutenticacao() {
    Integer tabelas =
        jdbcTemplate.queryForObject(
            "select count(*) from information_schema.tables "
                + "where table_schema = database() and table_name in ('usuario', 'refresh_token')",
            Integer.class);
    assertEquals(2, tabelas);
  }
}
