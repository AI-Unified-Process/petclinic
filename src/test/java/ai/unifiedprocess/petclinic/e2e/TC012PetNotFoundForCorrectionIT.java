package ai.unifiedprocess.petclinic.e2e;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.TestCase;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.SideNavigationElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executes test case TC-012 (docs/test_cases/TC-012-pet-not-found-for-correction.md),
 * the "Owner on record? yes / Pet on record? no" path of the Update Pet process
 * (docs/processes/BP-006-update-pet.bpmn): a clinic user finds the owner
 * (UC-004), reviews his details (UC-005), does not find the animal the caller
 * names and changes nothing.
 *
 * <p>Read-only: the journey creates and changes nothing, so there is no cleanup.
 */
@TestCase(id = "TC-012", useCases = {"UC-004", "UC-005"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TC012PetNotFoundForCorrectionIT extends AbstractBasePlaywrightIT {

    // The seeded owner as V2__seed_reference_data.sql defines him.
    private static final String LAST_NAME = "Rodriquez";
    private static final String OWNER_NAME = "Eduardo Rodriquez";
    private static final String ADDRESS = "2693 Commerce St.";
    private static final String CITY = "McFarland";
    private static final String TELEPHONE = "6085558763";

    // The owner's pets as V2__seed_reference_data.sql defines them.
    private static final String FIRST_PET = "Jewel";
    private static final String FIRST_PET_BIRTH_DATE = "2000-03-07";
    private static final String SECOND_PET = "Rosy";
    private static final String SECOND_PET_BIRTH_DATE = "2001-04-17";

    // Test Data column of the Flow table: the animal the caller names.
    private static final String MISSING_PET = "Bruno";

    /** Accessible name of the drawer's navigation landmark. */
    private static final String NAV_LABEL = "Main menu";

    @LocalServerPort
    private int port;

    /** The Find Owners list before the journey, for the "nothing changed" diff. */
    private List<String> ownerRowsBefore;

    @Override
    public String getUrl() {
        return "http://localhost:%d/".formatted(port);
    }

    @Override
    public String getView() {
        return "owners/find";
    }

    @Test
    @DisplayName("TC-012: The animal whose details changed is not on record")
    void petNotFoundForCorrection() {
        ownerRowsBefore = snapshotOwnerRows();

        // Step 1: Find owner (UC-004)
        findOwner();

        // Step 2: Review owner details (UC-005)
        reviewOwnerDetails();

        // Step 3: Verify pet not on record
        verifyPetNotOnRecord();

        verifyPetsUntouched();
        verifyNothingChanged();
    }

    // --- Flow steps ----------------------------------------------------------

    /** Step 1: search Find Owners for the unique last name; the single match lands on Owner Details (UC-004 A2). */
    private void findOwner() {
        returnToFindOwners();
        TextFieldElement.getByLabel(page, "Last name").setValue(LAST_NAME);
        ButtonElement.getByText(page, "Find Owner").click();
        assertOnOwnerDetails();
    }

    /** Step 2: Owner Details shows the owner and his pets. */
    private void reviewOwnerDetails() {
        assertOwnerDetailsShown();
        assertPetListed(FIRST_PET);
        assertPetListed(SECOND_PET);
    }

    /** Step 3: the animal the caller names is not listed — "Pet on record? no". */
    private void verifyPetNotOnRecord() {
        assertPetNotListed(MISSING_PET);
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Edit Pet"))).hasCount(2);
    }

    /** Validation 1: both pets keep their names and birth dates. */
    private void verifyPetsUntouched() {
        findOwner();
        assertPetListed(FIRST_PET);
        assertPetListed(SECOND_PET);
        assertBirthDate(FIRST_PET, FIRST_PET_BIRTH_DATE);
        assertBirthDate(SECOND_PET, SECOND_PET_BIRTH_DATE);
    }

    /** The birth date is the line right below the pet's name, so it is tied to that pet. */
    private void assertBirthDate(String petName, String birthDate) {
        assertThat(page.getByRole(AriaRole.HEADING,
                        new Page.GetByRoleOptions().setName(petName).setExact(true))
                .locator("xpath=following-sibling::*[1]")).hasText("Birth Date: " + birthDate);
    }

    /** Owner Details has rendered — the step's text checks must not hit a previous view. */
    private void assertOnOwnerDetails() {
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Owner Information").setExact(true))).isVisible();
    }

    private void assertOwnerDetailsShown() {
        assertExactText(OWNER_NAME);
        assertExactText(ADDRESS);
        assertExactText(CITY);
        assertExactText(TELEPHONE);
    }

    private void assertPetListed(String name) {
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(name).setExact(true))).hasCount(1);
    }

    private void assertPetNotListed(String name) {
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(name).setExact(true))).hasCount(0);
    }

    private void assertExactText(String text) {
        assertThat(page.getByText(text, new Page.GetByTextOptions().setExact(true))).isVisible();
    }

    /** Validation: the Find Owners list is exactly what it was before the journey. */
    private void verifyNothingChanged() {
        assertEquals(ownerRowsBefore, snapshotOwnerRows(),
                "No owner or pet may be added, and every owner row must stay as it was");
    }

    // --- Shared assertions ---------------------------------------------------

    private void returnToFindOwners() {
        SideNavigationElement mainMenu = SideNavigationElement.getByLabel(page, NAV_LABEL);
        mainMenu.assertVisible();
        mainMenu.clickItem("Find Owners");
        TextFieldElement.getByLabel(page, "Last name").assertVisible();
    }

    /**
     * Every owner as the Find Owners grid shows them: an empty last-name search
     * returns all of them (UC-004 BR-003), one string per row holding the name,
     * address, city, telephone and pet names.
     */
    private List<String> snapshotOwnerRows() {
        returnToFindOwners();
        // Find Owners keeps a previous search term when the journey is already on it.
        TextFieldElement.getByLabel(page, "Last name").setValue("");
        ButtonElement.getByText(page, "Find Owner").click();

        GridElement results = GridElement.get(page);
        results.assertVisible();
        results.waitForGridToStopLoading();

        List<String> rows = new ArrayList<>();
        for (int row = 0; row < results.getTotalRowCount(); row++) {
            StringBuilder cells = new StringBuilder();
            for (String column : List.of("Name", "Address", "City", "Telephone", "Pets")) {
                int index = row;
                String text = results.findCell(row, column)
                        .orElseThrow(() -> new AssertionError(
                                "Find Owners grid has no '" + column + "' cell in row " + index))
                        .getCellContentLocator().textContent();
                cells.append(text == null ? "" : text.trim()).append(" | ");
            }
            rows.add(cells.toString());
        }
        return rows;
    }
}
