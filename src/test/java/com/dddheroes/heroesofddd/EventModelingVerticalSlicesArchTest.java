package com.dddheroes.heroesofddd;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModulithTest {
    @Test
    void test(){
        ApplicationModules.of(HeroesOfDDDApplication.class).verify();
    }
}

@DisplayName("Architecture Package Dependency Rules")
class PackageDependencyRulesTest {

    private static JavaClasses importedClasses;
    private static final String BASE_PACKAGE = "com.dddheroes.heroesofddd"; // Replace with your base package

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("Events package should not have any dependencies")
    void eventPackageShouldNotDependOnAnything() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..event..")
                .should().dependOnClassesThat()
                .resideInAPackage("..*..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Write package should only depend on events and shared")
    void writePackageShouldOnlyDependOnEventsAndShared() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..write..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..read..", "..automation..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Read package should only depend on events and shared")
    void readPackageShouldOnlyDependOnEventsAndShared() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..read..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..write..", "..automation..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Comprehensive test for all package dependency rules")
    void allPackageDependencyRules() {
        ArchRule eventRule = noClasses()
                .that().resideInAPackage("..event..")
                .should().dependOnClassesThat()
                .resideInAPackage("..*..");

        ArchRule writeRule = noClasses()
                .that().resideInAPackage("..write..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..read..", "..automation..");

        ArchRule readRule = noClasses()
                .that().resideInAPackage("..read..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..write..", "..automation..");

        CompositeArchRule combinedRules = CompositeArchRule.of(eventRule)
                                                           .and(writeRule)
                                                           .and(readRule);

        combinedRules.check(importedClasses);
    }
}