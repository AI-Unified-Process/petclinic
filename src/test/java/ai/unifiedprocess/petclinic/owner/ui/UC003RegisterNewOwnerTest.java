package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-003: Register New Owner.
 *
 * <p>"Whitfield" is deliberately absent from {@code V2__seed_reference_data.sql}
 * so that after the save the post-navigation details page is unambiguous:
 * any rendered "Whitfield" must have come from this test's insert.
 */
class UC003RegisterNewOwnerTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-003")
    void addingValidOwnerPersistsAndNavigatesToDetails() {
        navigate(RegisterNewOwnerView.class);

        // Step 2: the form opens empty.
        for (String caption : new String[] {"First Name", "Last Name", "Address", "City", "Telephone"}) {
            assertEquals("", find(TextField.class).withCaption(caption).single().getValue(),
                    "Expected " + caption + " to be empty on a new owner form");
        }

        test(find(TextField.class).withCaption("First Name").single()).setValue("Jane");
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test(find(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Add Owner").single()).click();

        // Post-condition: routed to owners/<newId> (unknown id, but a number).
        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertTrue(path.matches("owners/\\d+"),
                "Expected owners/<id>, got: " + path);

        // The details view is now the active view — rendered paragraphs verify
        // the values actually round-tripped through the database, without
        // calling any repository directly.
        assertDoesNotThrow(
                () -> find(ViewOwnerDetailsView.class).single(),
                "Expected the details view of the new owner to be active");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Jane Whitfield").single(),
                "Expected owner name to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("123 Oak St").single(),
                "Expected address to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Madison").single(),
                "Expected city to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("5551234567").single(),
                "Expected telephone to be rendered");

        assertEquals("New Owner Created", test(find(Notification.class).single()).getText());
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-001", scenario = "A1: Validation Errors")
    void missingRequiredFieldsBlockCreation() {
        navigate(RegisterNewOwnerView.class);
        // leave everything blank and submit
        test(find(Button.class).withText("Add Owner").single()).click();

        assertTrue(find(TextField.class).withCaption("First Name").single().isInvalid());
        assertTrue(find(TextField.class).withCaption("Last Name").single().isInvalid());
        assertTrue(find(TextField.class).withCaption("Address").single().isInvalid());
        assertTrue(find(TextField.class).withCaption("City").single().isInvalid());
        assertTrue(find(TextField.class).withCaption("Telephone").single().isInvalid());

        // Still on the add-owner view: no navigation = nothing persisted.
        assertEquals("owners/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-002", scenario = "A1: Validation Errors")
    void telephoneMustBeTenDigits() {
        navigate(RegisterNewOwnerView.class);

        test(find(TextField.class).withCaption("First Name").single()).setValue("Jane");
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test(find(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        // TextField.allowedCharPattern restricts input to digits, but it does
        // not enforce the length, so short numbers still reach validation.
        test(find(TextField.class).withCaption("Telephone").single()).setValue("555");
        test(find(Button.class).withText("Add Owner").single()).click();

        assertTrue(find(TextField.class).withCaption("Telephone").single().isInvalid(),
                "Expected telephone field to be flagged invalid for non-10-digit value");
        assertEquals("owners/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Failure postcondition: no owner is persisted.
        assertNoWhitfieldRegistered();
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors")
    void correctedInputAfterValidationErrorRegistersTheOwner() {
        navigate(RegisterNewOwnerView.class);

        test(find(TextField.class).withCaption("First Name").single()).setValue("Jane");
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test(find(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        test(find(TextField.class).withCaption("Telephone").single()).setValue("555");
        test(find(Button.class).withText("Add Owner").single()).click();

        assertEquals("There was an error in creating the owner.",
                test(find(Notification.class).single()).getText());

        // The user corrects the input and the use case continues at step 3.
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Add Owner").single()).click();

        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertTrue(path.matches("owners/\\d+"), "Expected owners/<id>, got: " + path);
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Jane Whitfield").single(),
                "Expected the corrected owner to be rendered");
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-004", scenario = "A1: Validation Errors")
    void overLongValuesAreFieldErrorsNotConstraintViolations() {
        navigate(RegisterNewOwnerView.class);

        // One character past each column length of docs/entity_model.md. The
        // values are set on the components directly, past the tester: the
        // browser would stop at maxLength, and the server must not rely on it,
        // or the value ends as a constraint violation on the error page.
        find(TextField.class).withCaption("First Name").single().setValue("F".repeat(OwnerForm.NAME_LENGTH + 1));
        find(TextField.class).withCaption("Last Name").single().setValue("L".repeat(OwnerForm.NAME_LENGTH + 1));
        find(TextField.class).withCaption("Address").single().setValue("A".repeat(OwnerForm.ADDRESS_LENGTH + 1));
        find(TextField.class).withCaption("City").single().setValue("C".repeat(OwnerForm.CITY_LENGTH + 1));
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Add Owner").single()).click();

        for (String caption : new String[] {"First Name", "Last Name", "Address", "City"}) {
            TextField field = find(TextField.class).withCaption(caption).single();
            assertTrue(field.isInvalid(), "Expected " + caption + " to be rejected as too long");
            assertTrue(field.getErrorMessage().startsWith("At most "),
                    "Expected a length message on " + caption + ", got: " + field.getErrorMessage());
        }
        assertEquals("owners/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-003", businessRules = {"BR-001", "BR-005"}, scenario = "A1: Validation Errors")
    void rejectedSubmitKeepsEveryEnteredValue() {
        navigate(RegisterNewOwnerView.class);

        test(find(TextField.class).withCaption("First Name").single()).setValue("Jane");
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        // BR-001: a value of only spaces counts as blank.
        test(find(TextField.class).withCaption("Address").single()).setValue("   ");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Add Owner").single()).click();

        assertTrue(find(TextField.class).withCaption("Address").single().isInvalid(),
                "Expected an address of only spaces to be rejected as blank");
        // BR-005: the other entries stay, so only the rejected field needs correcting.
        assertEquals("Jane", find(TextField.class).withCaption("First Name").single().getValue());
        assertEquals("Whitfield", find(TextField.class).withCaption("Last Name").single().getValue());
        assertEquals("Madison", find(TextField.class).withCaption("City").single().getValue());
        assertEquals("5551234567", find(TextField.class).withCaption("Telephone").single().getValue());
        assertNoWhitfieldRegistered();
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-003")
    void ownerIdIsAssignedByTheDatabase() {
        navigate(RegisterNewOwnerView.class);

        // The user is given no way to supply an id: the form has no such field.
        assertTrue(find(TextField.class).withCaption("Id").all().isEmpty(),
                "Expected the add-owner form to expose no id field");

        // Two owners registered with identical data still end up on distinct
        // routes, so the id can only have come from the database sequence.
        int firstId = registerWhitfield("Jane");
        int secondId = registerWhitfield("John");

        assertNotEquals(firstId, secondId,
                "Expected the database to assign a distinct id to each new owner");
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A2: Registration Cancelled")
    void cancellingDiscardsTheInputAndReturnsToFindOwners() {
        navigate(RegisterNewOwnerView.class);

        test(find(TextField.class).withCaption("First Name").single()).setValue("Jane");
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test(find(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Cancel").single()).click();

        assertEquals("owners/find",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Failure postcondition: no owner is persisted.
        assertNoWhitfieldRegistered();
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-006")
    void surroundingSpacesAreRemovedBeforeTheOwnerIsRecorded() {
        navigate(RegisterNewOwnerView.class);

        test(find(TextField.class).withCaption("First Name").single()).setValue("  Jane ");
        test(find(TextField.class).withCaption("Last Name").single()).setValue(" Whitfield  ");
        test(find(TextField.class).withCaption("Address").single()).setValue(" 123 Oak St ");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison  ");
        // The telephone field takes digits only, so it cannot carry spaces at all.
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Add Owner").single()).click();

        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Jane Whitfield").single(),
                "Expected the name to be recorded without its surrounding spaces");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("123 Oak St").single(),
                "Expected the address to be recorded without its surrounding spaces");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Madison").single(),
                "Expected the city to be recorded without its surrounding spaces");

        // The recorded last name starts with "Whit", so the prefix search finds it (UC-004 BR-001).
        navigate(FindOwnersByLastNameView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("Whit");
        test(find(Button.class).withText("Find Owner").single()).click();
        assertDoesNotThrow(
                () -> find(ViewOwnerDetailsView.class).single(),
                "Expected the search for \"Whit\" to find the new owner");
    }

    @Test
    @UseCase(id = "UC-003", businessRules = {"BR-002", "BR-004", "BR-006"})
    void spacesAreRemovedBeforeLengthAndTelephoneAreValidated() {
        navigate(RegisterNewOwnerView.class);

        // Valid only once trimmed: a first name at its column length plus a
        // trailing space, and a telephone padded with spaces. Set on the
        // components directly, past the tester: the browser would stop them,
        // and the server must validate the trimmed value.
        String firstName = "F".repeat(OwnerForm.NAME_LENGTH);
        find(TextField.class).withCaption("First Name").single().setValue(firstName + " ");
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test(find(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        find(TextField.class).withCaption("Telephone").single().setValue(" 5551234567 ");
        test(find(Button.class).withText("Add Owner").single()).click();

        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertTrue(path.matches("owners/\\d+"),
                "Expected the padded values to be accepted, got: " + path);
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText(firstName + " Whitfield").single(),
                "Expected the first name to be recorded without its trailing space");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("5551234567").single(),
                "Expected the telephone to be recorded without its surrounding spaces");
        assertEquals("New Owner Created", test(find(Notification.class).single()).getText());
    }

    /** Searches Find Owners for "Whitfield", which the seed data does not contain, and expects no match. */
    private void assertNoWhitfieldRegistered() {
        navigate(FindOwnersByLastNameView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("Whitfield");
        test(find(Button.class).withText("Find Owner").single()).click();

        TextField lastNameField = find(TextField.class).withCaption("Last name").single();
        assertEquals("not found", lastNameField.getErrorMessage(),
                "Expected no owner named Whitfield to have been persisted");
    }

    /** Registers an owner and returns the id the server routed to. */
    private int registerWhitfield(String firstName) {
        navigate(RegisterNewOwnerView.class);
        test(find(TextField.class).withCaption("First Name").single()).setValue(firstName);
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test(find(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Add Owner").single()).click();

        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertTrue(path.matches("owners/\\d+"), "Expected owners/<id>, got: " + path);
        return Integer.parseInt(path.substring("owners/".length()));
    }
}
