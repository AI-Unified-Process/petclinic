package ai.unifiedprocess.petclinic.e2e;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.TestCase;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.SideNavigationElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executes test case TC-004 (docs/test_cases/TC-004-owner-contact-details-updated.md),
 * the "What changed? owner contact details" path of the Customer Record
 * Maintenance process (docs/processes/customer-record-maintenance.bpmn): a
 * clinic user finds an owner (UC-004), reviews his details (UC-005) and updates
 * address and telephone (UC-006).
 *
 * <p>The journey edits a seeded owner, so the Postconditions restore him to the
 * seed values rather than delete anything.
 */
@TestCase(id = "TC-004", useCases = {"UC-004", "UC-005", "UC-006"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TC004OwnerContactDetailsUpdatedIT extends AbstractBasePlaywrightIT {

    // The seeded owner as V2__seed_reference_data.sql defines him.
    private static final String FIRST_NAME = "George";
    private static final String LAST_NAME = "Franklin";
    private static final String CITY = "Madison";
    private static final String SEED_ADDRESS = "110 W. Liberty St.";
    private static final String SEED_TELEPHONE = "6085551023";
    private static final String PET_NAME = "Leo";

    // Test Data column of the Flow table.
    private static final String NEW_ADDRESS = "88 Lakeshore Drive";
    private static final String NEW_TELEPHONE = "6085550777";

    /** Accessible name of the drawer's navigation landmark. */
    private static final String NAV_LABEL = "Main menu";

    /** A visit renders as "yyyy-MM-dd — description"; nothing else on Owner Details starts with a date. */
    private static final Pattern VISIT_ROW = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} — ");

    @LocalServerPort
    private int port;

    /** Cleanup only; every assertion in the journey goes through the browser. */
    @Autowired
    private DSLContext dsl;

    /** The Find Owners list before the journey, for the "nothing else changed" diff. */
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
    @DisplayName("TC-004: An owner's address and telephone are updated")
    void ownerContactDetailsUpdated() {
        ownerRowsBefore = snapshotOwnerRows();

        // Step 1: Find owner (UC-004)
        findOwner();

        // Step 2: Review owner details (UC-005)
        reviewOwnerDetails();

        // Step 3: Update owner (UC-006)
        updateOwner();

        // Step 4: Verify owner updated
        verifyOwnerUpdated();

        verifyNewDetailsAreFound();
        verifyNothingElseChanged();
    }

    // --- Flow steps ----------------------------------------------------------

    /** Step 1 (and Validation 1): search Find Owners for the unique last name. */
    private void findOwner() {
        returnToFindOwners();
        TextFieldElement.getByLabel(page, "Last name").setValue(LAST_NAME);
        ButtonElement.getByText(page, "Find Owner").click();
        assertOnOwnerDetails();
    }

    /** Step 2: the single match lands on Owner Details with the details on record. */
    private void reviewOwnerDetails() {
        assertOwnerShown(SEED_ADDRESS, SEED_TELEPHONE);
    }

    /** Step 3: "Edit Owner", replace address and telephone, keep the rest. */
    private void updateOwner() {
        ButtonElement.getByText(page, "Edit Owner").click();

        TextFieldElement address = TextFieldElement.getByLabel(page, "Address");
        address.assertVisible();
        TextFieldElement.getByLabel(page, "First Name").assertValue(FIRST_NAME);
        address.setValue(NEW_ADDRESS);
        TextFieldElement.getByLabel(page, "Telephone").setValue(NEW_TELEPHONE);

        ButtonElement.getByText(page, "Update Owner").click();
    }

    /** Step 4: the notification and the new values on Owner Details. */
    private void verifyOwnerUpdated() {
        assertNotification("Owner Values Updated");
        assertOwnerShown(NEW_ADDRESS, NEW_TELEPHONE);
        assertThat(page.getByText(SEED_ADDRESS)).hasCount(0);
    }

    /** Validations 1 and 2: a new search shows the new details, and the pet is untouched. */
    private void verifyNewDetailsAreFound() {
        findOwner();
        assertOwnerShown(NEW_ADDRESS, NEW_TELEPHONE);
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(PET_NAME).setExact(true))).hasCount(1);
        assertExactText("Birth Date: 2000-09-07");
        assertExactText("Type: cat");
        assertExactText("(no visits yet)");
        assertThat(page.getByText(VISIT_ROW)).hasCount(0);
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Edit Pet"))).hasCount(1);
    }

    /**
     * Validation 3: the Find Owners list — every owner with their pets — is
     * what it was before the journey, except for the owner's new address and
     * telephone.
     */
    private void verifyNothingElseChanged() {
        List<String> expected = ownerRowsBefore.stream()
                .map(row -> row.replace(SEED_ADDRESS + " | " + CITY + " | " + SEED_TELEPHONE,
                        NEW_ADDRESS + " | " + CITY + " | " + NEW_TELEPHONE))
                .toList();
        assertEquals(expected, snapshotOwnerRows(),
                "Only the owner's address and telephone may change");
    }

    // --- Shared assertions ---------------------------------------------------

    /** Owner Details has rendered — the step's text checks must not hit a previous view. */
    private void assertOnOwnerDetails() {
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Owner Information").setExact(true))).isVisible();
    }

    private void assertOwnerShown(String address, String telephone) {
        assertExactText(FIRST_NAME + " " + LAST_NAME);
        assertExactText(address);
        assertExactText(CITY);
        assertExactText(telephone);
    }

    private void assertNotification(String message) {
        NotificationElement notification = NotificationElement.getByText(page, message);
        notification.assertOpen();
        notification.assertContent(message);
    }

    private void assertExactText(String text) {
        assertThat(page.getByText(text, new Page.GetByTextOptions().setExact(true))).isVisible();
    }

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

    // --- Postconditions ------------------------------------------------------

    /** Restores the seeded owner; nothing is deleted. */
    @AfterEach
    void restoreSeededOwner() {
        restoreOwner();
    }

    /** A killed earlier run would leave the new values behind and break step 2. */
    @BeforeEach
    void restoreLeftoversFromAnEarlierRun() {
        restoreOwner();
    }

    private void restoreOwner() {
        dsl.update(OWNERS)
                .set(OWNERS.ADDRESS, SEED_ADDRESS)
                .set(OWNERS.TELEPHONE, SEED_TELEPHONE)
                .where(OWNERS.FIRST_NAME.eq(FIRST_NAME))
                .and(OWNERS.LAST_NAME.eq(LAST_NAME))
                .execute();
    }
}
