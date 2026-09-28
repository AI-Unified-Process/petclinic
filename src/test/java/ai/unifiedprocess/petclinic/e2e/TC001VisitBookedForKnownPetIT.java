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
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VISITS;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.jooq.impl.DSL.select;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executes test case TC-001 (docs/test_cases/TC-001-visit-booked-for-known-pet.md),
 * the "Owner registered? yes / Pet registered? yes" path of the Visit Booking
 * process (docs/processes/visit-booking.bpmn): a clinic user finds an existing
 * owner (UC-004), reviews her details (UC-005) and books a visit for a pet
 * already on record (UC-009).
 *
 * <p>Both gateway decisions are forced by the seed data: "Escobito" is a unique
 * last-name prefix, and Maria Escobito owns "Mulligan" without any visits.
 */
@TestCase(id = "TC-001", useCases = {"UC-004", "UC-005", "UC-009"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TC001VisitBookedForKnownPetIT extends AbstractBasePlaywrightIT {

    // Test Data column of the Flow table and the seeded owner it resolves to.
    private static final String LAST_NAME = "Escobito";
    private static final String OWNER_NAME = "Maria Escobito";
    private static final String ADDRESS = "345 Maple St.";
    private static final String CITY = "Madison";
    private static final String TELEPHONE = "6085557683";
    private static final String PET_NAME = "Mulligan";
    private static final String PET_BIRTH_DATE = "1997-02-24";
    private static final String PET_TYPE = "dog";
    private static final String VISIT_DESCRIPTION = "limping on front left leg";

    /** The views render dates with {@link DateTimeFormatter#ISO_LOCAL_DATE}. */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

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
    @DisplayName("TC-001: A visit is booked for a pet already on record")
    void visitBookedForKnownPet() {
        ownerRowsBefore = snapshotOwnerRows();

        // Step 1: Find owner (UC-004)
        findOwner();

        // Step 2: Review owner details (UC-005)
        reviewOwnerDetails();

        // Step 3: Verify pet on record
        verifyPetOnRecord();

        // Step 4: Book visit (UC-009)
        bookVisit();

        // Step 5: Verify visit recorded
        verifyVisitRecorded();

        verifyNothingElseChanged();
    }

    // --- Flow steps ----------------------------------------------------------

    /** Step 1: search Find Owners for the unique last name. */
    private void findOwner() {
        returnToFindOwners();
        TextFieldElement.getByLabel(page, "Last name").setValue(LAST_NAME);
        ButtonElement.getByText(page, "Find Owner").click();
        assertOnOwnerDetails();
    }

    /** Step 2: the single match lands on Owner Details (UC-004 A2), pet without visits. */
    private void reviewOwnerDetails() {
        assertOwnerDetailsShown();
        assertExactText("(no visits yet)");
    }

    /** Step 3: "Pet registered? yes" — the pet is listed, and it is the only one. */
    private void verifyPetOnRecord() {
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(PET_NAME).setExact(true))).hasCount(1);
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Add Visit"))).hasCount(1);
    }

    /** Step 4: "Add Visit" next to the pet, keep the pre-filled date, describe the visit. */
    private void bookVisit() {
        ButtonElement.getByText(page, "Add Visit").click();

        TextFieldElement description = TextFieldElement.getByLabel(page, "Description");
        description.assertVisible();
        DatePickerElement.getByLabel(page, "Date").assertValue(LocalDate.now());
        description.setValue(VISIT_DESCRIPTION);

        ButtonElement.getByText(page, "Add Visit").click();
    }

    /** Step 5 and Validations 1-2: exactly one visit, dated today; owner and pet unchanged. */
    private void verifyVisitRecorded() {
        assertNotification("Your visit has been booked");
        assertOwnerDetailsShown();
        assertExactText(LocalDate.now().format(DISPLAY_DATE) + " — " + VISIT_DESCRIPTION);
        assertThat(page.getByText(VISIT_ROW)).hasCount(1);
        assertThat(page.getByText("(no visits yet)")).hasCount(0);

        // Validation 2: the owner keeps her details and her single, unchanged pet.
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(PET_NAME).setExact(true))).hasCount(1);
        assertExactText("Birth Date: " + PET_BIRTH_DATE);
        assertExactText("Type: " + PET_TYPE);
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Edit Pet"))).hasCount(1);
    }

    /**
     * Validation 3: the Find Owners list — every owner with their pets — is
     * exactly what it was before the journey.
     */
    private void verifyNothingElseChanged() {
        assertEquals(ownerRowsBefore, snapshotOwnerRows(),
                "No owner or pet may be added, and every owner row must stay as it was");
    }

    // --- Shared assertions ---------------------------------------------------

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

    /** Removes the one visit the journey books; the seeded owner and pet stay. */
    @AfterEach
    void removeJourneyData() {
        deleteJourneyData();
    }

    /** A killed earlier run would leave a second visit behind and break Validation 1. */
    @BeforeEach
    void removeLeftoversFromAnEarlierRun() {
        deleteJourneyData();
    }

    private void deleteJourneyData() {
        dsl.deleteFrom(VISITS)
                .where(VISITS.DESCRIPTION.eq(VISIT_DESCRIPTION))
                .and(VISITS.PET_ID.in(select(PETS.ID)
                        .from(PETS)
                        .join(OWNERS).on(OWNERS.ID.eq(PETS.OWNER_ID))
                        .where(OWNERS.LAST_NAME.eq(LAST_NAME))
                        .and(PETS.NAME.eq(PET_NAME))))
                .execute();
    }
}
