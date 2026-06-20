package com.netkar.infrastructure.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HexagonalBoundaryTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.netkar");
    }

    @Test
    void domain_does_not_depend_on_spring_or_jakarta() {
        noClasses().that().resideInAPackage("com.netkar.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..")
            .check(classes);
    }

    @Test
    void domain_does_not_depend_on_application_or_infrastructure() {
        noClasses().that().resideInAPackage("com.netkar.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.netkar.application..", "com.netkar.infrastructure..")
            .check(classes);
    }

    @Test
    void application_does_not_depend_on_infrastructure_or_spring() {
        noClasses().that().resideInAPackage("com.netkar.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.netkar.infrastructure..", "org.springframework..")
            .check(classes);
    }
}
