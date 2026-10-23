package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.owner.domain.OwnerListing;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VISITS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-004: Find Owners by Last Name.
 *
 * <p>Exercises the seed owners loaded by {@code V2__seed_reference_data.sql}:
 * <ul>
 *   <li>"Davis" (Betty and Harold) gives the prefix-match case,</li>
 *   <li>"Franklin" is the unique last-name for the exactly-one-match shortcut (prefix "Fra"),</li>
 *   <li>"Nonexistent" drives the no-match branch.</li>
 * </ul>
 */
class UC004FindOwnersByLastNameTest extends PetClinicTestBase {

    @Autowired
    private DSLContext dsl;

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-001")
    void prefixSearchReturnsMatchingOwners() {
        navigate(FindOwnersByLastNameView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("Dav");
        test(find(Button.class).withText("Find Owner").single()).click();

        assertTrue(find(Grid.class).single().isVisible());
        // Betty Davis + Harold Davis
        assertEquals(2, test(find(Grid.class).single()).size());
    }

    @Test
    @UseCase(id = "UC-004")
    void ownersListShowsNameAddressCityTelephoneAndPets() {
        search("Dav");
        Grid<OwnerListing> grid = find(Grid.class).single();

        // Betty Davis sorts before Harold Davis (BR-004); seed row of owner 2 with pet Basil.
        assertEquals("Betty Davis", test(grid).getCellText(0, 0));
        assertEquals("638 Cardinal Ave.", test(grid).getCellText(0, 1));
        assertEquals("Sun Prairie", test(grid).getCellText(0, 2));
        assertEquals("6085551749", test(grid).getCellText(0, 3));
        assertEquals("Basil", test(grid).getCellText(0, 4));
    }

    @Test
    @UseCase(id = "UC-004")
    void ownersListShowsEveryPetOfAnOwner() {
        search("Es");
        Grid<OwnerListing> grid = find(Grid.class).single();

        // Maria Escobito (Mulligan), then Carlos Estaban, who owns Lucky and Sly.
        assertEquals("Carlos Estaban", test(grid).getCellText(1, 0));
        assertEquals("Lucky Sly", test(grid).getCellText(1, 4));
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-003", scenario = "A1: Empty Last-Name Search")
    void emptyLastNameReturnsAllOwners() {
        navigate(FindOwnersByLastNameView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("");
        test(find(Button.class).withText("Find Owner").single()).click();

        assertTrue(find(Grid.class).single().isVisible());
        // all 10 seed owners
        assertEquals(10, test(find(Grid.class).single()).size());
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-003", scenario = "A1: Empty Last-Name Search")
    void emptyLastNameWithOneOwnerRecordedNavigatesToOwnerDetails() {
        // The deletes run in the test transaction and are rolled back afterwards.
        keepOnlyOwner(OWNER_FRANKLIN_ID);

        searchAllOwners();

        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertEquals("owners/" + OWNER_FRANKLIN_ID, path, "BR-003: one recorded owner opens Owner Details");
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-003", scenario = "A1: Empty Last-Name Search")
    void emptyLastNameWithNoOwnerRecordedAttachesNotFound() {
        // The deletes run in the test transaction and are rolled back afterwards.
        keepOnlyOwner(null);

        searchAllOwners();

        TextField lastNameField = find(TextField.class).withCaption("Last name").single();
        assertTrue(lastNameField.isInvalid(), "BR-003: no recorded owner ends in 'not found'");
        assertEquals("not found", lastNameField.getErrorMessage());
        assertTrue(find(Grid.class).all().isEmpty(), "Expected results grid to be hidden");
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A2: Exactly One Match")
    void exactlyOneMatchNavigatesDirectlyToOwnerDetails() {
        navigate(FindOwnersByLastNameView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("Fra");
        test(find(Button.class).withText("Find Owner").single()).click();

        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertEquals("owners/" + OWNER_FRANKLIN_ID, path);
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A3: No Match")
    void noMatchAttachesNotFoundToLastNameField() {
        navigate(FindOwnersByLastNameView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("Nonexistent");
        test(find(Button.class).withText("Find Owner").single()).click();

        TextField lastNameField = find(TextField.class).withCaption("Last name").single();
        assertTrue(lastNameField.isInvalid());
        assertEquals("not found", lastNameField.getErrorMessage());
        assertEquals("Nonexistent", lastNameField.getValue(), "Expected the entered search term to be kept");
        assertTrue(find(Grid.class).all().isEmpty(), "Expected results grid to be hidden");
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-001")
    void prefixSearchIgnoresCase() {
        for (String term : List.of("dav", "DAV", "dAv")) {
            search(term);

            assertFalse(find(TextField.class).withCaption("Last name").single().isInvalid(),
                    "BR-001: '" + term + "' must match 'Davis'");
            // Betty Davis + Harold Davis, the same as for "Dav"
            assertEquals(2, test(find(Grid.class).single()).size(), "Expected both Davises for '" + term + "'");
        }
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-001")
    void substringThatIsNotAPrefixFindsNoOwner() {
        search("avis");

        TextField lastNameField = find(TextField.class).withCaption("Last name").single();
        assertTrue(lastNameField.isInvalid(), "BR-001: 'avis' is inside 'Davis' but not its beginning");
        assertEquals("not found", lastNameField.getErrorMessage());
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-001")
    void surroundingSpacesInTheSearchTermAreIgnored() {
        search("  Dav  ");

        // Betty Davis + Harold Davis, the same as for "Dav"
        assertEquals(2, test(find(Grid.class).single()).size());
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-003", scenario = "A1: Empty Last-Name Search")
    void lastNameOfOnlySpacesReturnsAllOwners() {
        search("   ");

        assertFalse(find(TextField.class).withCaption("Last name").single().isInvalid(),
                "BR-003: a blank search is not a validation error");
        // all 10 seed owners
        assertEquals(10, test(find(Grid.class).single()).size());
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A3: No Match")
    void newSearchAfterNoMatchClearsTheErrorAndShowsTheResult() {
        search("Nonexistent");
        assertTrue(find(TextField.class).withCaption("Last name").single().isInvalid());

        // A3 continues at step 3 on the same view: the user adjusts the term.
        test(find(TextField.class).withCaption("Last name").single()).setValue("Dav");
        test(find(Button.class).withText("Find Owner").single()).click();

        assertFalse(find(TextField.class).withCaption("Last name").single().isInvalid(),
                "Expected the 'not found' error to be cleared by the new search");
        assertEquals(2, test(find(Grid.class).single()).size());
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A4: Scroll Through Results")
    void scrollingFetchesTheNextSetOfOwners() {
        searchAllOwners();
        Grid<OwnerListing> grid = find(Grid.class).single();

        // Ten seed owners fit into a single fetch of the grid's default page,
        // so the test asks the grid's data provider for the ranges a scroll
        // would request: the first three rows, then the next three.
        List<OwnerListing> firstSet = fetch(grid, 0, 3);
        List<OwnerListing> nextSet = fetch(grid, 3, 3);

        assertEquals(List.of(test(grid).getRow(0), test(grid).getRow(1), test(grid).getRow(2)), firstSet);
        assertEquals(List.of(test(grid).getRow(3), test(grid).getRow(4), test(grid).getRow(5)), nextSet,
                "Expected the next set to continue where the first one ended");
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-004")
    void ownersAreSortedByLastNameThenFirstName() {
        searchAllOwners();
        Grid<OwnerListing> grid = find(Grid.class).single();

        // Betty and Harold Davis share a last name, so the first name decides.
        List<String> expected = List.of(
                "Jeff Black", "Jean Coleman", "Betty Davis", "Harold Davis", "Maria Escobito",
                "Carlos Estaban", "George Franklin", "Peter McTavish", "Eduardo Rodriquez", "David Schroeder");
        for (int row = 0; row < expected.size(); row++) {
            assertEquals(expected.get(row), test(grid).getCellText(row, 0), "BR-004: unexpected owner in row " + row);
        }
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-002", requirements = "NFR-002")
    void eachFetchReturnsOnlyTheRequestedRows() {
        searchAllOwners();
        Grid<OwnerListing> grid = find(Grid.class).single();

        // BR-002 / NFR-002: a scroll step fetches the rows it asks for, never
        // every matching owner, so its cost does not grow with the owner count.
        assertEquals(10, test(grid).size());
        assertEquals(3, fetch(grid, 0, 3).size());
        assertEquals(3, fetch(grid, 6, 3).size());
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-002", requirements = "NFR-001")
    void ownersGridFetchesRowsLazily() {
        searchAllOwners();
        Grid<?> grid = find(Grid.class).single();

        // BR-002: rows are fetched from the backend per requested range. An
        // in-memory provider would mean the whole result set was loaded up
        // front, which is exactly what the rule forbids.
        assertFalse(grid.getDataProvider().isInMemory(),
                "BR-002: expected a lazy backend data provider, not an in-memory one");
    }

    /**
     * Deletes every owner except {@code ownerId} (every owner when it is
     * {@code null}), together with their pets and visits.
     */
    private void keepOnlyOwner(Integer ownerId) {
        var removedOwners = ownerId == null ? OWNERS.ID.isNotNull() : OWNERS.ID.ne(ownerId);
        var removedPets = PETS.OWNER_ID.in(dsl.select(OWNERS.ID).from(OWNERS).where(removedOwners));
        dsl.deleteFrom(VISITS).where(VISITS.PET_ID.in(dsl.select(PETS.ID).from(PETS).where(removedPets))).execute();
        dsl.deleteFrom(PETS).where(removedPets).execute();
        dsl.deleteFrom(OWNERS).where(removedOwners).execute();
    }

    private void searchAllOwners() {
        search("");
    }

    private void search(String lastName) {
        navigate(FindOwnersByLastNameView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue(lastName);
        test(find(Button.class).withText("Find Owner").single()).click();
    }

    @SuppressWarnings("unchecked")
    private static List<OwnerListing> fetch(Grid<OwnerListing> grid, int offset, int limit) {
        DataProvider<OwnerListing, Object> provider = (DataProvider<OwnerListing, Object>) grid.getDataProvider();
        return provider.fetch(new Query<>(offset, limit, List.of(), null, null)).toList();
    }
}
