package ai.unifiedprocess.petclinic.vet.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.welcome.ui.WelcomeView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.sidenav.SideNav;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * UC-002: View Veterinarians.
 *
 * <p>Expected values come from the specification and the seed data in
 * {@code V2__seed_reference_data.sql}: six vets — Carter (no specialty),
 * Leary (radiology), Douglas (surgery, dentistry), Ortega (surgery),
 * Stevens (radiology), Jenkins (no specialty). Rows a test adds are rolled
 * back with the test transaction of {@link PetClinicTestBase}.
 */
class UC002ViewVeterinariansTest extends PetClinicTestBase {

    private static final int FIRST_NAME = 0;
    private static final int LAST_NAME = 1;
    private static final int SPECIALTIES = 2;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    @UseCase(id = "UC-002")
    void veterinariansLinkInNavigationOpensTheVeterinariansView() {
        navigate(WelcomeView.class);

        // Step 1: the Visitor clicks the "Veterinarians" link in the navigation bar.
        test(find(SideNav.class).single()).clickItem("Veterinarians");

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEqualTo("vets");
        assertDoesNotThrow(() -> find(VeterinariansView.class).single(),
                "Expected the Veterinarians link to open the veterinarians view");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-002", "BR-004"})
    void gridShowsEveryVetWithNamesAndSpecialtiesSortedByLastName() {
        navigate(VeterinariansView.class);

        Grid<?> grid = vetsGrid();
        // Steps 2–3, BR-004 by last name, BR-002 specialties alphabetically, "none" without specialties.
        assertThat(rows(grid)).containsExactly(
                List.of("James", "Carter", "none"),
                List.of("Linda", "Douglas", "dentistry, surgery"),
                List.of("Sharon", "Jenkins", "none"),
                List.of("Helen", "Leary", "radiology"),
                List.of("Rafael", "Ortega", "surgery"),
                List.of("Henry", "Stevens", "radiology"));
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-004")
    void vetsWithTheSameLastNameAreSortedByFirstName() {
        // Inserted in reverse alphabetical order so the id order cannot produce the expected one.
        insertVet("Zoe", "Carter");
        insertVet("Adam", "Carter");

        navigate(VeterinariansView.class);

        Grid<?> grid = vetsGrid();
        assertThat(rows(grid).subList(0, 4)).extracting(row -> row.get(FIRST_NAME) + " " + row.get(LAST_NAME))
                .containsExactly("Adam Carter", "James Carter", "Zoe Carter", "Linda Douglas");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-002")
    void specialtiesOfAVetAreListedAlphabeticallyByName() {
        // Linda Douglas holds surgery (inserted first) and dentistry.
        navigate(VeterinariansView.class);

        Grid<?> grid = vetsGrid();
        int douglas = rowOf(grid, "Linda", "Douglas");
        assertThat(test(grid).getCellText(douglas, SPECIALTIES)).isEqualTo("dentistry, surgery");
    }

    @Test
    @UseCase(id = "UC-002")
    void vetWithoutSpecialtiesShowsNone() {
        navigate(VeterinariansView.class);

        Grid<?> grid = vetsGrid();
        assertThat(test(grid).getCellText(rowOf(grid, "James", "Carter"), SPECIALTIES)).isEqualTo("none");
        assertThat(test(grid).getCellText(rowOf(grid, "Sharon", "Jenkins"), SPECIALTIES)).isEqualTo("none");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001", requirements = {"NFR-001", "NFR-002"})
    void gridLoadsLazilyWithoutPageControlsAndScrollingReachesTheLastVet() {
        navigate(VeterinariansView.class);
        int chunkWithSeedData = vetsGrid().getPageSize();

        // More vets than one chunk, all sorting after the seed data ("Zz…" last names).
        int added = 3 * chunkWithSeedData;
        IntStream.rangeClosed(1, added).forEach(i -> insertVet("Vet", "Zz%04d".formatted(i)));

        navigate(VeterinariansView.class);
        Grid<?> grid = vetsGrid();

        // BR-001: rows come from the backend, not from an in-memory list.
        assertThat(grid.getDataProvider().isInMemory()).isFalse();
        // BR-001: no page controls.
        assertThat(find(Button.class).from(find(VeterinariansView.class).single()).all()).isEmpty();
        // BR-001 / NFR-002: rows fetched per scroll step do not grow with the number of vets.
        assertThat(grid.getPageSize()).isEqualTo(chunkWithSeedData);

        // Step 4: scrolling to the end loads every entry, the last one included.
        int total = 6 + added;
        assertThat(test(grid).size()).isEqualTo(total);
        assertThat(test(grid).getCellText(total - 1, LAST_NAME)).isEqualTo("Zz%04d".formatted(added));
        assertThat(test(grid).getCellText(6, LAST_NAME)).isEqualTo("Zz0001");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-003", requirements = "C-010")
    void veterinariansViewOpensWithoutAuthentication() {
        // No user is signed in; the view is reached anyway (UC-001 BR-001).
        UI.getCurrent().navigate("vets");

        assertDoesNotThrow(() -> find(VeterinariansView.class).single(),
                "Expected the veterinarians view to open for an anonymous visitor");
        assertThat(test(vetsGrid()).size()).isEqualTo(6);
    }

    @Test
    @UseCase(id = "UC-002")
    void viewingTheVeterinariansModifiesNoData() {
        List<String> vetsBefore = vetSnapshot();
        List<String> specialtiesBefore = vetSpecialtySnapshot();

        navigate(VeterinariansView.class);
        Grid<?> grid = vetsGrid();
        rows(grid);

        // Success postcondition: no data is modified.
        assertThat(vetSnapshot()).isEqualTo(vetsBefore);
        assertThat(vetSpecialtySnapshot()).isEqualTo(specialtiesBefore);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A1: No Veterinarians Registered")
    void noVeterinariansRendersAnEmptyGrid() {
        // Emptied inside the test transaction; the rollback restores the seed data.
        jdbcClient.sql("delete from vet_specialties").update();
        jdbcClient.sql("delete from vets").update();

        navigate(VeterinariansView.class);

        Grid<?> grid = vetsGrid();
        assertThat(test(grid).size()).isZero();
    }

    private Grid<?> vetsGrid() {
        return assertDoesNotThrow(() -> find(Grid.class).single(),
                "Expected exactly one veterinarians grid");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<List<String>> rows(Grid<?> grid) {
        var tester = test((Grid) grid);
        return IntStream.range(0, tester.size())
                .mapToObj(row -> List.of(
                        tester.getCellText(row, FIRST_NAME),
                        tester.getCellText(row, LAST_NAME),
                        tester.getCellText(row, SPECIALTIES)))
                .toList();
    }

    private int rowOf(Grid<?> grid, String firstName, String lastName) {
        List<List<String>> rows = rows(grid);
        int index = IntStream.range(0, rows.size())
                .filter(i -> rows.get(i).get(FIRST_NAME).equals(firstName)
                        && rows.get(i).get(LAST_NAME).equals(lastName))
                .findFirst()
                .orElse(-1);
        assertThat(index).as("row of %s %s", firstName, lastName).isNotNegative();
        return index;
    }

    private void insertVet(String firstName, String lastName) {
        jdbcClient.sql("insert into vets (first_name, last_name) values (?, ?)")
                .params(firstName, lastName)
                .update();
    }

    private List<String> vetSnapshot() {
        return jdbcClient.sql("select id || '|' || first_name || '|' || last_name from vets order by id")
                .query(String.class).list();
    }

    private List<String> vetSpecialtySnapshot() {
        return jdbcClient.sql("select vet_id || '|' || specialty_id from vet_specialties order by vet_id, specialty_id")
                .query(String.class).list();
    }
}
