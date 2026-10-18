package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-006: Update Owner.
 *
 * <p>Uses the seed owner "Harold Davis" ({@link #OWNER_DAVIS_HAROLD_ID}).
 * Mutations roll back at the end of each test via {@link PetClinicTestBase}'s
 * {@code @Transactional}.
 */
class UC006UpdateOwnerTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-006")
    void editingOwnerPersistsChangesAndReturnsToDetails() {
        navigate(EditOwnerView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        // fields pre-filled from the seed data
        assertEquals("Harold", find(TextField.class).withCaption("First Name").single().getValue());
        assertEquals("Davis", find(TextField.class).withCaption("Last Name").single().getValue());
        assertEquals("563 Friendly St.", find(TextField.class).withCaption("Address").single().getValue());
        assertEquals("Windsor", find(TextField.class).withCaption("City").single().getValue());
        assertEquals("6085553198", find(TextField.class).withCaption("Telephone").single().getValue());

        test(find(TextField.class).withCaption("First Name").single()).setValue("Harry");
        test(find(TextField.class).withCaption("City").single()).setValue("Springfield");
        test(find(Button.class).withText("Update Owner").single()).click();

        // Returned to owner details
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID, activePath());

        // The details view now reflects the persisted update — rendered
        // paragraphs verify the round-trip without the repo.
        assertDoesNotThrow(
                () -> find(OwnerDetailsView.class).single(),
                "Expected the details view of the updated owner to be active");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Harry Davis").single(),
                "Expected updated name to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Springfield").single(),
                "Expected updated city to be rendered");
    }

    @Test
    @UseCase(id = "UC-006", businessRules = "BR-001", scenario = "A1: Validation Errors")
    void clearedContactFieldsBlockUpdate() {
        navigate(EditOwnerView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        for (String caption : new String[]{"First Name", "Last Name", "Address", "City", "Telephone"}) {
            test(find(TextField.class).withCaption(caption).single()).setValue("");
        }
        test(find(Button.class).withText("Update Owner").single()).click();

        for (String caption : new String[]{"First Name", "Last Name", "Address", "City", "Telephone"}) {
            assertTrue(find(TextField.class).withCaption(caption).single().isInvalid(),
                    "Expected " + caption + " to be flagged invalid when cleared");
        }
        // Still on edit view — no save happened.
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/edit", activePath());

        assertSeedOwnerUnchanged();
    }

    @Test
    @UseCase(id = "UC-006", businessRules = "BR-002", scenario = "A1: Validation Errors")
    void telephoneMustBeTenDigitsOnUpdate() {
        navigate(EditOwnerView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test(find(TextField.class).withCaption("First Name").single()).setValue("Harry");
        test(find(TextField.class).withCaption("Telephone").single()).setValue("555");
        test(find(Button.class).withText("Update Owner").single()).click();

        assertTrue(find(TextField.class).withCaption("Telephone").single().isInvalid(),
                "Expected telephone field to be flagged invalid for non-10-digit value");
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/edit", activePath());

        assertSeedOwnerUnchanged();
    }

    @Test
    @UseCase(id = "UC-006", scenario = "A1: Validation Errors")
    void correctedInputAfterValidationErrorUpdatesTheOwner() {
        navigate(EditOwnerView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test(find(TextField.class).withCaption("City").single()).setValue("");
        test(find(Button.class).withText("Update Owner").single()).click();

        assertEquals("There was an error in updating the owner.",
                test(find(Notification.class).single()).getText());

        // The user corrects the input and the use case continues at step 3.
        test(find(TextField.class).withCaption("City").single()).setValue("Springfield");
        test(find(Button.class).withText("Update Owner").single()).click();

        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID, activePath());
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Springfield").single(),
                "Expected the corrected city to be rendered");
    }

    @Test
    @UseCase(id = "UC-006", scenario = "A2: Edit Cancelled")
    void cancellingDiscardsChangesAndReturnsToDetails() {
        navigate(EditOwnerView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test(find(TextField.class).withCaption("First Name").single()).setValue("Harry");
        test(find(Button.class).withText("Cancel").single()).click();

        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID, activePath());
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Harold Davis").single(),
                "Expected the unchanged owner name to be rendered");
        assertTrue(find(Paragraph.class).withText("Harry Davis").all().isEmpty(),
                "Expected the discarded name not to be rendered");
    }

    private void assertSeedOwnerUnchanged() {
        // Re-render the details page and verify the seed values are unchanged.
        navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Harold Davis").single(),
                "Expected seed name to remain unchanged");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("6085553198").single(),
                "Expected seed telephone to remain unchanged");
    }

    private static String activePath() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }
}
