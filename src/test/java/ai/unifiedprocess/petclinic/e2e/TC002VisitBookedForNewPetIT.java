package ai.unifiedprocess.petclinic.e2e;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.TestCase;
import com.microsoft.playwright.Locator;
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
import org.vaadin.addons.dramafinder.element.ComboBoxElement;
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
 * Executes test case TC-002 (docs/test_cases/TC-002-visit-booked-for-new-pet.md),
 * the "Owner registered? yes / Pet registered? no" path of the Visit Booking
 * process (docs/processes/visit-booking.bpmn): a clinic user finds an existing
 * owner (UC-004), reviews his details (UC-005), adds the animal the clinic has
 * not seen before (UC-007) and books its first visit (UC-009).
 *
 * <p>The owner then has two pets, so every pet-level lookup is scoped to the
 * pet's box on the Owner Details view.
 */
@TestCase(id = "TC-002", useCases = {"UC-004", "UC-005", "UC-007", "UC-009"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TC002VisitBookedForNewPetIT extends AbstractBasePlaywrightIT {

    // Test Data column of the Flow table and the seeded owner it resolves to.
    private static final String LAST_NAME = "Black";
    private static final String OWNER_NAME = "Jeff Black";
    private static final String ADDRESS = "1450 Oak Blvd.";
    private static final String SEED_PET_NAME = "Lucky";
    private static final String SEED_PET_TYPE = "bird";
    private static final String PET_NAME = "Pepper";
    private static final LocalDate PET_BIRTH_DATE = LocalDate.of(2024, 2, 10);
    private static final String PET_TYPE = "hamster";
    private static final String VISIT_DESCRIPTION = "first check-up";

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
    @DisplayName("TC-002: An existing owner's new pet is added and booked for its first visit")
    void visitBookedForNewPet() {
        ownerRowsBefore = snapshotOwnerRows();

        // Step 1: Find owner (UC-004)
        findOwner();

        // Step 2: Review owner details (UC-005)
        reviewOwnerDetails();

        // Step 3: Add pet (UC-007)
        addPet();

        // Step 4: Verify pet listed
        verifyPetListed();

        // Step 5: Book visit (UC-009)
        bookVisit();

        // Step 6: Verify visit recorded
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

    /** Step 2: "Pet registered? no" — only the seeded pet is on record. */
    private void reviewOwnerDetails() {
        assertExactText(OWNER_NAME);
        assertExactText(ADDRESS);
        assertThat(petHeading(SEED_PET_NAME)).hasCount(1);
        assertThat(petHeading(PET_NAME)).hasCount(0);
        // One "Edit Pet" per pet box, so exactly one means "Lucky" is the only pet.
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Edit Pet"))).hasCount(1);
    }

    /** Step 3: "Add New Pet" and submit name, birth date and type. */
    private void addPet() {
        ButtonElement.getByText(page, "Add New Pet").click();

        TextFieldElement name = TextFieldElement.getByLabel(page, "Name");
        name.assertVisible();
        name.setValue(PET_NAME);
        DatePickerElement.getByLabel(page, "Birth Date").setValue(PET_BIRTH_DATE);
        ComboBoxElement.getByLabel(page, "Type").selectItem(PET_TYPE);

        ButtonElement.getByText(page, "Add Pet").click();
    }

    /** Step 4 and Validation 1: the new pet is listed next to the seeded one. */
    private void verifyPetListed() {
        assertNotification("New Pet has been Added");
        assertExactText(OWNER_NAME);

        assertThat(petHeading(SEED_PET_NAME)).hasCount(1);
        assertThat(petHeading(PET_NAME)).hasCount(1);
        Locator newPet = petBox(PET_NAME);
        assertThat(newPet.getByText("Birth Date: " + PET_BIRTH_DATE.format(DISPLAY_DATE),
                new Locator.GetByTextOptions().setExact(true))).isVisible();
        assertThat(newPet.getByText("Type: " + PET_TYPE,
                new Locator.GetByTextOptions().setExact(true))).isVisible();
        assertThat(petBox(SEED_PET_NAME).getByText("Type: " + SEED_PET_TYPE,
                new Locator.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Edit Pet"))).hasCount(2);
    }

    /** Step 5: "Add Visit" next to the pet from step 3, keep the date, describe the visit. */
    private void bookVisit() {
        ButtonElement.getByText(petBox(PET_NAME), "Add Visit").click();

        TextFieldElement description = TextFieldElement.getByLabel(page, "Description");
        description.assertVisible();
        DatePickerElement.getByLabel(page, "Date").assertValue(LocalDate.now());
        description.setValue(VISIT_DESCRIPTION);

        ButtonElement.getByText(page, "Add Visit").click();
    }

    /** Step 6 and Validation 2: the visit is on the new pet, the seeded pet has none. */
    private void verifyVisitRecorded() {
        assertNotification("Your visit has been booked");
        assertExactText(OWNER_NAME);

        assertThat(petBox(PET_NAME).getByText(
                LocalDate.now().format(DISPLAY_DATE) + " — " + VISIT_DESCRIPTION,
                new Locator.GetByTextOptions().setExact(true))).isVisible();
        assertThat(petBox(PET_NAME).getByText(VISIT_ROW)).hasCount(1);
        assertThat(petBox(PET_NAME).getByText("(no visits yet)")).hasCount(0);
        assertThat(petBox(SEED_PET_NAME).getByText("(no visits yet)")).isVisible();
    }

    /**
     * Validation 3: the Find Owners list — every owner with their pets — is
     * what it was before the journey, except that the owner's row now also
     * names the added pet. No owner is added and no other owner gains a pet.
     */
    private void verifyNothingElseChanged() {
        List<String> expected = ownerRowsBefore.stream()
                .map(row -> row.startsWith(OWNER_NAME + " | ")
                        ? row.replace(" | " + SEED_PET_NAME + " | ", " | " + SEED_PET_NAME + " " + PET_NAME + " | ")
                        : row)
                .toList();
        assertEquals(expected, snapshotOwnerRows(),
                "Only the owner's row may change, and only by the added pet");
    }

    // --- Shared assertions ---------------------------------------------------

    /** Owner Details has rendered — the step's text checks must not hit a previous view. */
    private void assertOnOwnerDetails() {
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Owner Information").setExact(true))).isVisible();
    }

    private Locator petHeading(String petName) {
        return page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(petName).setExact(true));
    }

    /**
     * The box the Owner Details view renders per pet. Every layout around the
     * pet's heading matches the filter; the pet box is the innermost, hence the
     * last in document order.
     */
    private Locator petBox(String petName) {
        return page.locator("vaadin-vertical-layout")
                .filter(new Locator.FilterOptions().setHas(petHeading(petName)))
                .last();
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

    /** The visit before the pet; the seeded owner and "Lucky" stay. */
    @AfterEach
    void removeJourneyData() {
        deleteJourneyData();
    }

    /** A leftover "Pepper" would make step 3 fail with "already exists". */
    @BeforeEach
    void removeLeftoversFromAnEarlierRun() {
        deleteJourneyData();
    }

    private void deleteJourneyData() {
        dsl.deleteFrom(VISITS)
                .where(VISITS.PET_ID.in(select(PETS.ID)
                        .from(PETS)
                        .join(OWNERS).on(OWNERS.ID.eq(PETS.OWNER_ID))
                        .where(OWNERS.LAST_NAME.eq(LAST_NAME))
                        .and(PETS.NAME.eq(PET_NAME))))
                .execute();
        dsl.deleteFrom(PETS)
                .where(PETS.NAME.eq(PET_NAME))
                .and(PETS.OWNER_ID.in(select(OWNERS.ID).from(OWNERS).where(OWNERS.LAST_NAME.eq(LAST_NAME))))
                .execute();
    }
}
