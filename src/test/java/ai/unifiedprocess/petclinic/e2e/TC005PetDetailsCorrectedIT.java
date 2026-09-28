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
import org.vaadin.addons.dramafinder.element.DatePickerElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.SideNavigationElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.jooq.impl.DSL.select;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executes test case TC-005 (docs/test_cases/TC-005-pet-details-corrected.md),
 * the "What changed? pet details" path of the Customer Record Maintenance
 * process (docs/processes/customer-record-maintenance.bpmn): a clinic user
 * picks an owner from several matches (UC-004), reviews his details (UC-005)
 * and corrects a pet's birth date (UC-008).
 *
 * <p>The journey edits a seeded pet, so the Postconditions restore it to the
 * seed value rather than delete anything.
 */
@TestCase(id = "TC-005", useCases = {"UC-004", "UC-005", "UC-008"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TC005PetDetailsCorrectedIT extends AbstractBasePlaywrightIT {

    // Test Data column of the Flow table and the seeded records it resolves to.
    private static final String LAST_NAME = "Davis";
    private static final String OWNER_NAME = "Harold Davis";
    private static final String OWNER_ADDRESS = "563 Friendly St.";
    private static final String OWNER_CITY = "Windsor";
    private static final String OWNER_TELEPHONE = "6085553198";
    private static final String OTHER_OWNER_NAME = "Betty Davis";
    private static final String PET_NAME = "Iggy";
    private static final LocalDate SEED_BIRTH_DATE = LocalDate.of(2000, 11, 30);
    private static final LocalDate NEW_BIRTH_DATE = LocalDate.of(2000, 11, 3);

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
    @DisplayName("TC-005: A pet's birth date is corrected under the right owner")
    void petDetailsCorrected() {
        ownerRowsBefore = snapshotOwnerRows();

        // Step 1: Find owner (UC-004)
        findOwner(OWNER_NAME);

        // Step 2: Review owner details (UC-005)
        reviewOwnerDetails();

        // Step 3: Update pet (UC-008)
        updatePet();

        // Step 4: Verify pet updated
        verifyPetUpdated();

        verifyRightOwnerOnly();
        verifyNothingElseChanged();
    }

    // --- Flow steps ----------------------------------------------------------

    /** Step 1: search for the shared last name and pick the owner from the list. */
    private void findOwner(String ownerName) {
        returnToFindOwners();
        TextFieldElement.getByLabel(page, "Last name").setValue(LAST_NAME);
        ButtonElement.getByText(page, "Find Owner").click();

        // The view may still show the full list from the snapshot, so wait until
        // the grid holds the search result: the two owners named Davis.
        GridElement results = GridElement.get(page);
        results.assertVisible();
        page.waitForCondition(() -> results.getTotalRowCount() == 2);
        results.waitForGridToStopLoading();
        List<Integer> rows = results.findRowIndexesWithColumnText(0, ownerName);
        assertEquals(1, rows.size(), () -> "Expected one '" + ownerName + "' row in the Owners List");
        results.findCell(rows.getFirst(), "Name")
                .orElseThrow(() -> new AssertionError("No Name cell for " + ownerName))
                .click();
        assertOnOwnerDetails();
    }

    /** Step 2: the owner's details list the pet with the birth date on record. */
    private void reviewOwnerDetails() {
        assertOwnerShown();
        assertThat(petHeading()).hasCount(1);
        // The owner has exactly this one pet, so the page's single "Edit Pet" is Iggy's.
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Edit Pet"))).hasCount(1);
        assertExactText("Birth Date: " + SEED_BIRTH_DATE);
    }

    /** Step 3: "Edit Pet" next to the pet, replace the birth date, keep name and type. */
    private void updatePet() {
        ButtonElement.getByText(page, "Edit Pet").click();

        TextFieldElement name = TextFieldElement.getByLabel(page, "Name");
        name.assertVisible();
        name.assertValue(PET_NAME);
        DatePickerElement.getByLabel(page, "Birth Date").setValue(NEW_BIRTH_DATE);

        ButtonElement.getByText(page, "Update Pet").click();
    }

    /** Step 4 and Validation 1: the notification and the corrected birth date. */
    private void verifyPetUpdated() {
        assertNotification("Pet details has been edited");
        assertOwnerShown();
        assertThat(petHeading()).hasCount(1);
        assertExactText("Birth Date: " + NEW_BIRTH_DATE);
        assertExactText("Type: lizard");
        assertExactText("(no visits yet)");
        assertThat(page.getByText(VISIT_ROW)).hasCount(0);
    }

    /** Validation 2: the other owner of the same name still has only her own pet, unchanged. */
    private void verifyRightOwnerOnly() {
        findOwner(OTHER_OWNER_NAME);
        assertExactText(OTHER_OWNER_NAME);
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Basil").setExact(true))).hasCount(1);
        assertExactText("Birth Date: 2002-08-06");
        assertExactText("Type: hamster");
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Edit Pet"))).hasCount(1);
    }

    /**
     * Validation 3: the Find Owners list — every owner with their pets — is
     * exactly what it was before the journey; Harold Davis's own details are
     * asserted in step 4.
     */
    private void verifyNothingElseChanged() {
        assertEquals(ownerRowsBefore, snapshotOwnerRows(),
                "No owner or pet may be added, and every owner row must stay as it was");
    }

    // --- Shared assertions ---------------------------------------------------

    private com.microsoft.playwright.Locator petHeading() {
        return page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(PET_NAME).setExact(true));
    }

    /** Owner Details has rendered — the step's text checks must not hit a previous view. */
    private void assertOnOwnerDetails() {
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Owner Information").setExact(true))).isVisible();
    }

    private void assertOwnerShown() {
        assertExactText(OWNER_NAME);
        assertExactText(OWNER_ADDRESS);
        assertExactText(OWNER_CITY);
        assertExactText(OWNER_TELEPHONE);
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

    /** Restores the seeded pet; nothing is deleted. */
    @AfterEach
    void restoreSeededPet() {
        restorePet();
    }

    /** A killed earlier run would leave the corrected date behind and break step 2. */
    @BeforeEach
    void restoreLeftoversFromAnEarlierRun() {
        restorePet();
    }

    private void restorePet() {
        dsl.update(PETS)
                .set(PETS.BIRTH_DATE, SEED_BIRTH_DATE)
                .where(PETS.NAME.eq(PET_NAME))
                .and(PETS.OWNER_ID.in(select(OWNERS.ID)
                        .from(OWNERS)
                        .where(OWNERS.FIRST_NAME.eq("Harold"))
                        .and(OWNERS.LAST_NAME.eq(LAST_NAME))))
                .execute();
    }
}
