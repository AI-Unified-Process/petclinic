package ai.unifiedprocess.petclinic.owner.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.core.ui.ApplicationErrorView;
import ai.unifiedprocess.petclinic.core.ui.NotFoundErrorView;
import ai.unifiedprocess.petclinic.welcome.ui.WelcomeView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * UC-003: Register New Owner.
 *
 * <p>Expected values come from the specification: the notifications "New Owner
 * Created" and "There was an error in creating the owner.", the maximum lengths
 * of BR-004 (first and last name 30, address 255, city 80), and the 10-digit
 * telephone of BR-002. The Owner Details route {@code owners/:ownerId} and the
 * Find Owners route {@code owners/find} come from the architecture and the main
 * menu. Every owner a test records carries a last name no seed owner has, so the
 * persisted state is read back by that name; the test transaction of
 * {@link PetClinicTestBase} rolls it back.
 *
 * <p>The field error texts are not asserted: the specification does not state
 * them (OQ-001), so the tests assert only that the offending field is invalid.
 */
class UC003RegisterNewOwnerTest extends PetClinicTestBase {

    private static final String CREATED = "New Owner Created";
    private static final String ERROR = "There was an error in creating the owner.";

    private static final String FIRST_NAME = "First Name";
    private static final String LAST_NAME = "Last Name";
    private static final String ADDRESS = "Address";
    private static final String CITY = "City";
    private static final String TELEPHONE = "Telephone";
    private static final List<String> FIELDS = List.of(FIRST_NAME, LAST_NAME, ADDRESS, CITY, TELEPHONE);

    @Autowired
    private JdbcClient jdbcClient;

    // ---- Main success scenario ---------------------------------------------

    @Test
    @UseCase(id = "UC-003")
    void addOwnerFromFindOwnersViewOpensTheOwnerForm() {
        navigate(WelcomeView.class);
        test(find(SideNav.class).single()).clickItem("Find Owners");

        // Step 1: the Clinic User chooses "Add Owner" from the Find Owners view.
        Button addOwner = assertDoesNotThrow(() -> find(Button.class).withText("Add Owner").single(),
                "Expected an \"Add Owner\" action on the Find Owners view");
        test(addOwner).click();

        assertDoesNotThrow(() -> find(RegisterNewOwnerView.class).single(),
                "Expected \"Add Owner\" to open the owner form");
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-003")
    void ownerFormShowsTheFiveFieldsEmptyAndNoIdentifier() {
        navigate(RegisterNewOwnerView.class);

        // Step 2: an empty form with first name, last name, address, city, and telephone.
        for (String caption : FIELDS) {
            TextField field = assertDoesNotThrow(() -> field(caption), "Expected a field " + caption);
            assertThat(field.getValue()).as(caption).isEmpty();
        }
        // BR-003: the Clinic User can neither enter nor change the identifier.
        assertThat(find(TextField.class).all()).hasSize(FIELDS.size());
        assertThat(find(IntegerField.class).all()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-003")
    void validOwnerIsRecordedAndItsDetailsAreShown() {
        Integer highestIdBefore = highestOwnerId();
        navigate(RegisterNewOwnerView.class);

        // Step 3: fill in every field and submit.
        fill("Jane", "Zylstra", "1 Main St.", "Madison", "6085551234");
        submit();

        // Step 5 / success postcondition: the owner exists with the submitted values.
        List<Map<String, Object>> rows = ownersNamed("Zylstra");
        assertThat(rows).hasSize(1);
        Map<String, Object> owner = rows.getFirst();
        assertThat(owner).containsEntry("first_name", "Jane")
                .containsEntry("last_name", "Zylstra")
                .containsEntry("address", "1 Main St.")
                .containsEntry("city", "Madison")
                .containsEntry("telephone", "6085551234");
        // BR-003: a new identifier is assigned by the system.
        Integer id = (Integer) owner.get("id");
        assertThat(id).isGreaterThan(highestIdBefore);

        // Step 6: the Owner Details view of the new owner and "New Owner Created".
        assertOwnerDetailsShown(id);
        assertThat(notificationTexts()).containsExactly(CREATED);
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-006")
    void surroundingSpacesAreRemovedBeforeTheOwnerIsRecorded() {
        navigate(RegisterNewOwnerView.class);

        fill(" Jane ", " Zylstra ", "  1 Main St. ", " Madison  ", " 6085551234 ");
        submit();

        // BR-006: " Smith " is recorded as "Smith"; a telephone of 10 digits with spaces around is valid.
        List<Map<String, Object>> rows = ownersNamed("Zylstra");
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("first_name", "Jane")
                .containsEntry("last_name", "Zylstra")
                .containsEntry("address", "1 Main St.")
                .containsEntry("city", "Madison")
                .containsEntry("telephone", "6085551234");
        assertThat(notificationTexts()).containsExactly(CREATED);
    }

    @Test
    @UseCase(id = "UC-003", businessRules = {"BR-004", "BR-006"})
    void maximumLengthIsCheckedAfterTheSurroundingSpacesAreRemoved() {
        // Each value is longer than its maximum only because of the spaces around it:
        // first and last name 30, address 255, city 80 (BR-004), telephone 20 (entity model).
        String firstName = "F".repeat(30);
        String lastName = "Zylstra" + "z".repeat(23);
        String address = "A".repeat(255);
        String city = "C".repeat(80);
        String telephone = "6085551234";
        navigate(RegisterNewOwnerView.class);

        fill(" " + firstName + " ", "  " + lastName + "  ", " " + address + " ", " " + city + " ",
                " ".repeat(6) + telephone + " ".repeat(6));
        submit();

        // BR-006: the spaces are removed before the length of BR-004 is checked and the value recorded.
        assertThat(find(RegisterNewOwnerView.class).exists()).as("form accepted").isFalse();
        List<Map<String, Object>> rows = ownersNamed(lastName);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("first_name", firstName)
                .containsEntry("last_name", lastName)
                .containsEntry("address", address)
                .containsEntry("city", city)
                .containsEntry("telephone", telephone);
        assertThat(notificationTexts()).containsExactly(CREATED);
    }

    // ---- A1: Validation Errors / BR-001 Mandatory Fields ----------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = "BR-001")
    void everyBlankFieldIsRejectedOnItsField() {
        navigate(RegisterNewOwnerView.class);

        submit();

        // A1 step 1: an error on each offending field; A1 step 2: the error notification.
        for (String caption : FIELDS) {
            assertThat(field(caption).isInvalid()).as(caption + " invalid").isTrue();
        }
        assertThat(notificationTexts()).containsExactly(ERROR);
        assertThat(find(RegisterNewOwnerView.class).exists()).isTrue();
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-001", "BR-006"})
    void aFieldOfOnlySpacesCountsAsBlank() {
        for (String blankField : FIELDS) {
            navigate(RegisterNewOwnerView.class);
            fill("Jane", "Zylstra", "1 Main St.", "Madison", "6085551234");
            test(field(blankField)).setValue("   ");

            submit();

            // BR-001: a value of only spaces is blank; only that field is rejected.
            for (String caption : FIELDS) {
                assertThat(field(caption).isInvalid()).as(caption + " invalid when " + blankField + " is spaces")
                        .isEqualTo(caption.equals(blankField));
            }
            assertThat(notificationTexts()).as(blankField).containsExactly(ERROR);
            // Failure postcondition: no owner is recorded.
            assertThat(ownersNamed("Zylstra")).as(blankField).isEmpty();
            closeNotifications();
        }
    }

    // ---- A1 / BR-002 Telephone Format -----------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = "BR-002")
    void telephoneThatIsNotExactlyTenDigitsIsRejected() {
        for (String telephone : List.of("608555123", "60855512345", "608555123a", "phone12345")) {
            navigate(RegisterNewOwnerView.class);
            fill("Jane", "Zylstra", "1 Main St.", "Madison", telephone);

            submit();

            assertThat(field(TELEPHONE).isInvalid()).as("telephone " + telephone).isTrue();
            for (String caption : List.of(FIRST_NAME, LAST_NAME, ADDRESS, CITY)) {
                assertThat(field(caption).isInvalid()).as(caption + " with telephone " + telephone).isFalse();
            }
            assertThat(notificationTexts()).as(telephone).containsExactly(ERROR);
            assertThat(ownersNamed("Zylstra")).as(telephone).isEmpty();
            closeNotifications();
        }
    }

    // ---- A1 / BR-004 Maximum Field Lengths ------------------------------------

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-004")
    void valuesOfExactlyTheMaximumLengthAreAccepted() {
        String firstName = "F".repeat(30);
        String lastName = "Zylstra" + "z".repeat(23);
        String address = "A".repeat(255);
        String city = "C".repeat(80);
        navigate(RegisterNewOwnerView.class);

        fill(firstName, lastName, address, city, "6085551234");
        submit();

        List<Map<String, Object>> rows = ownersNamed(lastName);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("first_name", firstName)
                .containsEntry("address", address)
                .containsEntry("city", city);
        assertThat(notificationTexts()).containsExactly(CREATED);
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-004", "BR-005"})
    void valueLongerThanItsMaximumIsRejectedOnTheField() {
        Map<String, String> tooLong = Map.of(
                FIRST_NAME, "F".repeat(31),
                LAST_NAME, "Zylstra" + "z".repeat(24),
                ADDRESS, "A".repeat(256),
                CITY, "C".repeat(81));
        tooLong.forEach((caption, value) -> {
            navigate(RegisterNewOwnerView.class);
            fill("Jane", "Zylstra", "1 Main St.", "Madison", "6085551234");
            // The tester refuses to type past the field's limit as a browser does; a longer value
            // can still reach the server (pasted, scripted), which is the input BR-004 rejects.
            field(caption).setValue(value);

            submit();

            // BR-004: rejected on the offending field, never with the application error view.
            assertThat(find(ApplicationErrorView.class).exists()).as(caption).isFalse();
            assertThat(find(RegisterNewOwnerView.class).exists()).as(caption).isTrue();
            assertThat(field(caption).isInvalid()).as(caption + " invalid").isTrue();
            // BR-005: the entered value stays in the form.
            assertThat(field(caption).getValue()).as(caption + " kept").isEqualTo(value);
            assertThat(notificationTexts()).as(caption).containsExactly(ERROR);
            assertThat(ownersNamed("Zylstra")).as(caption).isEmpty();
            assertThat(ownersNamed(tooLong.get(LAST_NAME))).as(caption).isEmpty();
            closeNotifications();
        });
    }

    // ---- A1 / BR-005 Entries Preserved, then corrected -------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = "BR-005")
    void rejectedInputKeepsEveryEnteredValueAndOnlyTheOffendingFieldIsMarked() {
        navigate(RegisterNewOwnerView.class);
        fill("Jane", "Zylstra", "1 Main St.", "", "60855");

        submit();

        // A1 step 1 / BR-005: the errors are on the offending fields, every entered value stays.
        assertThat(field(FIRST_NAME).getValue()).isEqualTo("Jane");
        assertThat(field(LAST_NAME).getValue()).isEqualTo("Zylstra");
        assertThat(field(ADDRESS).getValue()).isEqualTo("1 Main St.");
        assertThat(field(CITY).getValue()).isEmpty();
        assertThat(field(TELEPHONE).getValue()).isEqualTo("60855");
        assertThat(field(FIRST_NAME).isInvalid()).isFalse();
        assertThat(field(LAST_NAME).isInvalid()).isFalse();
        assertThat(field(ADDRESS).isInvalid()).isFalse();
        assertThat(field(CITY).isInvalid()).isTrue();
        assertThat(field(TELEPHONE).isInvalid()).isTrue();
        // A1 step 2.
        assertThat(notificationTexts()).containsExactly(ERROR);
        // Failure postcondition: no owner is recorded.
        assertThat(ownersNamed("Zylstra")).isEmpty();
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors")
    void correctedInputIsRecordedAfterARejection() {
        navigate(RegisterNewOwnerView.class);
        fill("Jane", "Zylstra", "1 Main St.", "Madison", "60855");
        submit();
        assertThat(field(TELEPHONE).isInvalid()).isTrue();
        closeNotifications();

        // A1 steps 3–4: the Clinic User corrects the input and the use case continues at step 3.
        test(field(TELEPHONE)).setValue("6085551234");
        submit();

        List<Map<String, Object>> rows = ownersNamed("Zylstra");
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("first_name", "Jane")
                .containsEntry("telephone", "6085551234");
        assertOwnerDetailsShown((Integer) rows.getFirst().get("id"));
        assertThat(notificationTexts()).containsExactly(CREATED);
    }

    // ---- A2: Registration Cancelled ---------------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A2: Registration Cancelled")
    void cancelDiscardsTheEntriesAndReturnsToFindOwners() {
        navigate(RegisterNewOwnerView.class);
        fill("Jane", "Zylstra", "1 Main St.", "Madison", "6085551234");

        test(find(Button.class).withText("Cancel").single()).click();

        // A2 step 1: the entered values are discarded and the Find Owners view is shown.
        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEqualTo("owners/find");
        assertThat(ownersNamed("Zylstra")).isEmpty();
        navigate(RegisterNewOwnerView.class);
        for (String caption : FIELDS) {
            assertThat(field(caption).getValue()).as(caption).isEmpty();
        }
    }

    // ---- helpers ------------------------------------------------------------

    /**
     * Step 6 / success postcondition: the Owner Details view of the owner is shown. The
     * route {@code owners/:ownerId} alone also matches when the not-found error view
     * renders, so the error views must be absent as well.
     */
    private void assertOwnerDetailsShown(Integer ownerId) {
        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath())
                .isEqualTo("owners/" + ownerId);
        assertThat(find(NotFoundErrorView.class).exists())
                .as("Owner Details view shown, not the not-found error view").isFalse();
        assertThat(find(ApplicationErrorView.class).exists())
                .as("Owner Details view shown, not the application error view").isFalse();
        assertThat(find(RegisterNewOwnerView.class).exists()).as("owner form left").isFalse();
    }

    private TextField field(String caption) {
        return find(TextField.class).withCaption(caption).single();
    }

    private void fill(String firstName, String lastName, String address, String city, String telephone) {
        setIfNotEmpty(FIRST_NAME, firstName);
        setIfNotEmpty(LAST_NAME, lastName);
        setIfNotEmpty(ADDRESS, address);
        setIfNotEmpty(CITY, city);
        setIfNotEmpty(TELEPHONE, telephone);
    }

    private void setIfNotEmpty(String caption, String value) {
        if (!value.isEmpty()) {
            test(field(caption)).setValue(value);
        }
    }

    private void submit() {
        test(find(Button.class).withText("Add Owner").single()).click();
    }

    private List<String> notificationTexts() {
        return find(Notification.class).all().stream()
                .map(notification -> test(notification).getText())
                .toList();
    }

    private void closeNotifications() {
        find(Notification.class).all().forEach(Notification::close);
        roundTrip();
    }

    private List<Map<String, Object>> ownersNamed(String lastName) {
        return jdbcClient.sql("""
                        select id, first_name, last_name, address, city, telephone
                        from owners where last_name like ? order by id""")
                .param(lastName + "%")
                .query()
                .listOfRows();
    }

    private Integer highestOwnerId() {
        return jdbcClient.sql("select coalesce(max(id), 0) from owners").query(Integer.class).single();
    }
}
