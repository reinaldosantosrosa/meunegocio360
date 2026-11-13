package br.com.meunegocio360.meunegocio360_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Meunegocio360CoreApplication {

  public static void main(String[] args) {
    SpringApplication.run(Meunegocio360CoreApplication.class, args);
  }
}
