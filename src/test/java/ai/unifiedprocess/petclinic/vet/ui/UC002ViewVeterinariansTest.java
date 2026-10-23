package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.demo.petclinic.database.tables.records.SpecialtiesRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.VetSpecialtiesRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.VetsRecord;
import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static ai.unifiedprocess.demo.petclinic.database.Tables.SPECIALTIES;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VET_SPECIALTIES;
import static org.jooq.impl.DSL.name;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-002: View Veterinarians.
 *
 * <p>Uses the V2 seed data (vets + specialties from the main migrations;
 * no V1000 fixtures are needed here). Grid rows are pulled via
 * {@code test(grid).getRow(i)} so the test never touches the repository
 * directly — the grid's own lazy loader executes the real query.
 */
class UC002ViewVeterinariansTest extends PetClinicTestBase {

    /** Vets in V2__seed_reference_data.sql. */
    private static final int SEEDED_VETS = 6;
    /** Enough vets to fill several of the grid's chunks (default page size 50). */
    private static final int EXTRA_VETS = 200;

    @Autowired
    private DSLContext dsl;

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-003")
    void vetsViewIsReachableWithoutAuthentication() {
        assertDoesNotThrow(() -> navigate(ViewVeterinariansView.class),
                "Expected /vets to render without authentication");
    }

    @Test
    @UseCase(id = "UC-002")
    void vetsGridContainsSeededVets() {
        navigate(ViewVeterinariansView.class);
        Grid<Vet> grid = grid();
        int total = test(grid).size();
        assertTrue(total > 0, "Expected the seeded vets to appear in the grid");

        Vet first = test(grid).getRow(0);
        assertEquals("James", first.firstName(), "Expected first vet (by last name) to be James Carter");
        assertEquals("Carter", first.lastName());
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-002")
    void specialtiesAreListedAlphabeticallyWithinEachVet() {
        navigate(ViewVeterinariansView.class);

        // Dr. Douglas holds both 'surgery' and 'dentistry' per V2 seed data —
        // BR-002 requires the view to display them alphabetically.
        Vet douglas = findVetByLastName("Douglas");
        assertEquals(List.of("dentistry", "surgery"), douglas.specialties());
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-004")
    void vetsAreSortedByLastNameThenFirstName() {
        // Two more Carters around the seeded James Carter decide the tie by first name.
        dsl.insertInto(VETS, VETS.FIRST_NAME, VETS.LAST_NAME)
                .values("Zoe", "Carter")
                .values("Adam", "Carter")
                .execute();

        navigate(ViewVeterinariansView.class);
        Grid<Vet> grid = grid();

        List<String> firstRows = List.of(0, 1, 2, 3).stream()
                .map(i -> test(grid).getRow(i))
                .map(vet -> vet.firstName() + " " + vet.lastName())
                .toList();
        assertEquals(List.of("Adam Carter", "James Carter", "Zoe Carter", "Linda Douglas"), firstRows);
    }

    @Test
    @UseCase(id = "UC-002")
    void vetWithoutSpecialtiesIsRenderedAsNone() {
        navigate(ViewVeterinariansView.class);

        // Dr. Carter has no specialties in the V2 seed data.
        Vet carter = findVetByLastName("Carter");
        assertTrue(carter.specialties().isEmpty());
        assertEquals("none", carter.specialtiesLabel());
    }

    @Test
    @UseCase(id = "UC-002")
    void gridColumnsMatchSpecification() {
        navigate(ViewVeterinariansView.class);
        List<String> headers = grid().getColumns().stream()
                .map(Grid.Column::getHeaderText)
                .toList();
        assertEquals(List.of("First Name", "Last Name", "Specialties"), headers);
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001")
    void vetsGridFetchesRowsLazily() {
        navigate(ViewVeterinariansView.class);
        Grid<Vet> grid = grid();

        // BR-001: rows come from the backend per requested range. An in-memory
        // provider would mean the whole list was loaded up front — precisely
        // what "no fixed page size, no page controls" rules out.
        assertFalse(grid.getDataProvider().isInMemory(),
                "BR-001: expected a lazy backend data provider, not an in-memory one");

        // The six seeded vets fit into the first chunk; fetching a second one is
        // nextChunkIsFetchedWhenScrollingPastTheFirst. Vets are ordered by last name.
        assertEquals(6, test(grid).size());
        assertEquals("Carter", test(grid).getRow(0).lastName());
        assertEquals("Stevens", test(grid).getRow(5).lastName(),
                "Expected the last seeded vet to be the last row");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001")
    void nextChunkIsFetchedWhenScrollingPastTheFirst() {
        // They sort after the seeded "Stevens", so extra vet number i lands on
        // row SEEDED_VETS + i.
        insertExtraVets(EXTRA_VETS);
        navigate(ViewVeterinariansView.class);
        Grid<Vet> grid = grid();
        int chunk = grid.getPageSize();
        assertTrue(SEEDED_VETS + EXTRA_VETS > 2 * chunk,
                "Precondition: a row past the first chunk can only arrive through a second fetch");

        int rowInSecondChunk = chunk + 1;
        assertEquals(SEEDED_VETS + EXTRA_VETS, test(grid).size(),
                "Step 4: expected all vets to be loaded once the grid is scrolled to the end");
        assertEquals(extraVetLastName(rowInSecondChunk - SEEDED_VETS),
                test(grid).getRow(rowInSecondChunk).lastName(),
                "Step 4: expected the row past the first chunk to be fetched and appended");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001")
    void eachFetchReturnsOnlyTheRequestedChunk() {
        insertExtraVets(EXTRA_VETS);
        navigate(ViewVeterinariansView.class);
        Grid<Vet> grid = grid();
        int chunk = grid.getPageSize();
        assertTrue(SEEDED_VETS + EXTRA_VETS > 2 * chunk,
                "Precondition: more vets than two chunks hold");

        // NFR-002: one scroll step costs one bounded fetch, however many vets exist.
        @SuppressWarnings("unchecked")
        DataProvider<Vet, Void> provider = (DataProvider<Vet, Void>) grid.getDataProvider();
        List<Vet> secondChunk = provider
                .fetch(new Query<>(chunk, chunk, List.of(), null, null))
                .toList();

        assertEquals(chunk, secondChunk.size(),
                "Expected one fetch to return exactly one chunk, not the whole list");
        assertEquals(extraVetLastName(chunk - SEEDED_VETS), secondChunk.getFirst().lastName(),
                "Expected the second chunk to start right after the first");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001")
    void gridKnowsTheNumberOfVetsBeforeItIsScrolled() {
        insertExtraVets(EXTRA_VETS);
        navigate(ViewVeterinariansView.class);
        Grid<Vet> grid = grid();

        // The count sizes the grid up front, so the scrollbar spans every vet
        // although only the first chunk has been fetched.
        assertTrue(grid.getDataCommunicator().isDefinedSize(),
                "Expected the grid to be sized by a count, not to grow while scrolling");
        assertEquals(SEEDED_VETS + EXTRA_VETS, grid.getDataCommunicator().getItemCount(),
                "Expected the grid to know every vet before it is scrolled");
    }

    @Test
    @UseCase(id = "UC-002")
    void veterinariansCannotBeMaintainedFromTheList() {
        // C-011: vets and specialties are maintained outside the application,
        // so the list offers no action and no inline editing.
        ViewVeterinariansView view = navigate(ViewVeterinariansView.class);

        assertTrue(find(Button.class).from(view).all().isEmpty(),
                "C-011: expected no action on the veterinarians list");
        // A bean-typed grid always carries a binder; a column without an editor
        // component is what keeps a cell from being edited.
        for (Grid.Column<Vet> column : grid().getColumns()) {
            assertNull(column.getEditorComponent(),
                    "C-011: expected no editor on the " + column.getHeaderText() + " column");
        }
    }

    @Test
    @UseCase(id = "UC-002")
    void viewingTheVeterinariansModifiesNoData() {
        Result<VetsRecord> vetsBefore = dsl.selectFrom(VETS).orderBy(VETS.ID).fetch();
        Result<SpecialtiesRecord> specialtiesBefore = dsl.selectFrom(SPECIALTIES).orderBy(SPECIALTIES.ID).fetch();
        Result<VetSpecialtiesRecord> assignmentsBefore = dsl.selectFrom(VET_SPECIALTIES)
                .orderBy(VET_SPECIALTIES.VET_ID, VET_SPECIALTIES.SPECIALTY_ID).fetch();

        navigate(ViewVeterinariansView.class);
        Grid<Vet> grid = grid();
        test(grid).getRow(test(grid).size() - 1);

        assertEquals(vetsBefore, dsl.selectFrom(VETS).orderBy(VETS.ID).fetch(),
                "Success postcondition: expected the vets to be unchanged");
        assertEquals(specialtiesBefore, dsl.selectFrom(SPECIALTIES).orderBy(SPECIALTIES.ID).fetch(),
                "Success postcondition: expected the specialties to be unchanged");
        assertEquals(assignmentsBefore, dsl.selectFrom(VET_SPECIALTIES)
                        .orderBy(VET_SPECIALTIES.VET_ID, VET_SPECIALTIES.SPECIALTY_ID).fetch(),
                "Success postcondition: expected the vets' specialties to be unchanged");
    }

    @Test
    @UseCase(id = "UC-002")
    void dataAccessErrorShowsTheApplicationErrorViewInsteadOfTheList() {
        // Failure postcondition. Renaming the table inside the test transaction
        // makes the grid's real query fail; the rename is rolled back with it.
        dsl.alterTable(VETS).renameTo(name("vets_unavailable")).execute();

        // Navigate by location: the router is expected to reroute to the error view.
        UI.getCurrent().navigate("vets");
        roundTrip();

        // The application error view is identified by what the user sees, not by
        // which of the UC-010 error view classes rendered it.
        assertDoesNotThrow(() -> find(H2.class).withText("Something happened...").single(),
                "Failure postcondition: expected the application error view on a data-access error");
        Paragraph message = find(Paragraph.class).withClassName("secondary-text").single();
        assertFalse(message.getText().toLowerCase().contains("select"),
                "Expected no SQL on the error view, got: " + message.getText());
        assertTrue(find(Grid.class).all().isEmpty(),
                "Failure postcondition: expected no vet list to be displayed");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A1: No Veterinarians Registered")
    void gridRendersWithoutRowsWhenNoVetsAreRegistered() {
        // A1 is the *absence* of data, which V2 seed data cannot express —
        // seeding can only add rows. Emptying the tables here is safe because
        // PetClinicTestBase wraps every test in a transaction that is rolled
        // back, so no other test observes it. The view still runs the real
        // VetRepository query, now against an empty table.
        dsl.deleteFrom(VET_SPECIALTIES).execute();
        dsl.deleteFrom(VETS).execute();

        navigate(ViewVeterinariansView.class);

        assertEquals(0, test(grid()).size(),
                "Expected an empty grid when no veterinarians are registered");
        // A1 must render the grid with no rows — not degrade into an error page.
        assertDoesNotThrow(() -> find(ViewVeterinariansView.class).single(),
                "Expected ViewVeterinariansView to still render when there are no vets");
        assertEquals(List.of("First Name", "Last Name", "Specialties"),
                grid().getColumns().stream().map(Grid.Column::getHeaderText).toList(),
                "Expected the column headers to survive the empty result set");
    }

    /** Inserts vets that sort after every seeded one; rolled back with the test. */
    private void insertExtraVets(int count) {
        var insert = dsl.insertInto(VETS, VETS.FIRST_NAME, VETS.LAST_NAME);
        for (int i = 0; i < count; i++) {
            insert = insert.values("Extra", extraVetLastName(i));
        }
        insert.execute();
    }

    private static String extraVetLastName(int i) {
        return "Zulu%04d".formatted(i);
    }

    @SuppressWarnings("unchecked")
    private Grid<Vet> grid() {
        return (Grid<Vet>) find(Grid.class).single();
    }

    private Vet findVetByLastName(String lastName) {
        Grid<Vet> grid = grid();
        int size = test(grid).size();
        for (int i = 0; i < size; i++) {
            Vet vet = test(grid).getRow(i);
            if (lastName.equals(vet.lastName())) {
                return vet;
            }
        }
        throw new AssertionError("Vet with last name '" + lastName + "' not found in grid");
    }
}
