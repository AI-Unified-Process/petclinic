package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.core.ui.NotFoundErrorView;
import ai.unifiedprocess.petclinic.owner.ui.ViewOwnerDetailsView;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
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

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DSLContext dsl;

    @Test
    @UseCase(id = "UC-008")
    void mainSuccessScenarioFromOwnerDetailsStoresAllThreeFieldsAndConfirms() {
        // Step 1: "Edit Pet" next to Max on the Owner Details view.
        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));
        Component maxCard = find(H3.class).withText("Max").single().getParent().orElseThrow();
        test(find(Button.class).withText("Edit Pet").from(maxCard).single()).click();
        assertEquals(editPath(PET_MAX_ID), activePath());

        // Step 2: pre-filled with the current name, birth date, and type.
        assertEquals("Max", nameField().getValue());
        assertEquals(LocalDate.of(1995, 9, 4), birthDateField().getValue());
        assertEquals("cat", petTypeField().getValue().name());

        // Step 3: amend all three fields and submit.
        test(nameField()).setValue("Maximilian");
        test(birthDateField()).setValue(LocalDate.of(1996, 2, 3));
        test(petTypeField()).selectItem("dog");
        test(find(Button.class).withText("Update Pet").single()).click();

        // Step 6: back on Owner Details with the confirmation.
        assertEquals("owners/" + OWNER_COLEMAN_ID, activePath());
        assertEquals("Pet details has been edited", test(find(Notification.class).single()).getText());
        // Success postconditions: the details view lists the updated pet, and the record holds all three values.
        assertEquals(List.of("Maximilian", "Samantha"), petNamesOnDetails());
        Pet stored = petRepository.findById(PET_MAX_ID).orElseThrow();
        assertEquals("Maximilian", stored.name());
        assertEquals(LocalDate.of(1996, 2, 3), stored.birthDate());
        assertEquals("dog", stored.type().name());
    }

    @Test
    @UseCase(id = "UC-008")
    void valuesSubmittedLastReplaceAConcurrentChangeWithoutConflict() {
        // Step 2: this Clinic User opens Max's form.
        openEditForm(PET_MAX_ID);
        // Another Clinic User changes Max after step 2.
        Pet max = petRepository.findById(PET_MAX_ID).orElseThrow();
        PetType dog = petRepository.findAllTypes().stream()
                .filter(type -> type.name().equals("dog")).findFirst().orElseThrow();
        assertTrue(petRepository.update(new Pet(max.id(), "Maximus", LocalDate.of(2001, 1, 1), dog, max.ownerId())));

        test(nameField()).setValue("Max Jr");
        test(find(Button.class).withText("Update Pet").single()).click();

        // Step 5: no conflict is reported, the save confirms as usual.
        assertEquals("owners/" + OWNER_COLEMAN_ID, activePath());
        assertEquals("Pet details has been edited", test(find(Notification.class).single()).getText());
        // The values submitted last — the whole form as this user submitted it — replace the other user's.
        Pet stored = petRepository.findById(PET_MAX_ID).orElseThrow();
        assertEquals("Max Jr", stored.name());
        assertEquals(LocalDate.of(1995, 9, 4), stored.birthDate());
        assertEquals("cat", stored.type().name());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-001")
    void thePetIsNotComparedAgainstItself() {
        openEditForm(PET_MAX_ID);

        // BR-001: "MAX" equals Max's own name case-insensitively, but the pet being edited is not compared against itself.
        test(nameField()).setValue("MAX");
        test(find(Button.class).withText("Update Pet").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID, activePath());
        assertEquals("MAX", petRepository.findById(PET_MAX_ID).orElseThrow().name());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-001")
    void aNameAnotherOwnersPetHasIsAccepted() {
        openEditForm(PET_SAMANTHA_ID);

        // UC-007 BR-001: different owners may have pets with the same name — "Leo" is George Franklin's.
        test(nameField()).setValue("Leo");
        test(find(Button.class).withText("Update Pet").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID, activePath());
        assertEquals(List.of("Leo", "Max"), petNamesOnDetails());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-002")
    void birthDateOfTodayIsAccepted() {
        openEditForm(PET_MAX_ID);

        // UC-007 BR-002: the birth date lies on or before today, so today itself is allowed.
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        test(birthDateField()).setValue(today);
        test(find(Button.class).withText("Update Pet").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID, activePath());
        assertEquals(today, petRepository.findById(PET_MAX_ID).orElseThrow().birthDate());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-004")
    void nameOfExactlyThirtyCharactersIsAccepted() {
        openEditForm(PET_MAX_ID);

        // UC-007 BR-004: at most 30 characters — 30 is the allowed boundary.
        String thirty = "M".repeat(30);
        test(nameField()).setValue(thirty);
        test(find(Button.class).withText("Update Pet").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID, activePath());
        assertEquals(thirty, petRepository.findById(PET_MAX_ID).orElseThrow().name());
    }

    @Test
    @UseCase(id = "UC-008")
    void editingPetPersistsChanges() {
        navigate(UpdatePetView.class, Map.of(
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
        ViewOwnerDetailsView details = find(ViewOwnerDetailsView.class).single();
        List<String> petNames = find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max Jr", "Samantha"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = {"BR-001", "BR-005"}, scenario = "A1: Duplicate Pet Name")
    void cannotRenamePetToMatchSiblingPet() {
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_SAMANTHA_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("Max");
        test(birthDateField()).setValue(LocalDate.of(1996, 1, 2));
        test(petTypeField()).selectItem("dog");
        test(find(Button.class).withText("Update Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("already exists", nameField.getErrorMessage());
        // A1 step 2, BR-005: every entered value stays in the form.
        assertEquals("Max", nameField.getValue());
        assertEquals(LocalDate.of(1996, 1, 2), birthDateField().getValue());
        assertEquals("dog", petTypeField().getValue().name());
        // Still on edit view.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_SAMANTHA_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Seed pet list unchanged when we re-render the details view.
        ViewOwnerDetailsView details = navigate(ViewOwnerDetailsView.class,
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
        navigate(UpdatePetView.class, Map.of(
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

        ViewOwnerDetailsView details = navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));
        List<String> petNames = find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max", "Samantha"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = {"BR-002", "BR-005"}, scenario = "A2: Birth Date in the Future")
    void futureBirthDateIsRejectedOnUpdate() {
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        LocalDate future = LocalDate.now(ZoneId.systemDefault()).plusDays(3);
        test(nameField()).setValue("Max Jr");
        test(petTypeField()).selectItem("dog");
        // Bypass the DatePickerTester (which enforces the component's max).
        find(DatePicker.class).withCaption("Birth Date").single().setValue(future);
        test(find(Button.class).withText("Update Pet").single()).click();

        DatePicker birthDate = find(DatePicker.class).withCaption("Birth Date").single();
        assertTrue(birthDate.isInvalid());
        assertEquals("invalid date", birthDate.getErrorMessage());
        // Still on edit view — update was rejected.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
        // A2 step 2, BR-005: every entered value stays in the form.
        assertEquals(future, birthDate.getValue());
        assertEquals("Max Jr", nameField().getValue());
        assertEquals("dog", petTypeField().getValue().name());
        // Failure postcondition: no pet record is changed.
        assertMaxUnchanged();
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-005", scenario = "A3: Missing Required Field")
    void blankNameIsRejectedOnUpdate() {
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("");
        test(birthDateField()).setValue(LocalDate.of(1996, 1, 2));
        test(petTypeField()).selectItem("dog");
        test(find(Button.class).withText("Update Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid(), "Expected a blank name to be rejected");
        assertEquals("required", nameField.getErrorMessage());

        // Still on the edit view — the form is re-rendered, nothing persisted.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
        // A3 step 2, BR-005: the other entries stay in the form.
        assertEquals(LocalDate.of(1996, 1, 2), birthDateField().getValue());
        assertEquals("dog", petTypeField().getValue().name());
        // Failure postcondition: no pet record is changed.
        assertMaxUnchanged();
    }

    @Test
    @UseCase(id = "UC-008", businessRules = {"BR-002", "BR-005"}, scenario = "A3: Missing Required Field")
    void missingBirthDateIsRejectedOnUpdate() {
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test(nameField()).setValue("Max Jr");
        find(DatePicker.class).withCaption("Birth Date").single().clear();
        test(find(Button.class).withText("Update Pet").single()).click();

        DatePicker birthDate = find(DatePicker.class).withCaption("Birth Date").single();
        assertTrue(birthDate.isInvalid(), "Expected a missing birth date to be rejected");
        assertEquals("required", birthDate.getErrorMessage());
        // A3 step 2, BR-005: the amended name stays in the form.
        assertEquals("Max Jr", nameField().getValue());
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Failure postcondition "The pet record is unchanged": the stored birth date survives.
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals(LocalDate.of(1995, 9, 4), find(DatePicker.class).withCaption("Birth Date").single().getValue());
    }

    @Test
    @UseCase(id = "UC-008")
    void changingTypePersistsTheNewType() {
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test(petTypeField()).selectItem("dog");
        test(find(Button.class).withText("Update Pet").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Step 5 / success postcondition: re-reading the pet shows the new type.
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals("dog", petTypeField().getValue().name());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-003")
    void typeMayBeLeftUnchangedOnUpdate() {
        navigate(UpdatePetView.class, Map.of(
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
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals("Max Jr", find(TextField.class).withCaption("Name").single().getValue());
        assertEquals("cat", petTypeField().getValue().name());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-003")
    void typeCannotBeRemovedOnUpdate() {
        navigate(UpdatePetView.class, Map.of(
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

        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals("Max", find(TextField.class).withCaption("Name").single().getValue());
        assertEquals("cat", petTypeField().getValue().name(),
                "Expected the stored type to be unchanged");
    }

    @Test
    @UseCase(id = "UC-008", scenario = "A4: Pet Edit Cancelled")
    void cancellingDiscardsAmendedValuesAndReturnsToDetails() {
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("Max Jr");
        test(find(Button.class).withText("Cancel").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Failure postcondition "The pet record is unchanged".
        ViewOwnerDetailsView details = find(ViewOwnerDetailsView.class).single();
        List<String> petNames = find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max", "Samantha"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = {"BR-004", "BR-005"}, scenario = "A7: Pet Name Too Long")
    void overLongNameIsAFieldErrorAndKeepsTheOtherEntries() {
        navigate(UpdatePetView.class, Map.of(
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

    @Test
    @UseCase(id = "UC-008", businessRules = {"BR-001", "BR-005"}, scenario = "A8: Pet Name Taken Meanwhile")
    void petNameTakenMeanwhileIsRejectedOnTheNameFieldAndKeepsTheEntries() throws Exception {
        // A8 as a real race: another Clinic User, on a thread and in a transaction
        // of their own, records "Buddy" for Jean Coleman and commits only once this
        // user's view has passed its check and is writing. The check cannot see the
        // uncommitted pet, so the view's rename is the write pets_owner_name_unique rejects.
        PetType dog = petRepository.findAllTypes().stream()
                .filter(type -> type.name().equals("dog")).findFirst().orElseThrow();
        Thread thisClinicUser = Thread.currentThread();
        CountDownLatch recordedUncommitted = new CountDownLatch(1);
        ExecutorService otherClinicUser = Executors.newSingleThreadExecutor();
        try {
            Future<?> competingWrite = otherClinicUser.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        petRepository.insert(new Pet(null, "Buddy", LocalDate.of(2021, 3, 3), dog, OWNER_COLEMAN_ID));
                        recordedUncommitted.countDown();
                        awaitPetUpdateOf(thisClinicUser);
                    }));
            assertTrue(recordedUncommitted.await(10, TimeUnit.SECONDS),
                    "Expected the other Clinic User to record the pet");

            openEditForm(PET_SAMANTHA_ID);
            test(nameField()).setValue("Buddy");
            test(birthDateField()).setValue(LocalDate.of(1996, 1, 2));
            test(petTypeField()).selectItem("dog");
            test(find(Button.class).withText("Update Pet").single()).click();
            competingWrite.get(10, TimeUnit.SECONDS);

            // A8 step 1: the name field reports the taken name.
            TextField nameField = nameField();
            assertTrue(nameField.isInvalid(), "A8: expected the taken name to be rejected on its field");
            assertEquals("already exists", nameField.getErrorMessage());
            // A8 step 2, BR-005: every entry stays in the form.
            assertEquals("Buddy", nameField.getValue());
            assertEquals(LocalDate.of(1996, 1, 2), birthDateField().getValue());
            assertEquals("dog", petTypeField().getValue().name());
            assertEquals(editPath(PET_SAMANTHA_ID), activePath());
            // Failure postcondition: Samantha is unchanged; only the competing Buddy is new.
            Pet samantha = petRepository.findById(PET_SAMANTHA_ID).orElseThrow();
            assertEquals("Samantha", samantha.name());
            assertEquals(LocalDate.of(1995, 9, 4), samantha.birthDate());
            assertEquals("cat", samantha.type().name());
        } finally {
            // The competing pet was committed outside the test transaction: remove it the same way.
            otherClinicUser.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> dsl.deleteFrom(PETS)
                            .where(PETS.OWNER_ID.eq(OWNER_COLEMAN_ID))
                            .and(PETS.NAME.eq("Buddy"))
                            .execute()))
                    .get(10, TimeUnit.SECONDS);
            otherClinicUser.shutdown();
        }
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-006")
    void surroundingSpacesAreRemovedFromThePetNameOnUpdate() {
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_SAMANTHA_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("  Sammy ");
        test(find(Button.class).withText("Update Pet").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
        ViewOwnerDetailsView details = find(ViewOwnerDetailsView.class).single();
        List<String> petNames = find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max", "Sammy"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-003", requirements = "NFR-003")
    void petTypesAreOfferedInTheOrderOfUC007() {
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        // BR-003: the edit form offers the pet types in the order of UC-007 BR-007, alphabetically by name.
        List<String> offered = petTypeField().getListDataView().getItems().map(PetType::name).toList();
        assertEquals(List.of("bird", "cat", "dog", "hamster", "lizard", "snake"), offered,
                "BR-003: expected the pet types alphabetically by name");
    }

    @SuppressWarnings("unchecked")
    private ComboBox<PetType> petTypeField() {
        return (ComboBox<PetType>) find(ComboBox.class).withCaption("Type").single();
    }

    private TextField nameField() {
        return find(TextField.class).withCaption("Name").single();
    }

    private DatePicker birthDateField() {
        return find(DatePicker.class).withCaption("Birth Date").single();
    }

    private void openEditForm(int petId) {
        navigate(UpdatePetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(petId)));
    }

    private List<String> petNamesOnDetails() {
        ViewOwnerDetailsView details = find(ViewOwnerDetailsView.class).single();
        return find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
    }

    private void assertMaxUnchanged() {
        Pet max = petRepository.findById(PET_MAX_ID).orElseThrow();
        assertEquals("Max", max.name());
        assertEquals(LocalDate.of(1995, 9, 4), max.birthDate());
        assertEquals("cat", max.type().name());
    }

    private static String editPath(int petId) {
        return "owners/" + OWNER_COLEMAN_ID + "/pets/" + petId + "/edit";
    }

    private static String activePath() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }

    /** Waits until the given thread is inside {@link PetRepository#update}, i.e. past the view's check. */
    private static void awaitPetUpdateOf(Thread clinicUser) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (Arrays.stream(clinicUser.getStackTrace()).noneMatch(frame ->
                frame.getClassName().equals(PetRepository.class.getName())
                        && frame.getMethodName().equals("update"))) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("The view never reached its write");
            }
            Thread.onSpinWait();
        }
    }
}
