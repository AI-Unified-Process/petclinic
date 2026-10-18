package ai.unifiedprocess.petclinic;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ai.unifiedprocess.petclinic.SpecDocuments.assertNoViolations;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Traceability sensor for the business rules of {@code docs/use_cases/}.
 *
 * <p>Business rules are numbered per use case, so {@code BR-001} means nothing
 * outside its own file. A rule several use cases share is stated once, in the
 * use case that introduced it, and the others cite it qualified, as
 * {@code UC-007 BR-001}, adding only what differs for them. A rule that follows
 * from a requirement names it, and the requirement is the project-wide id the
 * rule is traced by. Nothing in markdown keeps these references honest: a
 * renumbered rule orphans every citation, and a copied rule drifts as soon as
 * one copy is edited. This sensor closes that loop, the way
 * {@link RequirementTraceabilityTest} closes the one between the catalogue and
 * the use cases.
 *
 * <p>What it checks:
 *
 * <ul>
 *   <li><strong>Citations resolve</strong> — every {@code UC-NNN BR-NNN} a use
 *       case cites names a rule heading that exists in that other use case.</li>
 *   <li><strong>Requirements are listed</strong> — every requirement a business
 *       rule names is on its use case's {@code **Requirements:**} line, so the
 *       trace from requirement to use case does not depend on reading rules.</li>
 *   <li><strong>A rule is stated once</strong> — no two use cases carry the same
 *       rule text; the second one cites the first.</li>
 * </ul>
 *
 * <p>Failures list every violation at once rather than stopping at the first,
 * so the report reads as a work list.
 */
@Tag("sensor")
class BusinessRuleTraceabilityTest {

    private static final Path USE_CASE_DIR = Path.of("docs", "use_cases");

    /** {@code ### BR-001: Unique Pet Name per Owner} — one rule of one use case. */
    private static final Pattern RULE_HEADING =
            Pattern.compile("^### (BR-\\d{3}):[ \t]*+.+$", Pattern.MULTILINE);
    private static final Pattern USE_CASE_ID_LINE =
            Pattern.compile("^\\*\\*Use Case ID:\\*\\*[ \t]*(UC-\\d{3})[ \t]*$", Pattern.MULTILINE);
    /** {@code **Requirements:** [FR-014, FR-015, C-011](../requirements.md)} */
    private static final Pattern REQUIREMENTS_LINE =
            Pattern.compile("^\\*\\*Requirements:\\*\\*[ \t]*+(.*)$", Pattern.MULTILINE);
    /** A {@code UC-NNN BR-NNN} citation, wherever it appears in prose. */
    private static final Pattern CITATION = Pattern.compile("(UC-\\d{3})[ \t]+(BR-\\d{3})");
    private static final Pattern REQUIREMENT_ID = Pattern.compile("(?<![A-Za-z])(?:FR|NFR|C)-\\d{3}");
    /** Shorter rule texts ("Applies on update as well.") are too generic to count as copies. */
    private static final int MIN_COPIED_TEXT = 40;

    private static Map<String, UseCase> useCases;

    /** One {@code ### BR-NNN} section of one use case, without its heading. */
    private record Rule(String id, String body) {

        /** The rule's own statement: its body with citations and requirement links removed. */
        String statement() {
            String text = body.replaceAll("\\(\\[[^]]*]\\([^)]*\\)\\)", "");
            return SpecDocuments.normalize(CITATION.matcher(text).replaceAll(""));
        }
    }

    /** The parts of one {@code docs/use_cases/UC-NNN-*.md} this sensor reads. */
    private record UseCase(String id, Set<String> requirements, Map<String, Rule> rules, Path file) {

        String location(Rule rule) {
            return id + " " + rule.id() + " (" + file + ")";
        }
    }

    @BeforeAll
    static void loadUseCases() throws IOException {
        useCases = readUseCases();
    }

    // --- Guard -----------------------------------------------------------------

    /**
     * Without this, a wrong working directory would make every check below pass
     * over an empty input set, a sensor that reports green because it is blind.
     */
    @Test
    void businessRulesAreDiscovered() {
        assertFalse(useCases.values().stream().allMatch(useCase -> useCase.rules().isEmpty()),
                "No '### BR-NNN:' rule found in " + USE_CASE_DIR.toAbsolutePath()
                        + " Tests must run with the project root as working directory");
    }

    // --- Citations resolve -----------------------------------------------------

    @Test
    void everyCitedRuleExists() {
        List<String> violations = new ArrayList<>();
        useCases.values().forEach(useCase -> useCase.rules().values().forEach(rule -> {
            Matcher citation = CITATION.matcher(rule.body());
            while (citation.find()) {
                UseCase cited = useCases.get(citation.group(1));
                if (cited == null) {
                    violations.add(useCase.location(rule) + " cites " + citation.group()
                            + ", but there is no " + USE_CASE_DIR + "/" + citation.group(1) + "-*.md");
                } else if (!cited.rules().containsKey(citation.group(2))) {
                    violations.add(useCase.location(rule) + " cites " + citation.group()
                            + ", but " + cited.id() + " has no '### " + citation.group(2) + ":' rule");
                }
            }
        }));
        assertNoViolations("Business rules citing a rule that does not exist", violations.stream());
    }

    // --- Requirements are listed -----------------------------------------------

    @Test
    void everyRequirementARuleNamesIsListedByItsUseCase() {
        List<String> violations = new ArrayList<>();
        useCases.values().forEach(useCase -> useCase.rules().values().forEach(rule ->
                idsIn(REQUIREMENT_ID, rule.body()).stream()
                        .filter(id -> !useCase.requirements().contains(id))
                        .forEach(id -> violations.add(useCase.location(rule) + " names " + id
                                + ", but the '**Requirements:**' line of " + useCase.id() + " does not list it"))));
        assertNoViolations("Business rules naming a requirement their use case does not list", violations.stream());
    }

    // --- A rule is stated once -------------------------------------------------

    @Test
    void noRuleTextIsCopiedBetweenUseCases() {
        List<String> violations = new ArrayList<>();
        Map<String, String> firstStatedBy = new LinkedHashMap<>();
        useCases.values().forEach(useCase -> useCase.rules().values().forEach(rule -> {
            String statement = rule.statement().toLowerCase();
            if (statement.length() < MIN_COPIED_TEXT) {
                return;
            }
            String first = firstStatedBy.putIfAbsent(statement, useCase.id() + " " + rule.id());
            if (first != null && !first.startsWith(useCase.id())) {
                violations.add(useCase.location(rule) + " repeats the text of " + first
                        + "; state it once and cite '" + first + "'");
            }
        }));
        assertNoViolations("Business rules copied from another use case", violations.stream());
    }

    // --- Reading the use cases -------------------------------------------------

    private static Map<String, UseCase> readUseCases() throws IOException {
        Map<String, UseCase> parsed = new LinkedHashMap<>();
        for (Path file : SpecDocuments.filesIn(USE_CASE_DIR, "UC-")) {
            String markdown = Files.readString(file);
            String id = SpecDocuments.require(USE_CASE_ID_LINE, markdown, file, "**Use Case ID:** UC-NNN");
            String requirements = SpecDocuments.require(REQUIREMENTS_LINE, markdown, file,
                    "**Requirements:** [FR-NNN, …](../requirements.md)");
            parsed.put(id, new UseCase(id, idsIn(REQUIREMENT_ID, requirements), readRules(markdown), file));
        }
        return parsed;
    }

    private static Map<String, Rule> readRules(String markdown) {
        Map<String, Rule> rules = new LinkedHashMap<>();
        Matcher heading = RULE_HEADING.matcher(markdown);
        int bodyStart = -1;
        String ruleId = null;
        while (heading.find()) {
            if (ruleId != null) {
                rules.put(ruleId, new Rule(ruleId, scoped(markdown.substring(bodyStart, heading.start()))));
            }
            ruleId = heading.group(1);
            bodyStart = heading.end();
        }
        if (ruleId != null) {
            rules.put(ruleId, new Rule(ruleId, scoped(markdown.substring(bodyStart))));
        }
        return rules;
    }

    /**
     * A rule section ends at the next '### ' heading, so the last body may run
     * into the following section of the document. Only what precedes the next
     * heading of any level belongs to the rule.
     */
    private static String scoped(String body) {
        return body.split("(?m)^#{1,6} ", 2)[0];
    }

    // --- Helpers ---------------------------------------------------------------

    private static Set<String> idsIn(Pattern pattern, String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

}
