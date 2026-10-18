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
 * Executes test case TC-007 (docs/test_cases/TC-007-owner-already-on-record.md),
 * the "Owner already on record? yes" path of the Register New Owner process
 * (docs/processes/BP-002-register-new-owner.bpmn): a clinic user checks whether
 * a caller who wants to register is already a customer (UC-004), finds him and
 * registers nobody.
 *
 * <p>Read-only: the journey creates and changes nothing, so there is no cleanup.
 */
@TestCase(id = "TC-007", useCases = {"UC-004"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TC007OwnerAlreadyOnRecordIT extends AbstractBasePlaywrightIT {

    // The seeded owner as V2__seed_reference_data.sql defines him.
    private static final String LAST_NAME = "Schroeder";
    private static final String OWNER_NAME = "David Schroeder";
    private static final String ADDRESS = "2749 Blackhawk Trail";
    private static final String CITY = "Madison";
    private static final String TELEPHONE = "6085559435";

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
    @DisplayName("TC-007: A caller who wants to register is already on record")
    void ownerAlreadyOnRecord() {
        ownerRowsBefore = snapshotOwnerRows();

        // Step 1: Find owner (UC-004)
        findOwner();

        // Step 2: Verify owner on record
        verifyOwnerOnRecord();

        verifyOwnerFoundNotDuplicated();
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

    /** Step 2: the search landed on the caller's existing record. */
    private void verifyOwnerOnRecord() {
        assertOwnerDetailsShown();
    }

    /** Validation 1: a new search still leads straight to the one owner. */
    private void verifyOwnerFoundNotDuplicated() {
        findOwner();
        assertOwnerDetailsShown();
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
