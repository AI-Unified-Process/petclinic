package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.core.ui.NotFoundErrorView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-008: Update Pet.
 *
 * <p>Uses Jean Coleman's seed pets: {@link #PET_MAX_ID} (cat "Max") and
 * {@link #PET_SAMANTHA_ID} (cat "Samantha"). The name-collision case
 * renames Samantha → Max.
 */
class UC008UpdatePetTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-008")
    void editingPetPersistsChanges() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        // Step 2: the form is pre-filled with the seeded name and birth date.
        assertEquals("Max", find(TextField.class).withCaption("Name").single().getValue());
        assertEquals(LocalDate.of(1995, 9, 4), find(DatePicker.class).withCaption("Birth Date").single().getValue());

        test(find(TextField.class).withCaption("Name").single()).setValue("Max Jr");
        test(find(Button.class).withText("Update Pet").single()).click();

        // On success, navigation returns to the owner details view.
        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Coleman's pets, re-read through the details view, should now show
        // "Max Jr" instead of "Max" (alphabetical order preserved).
        OwnerDetailsView details = find(OwnerDetailsView.class).single();
        List<String> petNames = find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max Jr", "Samantha"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-001", scenario = "A1: Duplicate Pet Name")
    void cannotRenamePetToMatchSiblingPet() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_SAMANTHA_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("Max");
        test(find(Button.class).withText("Update Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("already exists", nameField.getErrorMessage());
        // Still on edit view.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_SAMANTHA_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Seed pet list unchanged when we re-render the details view.
        OwnerDetailsView details = navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));
        List<String> petNames = find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max", "Samantha"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-001", scenario = "A1: Duplicate Pet Name")
    void cannotRenamePetToSiblingNameInDifferentCase() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_SAMANTHA_ID)));

        // UC-007 BR-001: names are compared case-insensitively, so "mAX" collides with "Max".
        test(find(TextField.class).withCaption("Name").single()).setValue("mAX");
        test(find(Button.class).withText("Update Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("already exists", nameField.getErrorMessage());
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_SAMANTHA_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        OwnerDetailsView details = navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));
        List<String> petNames = find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max", "Samantha"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-002", scenario = "A2: Birth Date in the Future")
    void futureBirthDateIsRejectedOnUpdate() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        // Bypass the DatePickerTester (which enforces the component's max).
        find(DatePicker.class).withCaption("Birth Date").single().setValue(LocalDate.now().plusDays(3));
        test(find(Button.class).withText("Update Pet").single()).click();

        DatePicker birthDate = find(DatePicker.class).withCaption("Birth Date").single();
        assertTrue(birthDate.isInvalid());
        assertEquals("invalid date", birthDate.getErrorMessage());
        // Still on edit view — update was rejected.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-008", scenario = "A3: Missing Required Field")
    void blankNameIsRejectedOnUpdate() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("");
        test(find(Button.class).withText("Update Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid(), "Expected a blank name to be rejected");
        assertEquals("required", nameField.getErrorMessage());

        // Still on the edit view — the form is re-rendered, nothing persisted.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-002", scenario = "A3: Missing Required Field")
    void missingBirthDateIsRejectedOnUpdate() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        find(DatePicker.class).withCaption("Birth Date").single().clear();
        test(find(Button.class).withText("Update Pet").single()).click();

        DatePicker birthDate = find(DatePicker.class).withCaption("Birth Date").single();
        assertTrue(birthDate.isInvalid(), "Expected a missing birth date to be rejected");
        assertEquals("required", birthDate.getErrorMessage());
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Failure postcondition "The pet record is unchanged": the stored birth date survives.
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals(LocalDate.of(1995, 9, 4), find(DatePicker.class).withCaption("Birth Date").single().getValue());
    }

    @Test
    @UseCase(id = "UC-008")
    void changingTypePersistsTheNewType() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test(petTypeField()).selectItem("dog");
        test(find(Button.class).withText("Update Pet").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Step 5 / success postcondition: re-reading the pet shows the new type.
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals("dog", petTypeField().getValue().name());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-003")
    void typeMayBeLeftUnchangedOnUpdate() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        // BR-003: the current type stays selected unless the user chooses another one.
        assertEquals("cat", petTypeField().getValue().name());

        test(find(TextField.class).withCaption("Name").single()).setValue("Max Jr");
        test(find(Button.class).withText("Update Pet").single()).click();

        // Leaving the edit view at all proves the save was accepted: a rejected
        // form re-renders in place (see blankNameIsRejectedOnUpdate).
        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Re-reading the pet through the edit view shows the untouched type
        // survived the round-trip to the database.
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals("Max Jr", find(TextField.class).withCaption("Name").single().getValue());
        assertEquals("cat", petTypeField().getValue().name());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-003")
    void typeCannotBeRemovedOnUpdate() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        // BR-003: a pet always keeps a type, so an emptied field is rejected.
        petTypeField().clear();
        test(find(TextField.class).withCaption("Name").single()).setValue("Max Jr");
        test(find(Button.class).withText("Update Pet").single()).click();

        assertTrue(petTypeField().isInvalid(), "Expected a removed type to be rejected");
        assertEquals("required", petTypeField().getErrorMessage());
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals("Max", find(TextField.class).withCaption("Name").single().getValue());
        assertEquals("cat", petTypeField().getValue().name(),
                "Expected the stored type to be unchanged");
    }

    @Test
    @UseCase(id = "UC-008", scenario = "A4: Pet Edit Cancelled")
    void cancellingDiscardsAmendedValuesAndReturnsToDetails() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("Max Jr");
        test(find(Button.class).withText("Cancel").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Failure postcondition "The pet record is unchanged".
        OwnerDetailsView details = find(OwnerDetailsView.class).single();
        List<String> petNames = find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max", "Samantha"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = {"BR-004", "BR-005"}, scenario = "A7: Pet Name Too Long")
    void overLongNameIsAFieldErrorAndKeepsTheOtherEntries() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(1996, 1, 2));
        // Past the tester, which would stop at maxLength like the browser does.
        find(TextField.class).withCaption("Name").single().setValue("N".repeat(PetForm.NAME_LENGTH + 1));
        test(find(Button.class).withText("Update Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid(), "Expected an over-long name to be rejected on its field");
        assertEquals("At most " + PetForm.NAME_LENGTH + " characters", nameField.getErrorMessage());
        // BR-005: the amended birth date stays in the form.
        assertEquals(LocalDate.of(1996, 1, 2), find(DatePicker.class).withCaption("Birth Date").single().getValue());
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-008", scenario = "A5: Owner Not Found")
    void unknownOwnerRoutesToErrorView() {
        // By location: the typed navigate would assert the view type the router reroutes away from.
        UI.getCurrent().navigate("owners/99999/pets/" + PET_MAX_ID + "/edit");

        assertDoesNotThrow(() -> find(NotFoundErrorView.class).single(),
                "Expected the not-found error view for an owner id that resolves to nothing");
        assertTrue(find(PetForm.class).all().isEmpty(), "Expected no pet form to be shown");
    }

    @Test
    @UseCase(id = "UC-008", scenario = "A6: Pet Not Found for Owner")
    void petOfAnotherOwnerRoutesToErrorView() {
        // Max belongs to Jean Coleman, not to George Franklin.
        UI.getCurrent().navigate("owners/" + OWNER_FRANKLIN_ID + "/pets/" + PET_MAX_ID + "/edit");

        assertDoesNotThrow(() -> find(NotFoundErrorView.class).single(),
                "Expected the not-found error view for a pet the owner does not own");
        assertTrue(find(PetForm.class).all().isEmpty(), "Expected no pet form to be shown");
    }

    @Test
    @UseCase(id = "UC-008", scenario = "A6: Pet Not Found for Owner")
    void unknownPetRoutesToErrorView() {
        UI.getCurrent().navigate("owners/" + OWNER_COLEMAN_ID + "/pets/99999/edit");

        assertDoesNotThrow(() -> find(NotFoundErrorView.class).single(),
                "Expected the not-found error view for a pet id that resolves to nothing");
    }

    @SuppressWarnings("unchecked")
    private ComboBox<PetType> petTypeField() {
        return (ComboBox<PetType>) find(ComboBox.class).withCaption("Type").single();
    }
}
