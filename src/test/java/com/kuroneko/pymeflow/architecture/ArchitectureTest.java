package com.kuroneko.pymeflow.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.kuroneko.pymeflow");

    @Test
    void domainDoesNotDependOnFrameworksPersistenceOrAdapters() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "com.fasterxml.jackson..",
                        "..infrastructure..",
                        "..interfaces..")
                .check(MAIN_CLASSES);
    }

    @Test
    void applicationDoesNotDependOnInfrastructureOrInterfaces() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..interfaces..")
                .check(MAIN_CLASSES);
    }

    @Test
    void domainAndApplicationDoNotHardcodeVerticalProviderOrBankLiterals() throws IOException {
        var forbiddenLiterals = Pattern.compile(
                "(?i).*(pharmacy-cl|farmacia|pharmacy|banco|bank|acquirer|adquirente|getnet|tuu|transbank).*"
        );

        assertThat(javaStringLiteralsInDomainAndApplication())
                .filteredOn(literal -> forbiddenLiterals.matcher(literal).matches())
                .isEmpty();
    }

    @Test
    void domainAndApplicationDoNotContainClinicalOrPrescriptionIdentifiers() throws IOException {
        var forbiddenIdentifiers = Pattern.compile(
                "(?i).*(clinical|prescription|prescripcion|prescripción|receta|patient|paciente|medication|medicamento).*"
        );

        assertThat(sourceLinesInDomainAndApplication())
                .filteredOn(line -> forbiddenIdentifiers.matcher(line).matches())
                .isEmpty();
    }

    private static List<String> javaStringLiteralsInDomainAndApplication() throws IOException {
        var literal = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
        return sourceLinesInDomainAndApplication().stream()
                .flatMap(line -> literal.matcher(line).results())
                .map(match -> match.group().substring(1, match.group().length() - 1))
                .toList();
    }

    private static List<String> sourceLinesInDomainAndApplication() throws IOException {
        try (var paths = Files.walk(Path.of("src/main/java/com/kuroneko/pymeflow"))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(ArchitectureTest::isDomainOrApplicationSource)
                    .flatMap(ArchitectureTest::readLines)
                    .toList();
        }
    }

    private static boolean isDomainOrApplicationSource(Path path) {
        var normalized = path.toString().replace('\\', '/');
        return normalized.contains("/domain/") || normalized.contains("/application/");
    }

    private static java.util.stream.Stream<String> readLines(Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read source file " + path, exception);
        }
    }
}
