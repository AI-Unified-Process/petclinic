package ai.unifiedprocess.petclinic.owner.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * UC-003: Register New Owner.
 *
 * <p>Expected values come from the specification. Every owner a test records
 * carries a last name no seed row of {@code V2__seed_reference_data.sql} has,
 * so the persisted state is read back by that name; the test transaction of
 * {@link PetClinicTestBase} rolls the row back.
 *
 * <p>The text of the error message on an offending field is not specified
 * (OQ-001, non-blocking), so field-level assertions check that the field is
 * marked invalid and carries an error message, not its wording.
 */
class UC003RegisterNewOwnerTest extends PetClinicTestBase {

    private static final String FIRST_NAME = "First Name";
    private static final String LAST_NAME = "Last Name";
    private static final String ADDRESS = "Address";
    private static final String CITY = "City";
    private static final String TELEPHONE = "Telephone";
    private static final List<String> FIELDS = List.of(FIRST_NAME, LAST_NAME, ADDRESS, CITY, TELEPHONE);

    private static final String UNIQUE_LAST_NAME = "Registerova";

    private static final String CREATED_NOTIFICATION = "New Owner Created";
    private static final String ERROR_NOTIFICATION = "There was an error in creating the owner.";

    @Autowired
    private JdbcClient jdbcClient;

    // ---- Main Success Scenario ---------------------------------------------

    @Test
    @UseCase(id = "UC-003")
    void addOwnerOnFindOwnersOpensAnEmptyOwnerForm() {
        navigate(FindOwnersView.class);

        // Step 1: the Clinic User chooses "Add Owner" from the Find Owners view.
        test(find(Button.class).withText("Add Owner").single()).click();

        // Step 2: an empty form with first name, last name, address, city, and telephone.
        assertDoesNotThrow(() -> find(RegisterNewOwnerView.class).single(),
                "Expected Add Owner to open the owner creation form");
        for (String caption : FIELDS) {
            assertThat(field(caption).getValue()).as(caption).isEmpty();
        }
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-003")
    void formOffersExactlyTheFiveContactFieldsAndNoIdentifier() {
        navigate(RegisterNewOwnerView.class);

        // BR-003: the Clinic User can neither enter nor change the identifier.
        assertThat(findInView(TextField.class).all())
                .extracting(TextField::getLabel)
                .containsExactlyInAnyOrderElementsOf(FIELDS);
    }

    @Test
    @UseCase(id = "UC-003", businessRules = {"BR-002", "BR-003"})
    void validSubmissionRecordsTheOwnerAndOpensTheirDetails() {
        navigate(RegisterNewOwnerView.class);

        // Step 3: all fields filled and submitted.
        fill("Jane", UNIQUE_LAST_NAME, "12 Elm St.", "Madison", "6085550123");
        submit();

        // Step 5 / BR-003: a new owner with a system-assigned identifier.
        Map<String, Object> owner = singleOwner(UNIQUE_LAST_NAME);
        assertThat(owner).containsEntry("first_name", "Jane")
                .containsEntry("last_name", UNIQUE_LAST_NAME)
                .containsEntry("address", "12 Elm St.")
                .containsEntry("city", "Madison")
                .containsEntry("telephone", "6085550123");
        assertThat(owner.get("id")).isNotNull();

        // Step 6: Owner Details of the new owner and the notification "New Owner Created".
        assertThat(activePath()).isEqualTo("owners/" + owner.get("id"));
        assertThat(notificationText()).isEqualTo(CREATED_NOTIFICATION);
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-003")
    void eachRegistrationGetsItsOwnIdentifier() {
        navigate(RegisterNewOwnerView.class);
        fill("Jane", UNIQUE_LAST_NAME, "12 Elm St.", "Madison", "6085550123");
        submit();

        navigate(RegisterNewOwnerView.class);
        fill("John", UNIQUE_LAST_NAME, "12 Elm St.", "Madison", "6085550123");
        submit();

        List<Integer> ids = jdbcClient.sql("select id from owners where last_name = ?")
                .param(UNIQUE_LAST_NAME).query(Integer.class).list();
        assertThat(ids).hasSize(2).doesNotHaveDuplicates();
    }

    // ---- BR-006 -------------------------------------------------------------

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-006")
    void surroundingSpacesAreRemovedBeforeRecording() {
        navigate(RegisterNewOwnerView.class);

        fill(" Jane ", " " + UNIQUE_LAST_NAME + " ", "  12 Elm St. ", " Madison  ", " 6085550123 ");
        submit();

        assertThat(singleOwner(UNIQUE_LAST_NAME))
                .containsEntry("first_name", "Jane")
                .containsEntry("last_name", UNIQUE_LAST_NAME)
                .containsEntry("address", "12 Elm St.")
                .containsEntry("city", "Madison")
                .containsEntry("telephone", "6085550123");
        assertThat(notificationText()).isEqualTo(CREATED_NOTIFICATION);
    }

    // ---- A1: Validation Errors / BR-001 -------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-001", "BR-005"})
    void blankFirstNameIsRejected() {
        assertBlankFieldRejected(FIRST_NAME, "");
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-001", "BR-005"})
    void blankLastNameIsRejected() {
        assertBlankFieldRejected(LAST_NAME, "");
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-001", "BR-005"})
    void blankAddressIsRejected() {
        assertBlankFieldRejected(ADDRESS, "");
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-001", "BR-005"})
    void blankCityIsRejected() {
        assertBlankFieldRejected(CITY, "");
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-001", "BR-005"})
    void blankTelephoneIsRejected() {
        assertBlankFieldRejected(TELEPHONE, "");
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-001", "BR-006"})
    void valuesOfOnlySpacesCountAsBlank() {
        for (String caption : FIELDS) {
            assertBlankFieldRejected(caption, "   ");
        }
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = "BR-005")
    void everyOffendingFieldIsMarkedAtOnce() {
        navigate(RegisterNewOwnerView.class);

        submit();

        // A1 step 1: an error message on each offending field.
        for (String caption : FIELDS) {
            assertFieldRejected(caption);
        }
        assertThat(notificationText()).isEqualTo(ERROR_NOTIFICATION);
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors")
    void correctedInputContinuesToRegistration() {
        navigate(RegisterNewOwnerView.class);
        fill("Jane", UNIQUE_LAST_NAME, "12 Elm St.", "Madison", "608555");
        submit();
        assertFieldRejected(TELEPHONE);

        // A1 steps 3–4: the Clinic User corrects the input and submits again (step 3).
        test(field(TELEPHONE)).setValue("6085550123");
        submit();

        Map<String, Object> owner = singleOwner(UNIQUE_LAST_NAME);
        assertThat(owner).containsEntry("telephone", "6085550123");
        assertThat(activePath()).isEqualTo("owners/" + owner.get("id"));
    }

    // ---- BR-002: Telephone Format -------------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-002", "BR-005"})
    void telephoneWithNineDigitsIsRejected() {
        assertTelephoneRejected("608555012");
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-002", "BR-005"})
    void telephoneWithElevenDigitsIsRejected() {
        assertTelephoneRejected("60855501234");
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-002", "BR-005"})
    void telephoneWithNonDigitsIsRejected() {
        assertTelephoneRejected("608555O123");
    }

    // ---- BR-004: Maximum Field Lengths --------------------------------------

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-004")
    void valuesAtTheirMaximumLengthAreRecorded() {
        navigate(RegisterNewOwnerView.class);
        String lastName = UNIQUE_LAST_NAME + "x".repeat(30 - UNIQUE_LAST_NAME.length());

        fill("f".repeat(30), lastName, "a".repeat(255), "c".repeat(80), "6085550123");
        submit();

        assertThat(singleOwner(lastName))
                .containsEntry("first_name", "f".repeat(30))
                .containsEntry("address", "a".repeat(255))
                .containsEntry("city", "c".repeat(80));
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-004", "BR-005"})
    void firstNameLongerThan30IsRejectedOnTheField() {
        assertTooLongRejected(FIRST_NAME, 31);
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-004", "BR-005"})
    void lastNameLongerThan30IsRejectedOnTheField() {
        assertTooLongRejected(LAST_NAME, 31);
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-004", "BR-005"})
    void addressLongerThan255IsRejectedOnTheField() {
        assertTooLongRejected(ADDRESS, 256);
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors", businessRules = {"BR-004", "BR-005"})
    void cityLongerThan80IsRejectedOnTheField() {
        assertTooLongRejected(CITY, 81);
    }

    // ---- A2: Registration Cancelled -----------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A2: Registration Cancelled")
    void cancelDiscardsTheEntriesAndReturnsToFindOwners() {
        navigate(RegisterNewOwnerView.class);
        fill("Jane", UNIQUE_LAST_NAME, "12 Elm St.", "Madison", "6085550123");

        test(find(Button.class).withText("Cancel").single()).click();

        // A2 step 1: back on Find Owners, nothing recorded.
        assertThat(activePath()).isEqualTo("owners/find");
        assertDoesNotThrow(() -> find(FindOwnersView.class).single(),
                "Expected Cancel to return to the Find Owners view");
        assertThat(ownerCount(UNIQUE_LAST_NAME)).isZero();

        // The entered values are discarded: Add Owner opens an empty form again.
        test(find(Button.class).withText("Add Owner").single()).click();
        for (String caption : FIELDS) {
            assertThat(field(caption).getValue()).as(caption).isEmpty();
        }
    }

    // ---- helpers -------------------------------------------------------------

    private static final Map<String, String> VALID = Map.of(
            FIRST_NAME, "Jane",
            LAST_NAME, UNIQUE_LAST_NAME,
            ADDRESS, "12 Elm St.",
            CITY, "Madison",
            TELEPHONE, "6085550123");

    /** A1 with BR-001: one blank field, the others valid and kept (BR-005), no owner recorded. */
    private void assertBlankFieldRejected(String caption, String blank) {
        navigate(RegisterNewOwnerView.class);
        fillValidExcept(caption, blank);

        submit();

        assertRejectedWithOthersKept(caption, blank);
    }

    private void assertTelephoneRejected(String telephone) {
        navigate(RegisterNewOwnerView.class);
        fillValidExcept(TELEPHONE, telephone);

        submit();

        assertRejectedWithOthersKept(TELEPHONE, telephone);
    }

    private void assertTooLongRejected(String caption, int length) {
        navigate(RegisterNewOwnerView.class);
        fillValidExcept(caption, "");
        String tooLong = "x".repeat(length);
        // The tester refuses to type past the field's maximum length, as a browser does;
        // the value is set on the component the way it would arrive from the client.
        field(caption).setValue(tooLong);

        submit();

        // BR-004: rejected on the offending field, never with the application error view.
        assertDoesNotThrow(() -> find(RegisterNewOwnerView.class).single(),
                "Expected the owner form to stay open, not an error view");
        assertRejectedWithOthersKept(caption, tooLong);
    }

    private void assertRejectedWithOthersKept(String caption, String entered) {
        // A1 step 1: error on the offending field, every entered value kept (BR-005).
        assertFieldRejected(caption);
        assertThat(field(caption).getValue()).isEqualTo(entered);
        for (String other : FIELDS) {
            if (!other.equals(caption)) {
                assertThat(field(other).isInvalid()).as(other + " invalid").isFalse();
                assertThat(field(other).getValue()).as(other).isEqualTo(VALID.get(other));
            }
        }
        // A1 step 2 and the failure postcondition.
        assertThat(notificationText()).isEqualTo(ERROR_NOTIFICATION);
        assertThat(activePath()).isEqualTo("owners/new");
        assertThat(ownerCount(UNIQUE_LAST_NAME)).isZero();
    }

    private void assertFieldRejected(String caption) {
        TextField field = field(caption);
        assertThat(field.isInvalid()).as(caption + " invalid").isTrue();
        assertThat(field.getErrorMessage()).as(caption + " error message").isNotBlank();
    }

    private void fillValidExcept(String caption, String value) {
        for (String each : FIELDS) {
            String entry = each.equals(caption) ? value : VALID.get(each);
            if (!entry.isEmpty()) {
                test(field(each)).setValue(entry);
            }
        }
    }

    private void fill(String firstName, String lastName, String address, String city, String telephone) {
        test(field(FIRST_NAME)).setValue(firstName);
        test(field(LAST_NAME)).setValue(lastName);
        test(field(ADDRESS)).setValue(address);
        test(field(CITY)).setValue(city);
        test(field(TELEPHONE)).setValue(telephone);
    }

    private void submit() {
        test(find(Button.class).withText("Add Owner").single()).click();
    }

    private TextField field(String caption) {
        return find(TextField.class).withCaption(caption).single();
    }

    private String notificationText() {
        Optional<Notification> last = find(Notification.class).all().stream().reduce((a, b) -> b);
        assertThat(last).as("notification").isPresent();
        return test(last.get()).getText();
    }

    private static String activePath() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }

    private Map<String, Object> singleOwner(String lastName) {
        List<Map<String, Object>> rows = jdbcClient
                .sql("select id, first_name, last_name, address, city, telephone from owners where last_name = ?")
                .param(lastName).query().listOfRows();
        assertThat(rows).as("owners named " + lastName).hasSize(1);
        return rows.getFirst();
    }

    private int ownerCount(String lastName) {
        return jdbcClient.sql("select count(*) from owners where last_name = ?")
                .param(lastName).query(Integer.class).single();
    }
}
