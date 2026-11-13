package br.com.meunegocio360.meunegocio360_core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class AuthArchitectureTest {
  @Test
  void camadasInternasNaoAcessamInfraestruturaDiretamente() {
    JavaClasses classes =
        new ClassFileImporter().importPackages("br.com.meunegocio360.meunegocio360_core.auth");

    noClasses()
        .that()
        .resideInAnyPackage("..auth.api..", "..auth.application..", "..auth.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..auth.infra..")
        .check(classes);
  }
}
