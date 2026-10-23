package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.core.ui.NotFoundErrorView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-007: Add Pet to Owner.
 *
 * <p>Uses the seed owner "Harold Davis" ({@link #OWNER_DAVIS_HAROLD_ID}),
 * who already owns "Iggy", for the general add-pet cases. Uses "Betty Davis"
 * ({@link #OWNER_DAVIS_BETTY_ID}), who already owns "Basil", for the
 * duplicate-name branch.
 *
 * <p>Every unsuccessful end re-renders the owner's details page and checks
 * the pet list is the seed list, which is the failure postcondition
 * "No pet is persisted".
 */
class UC007AddPetToOwnerTest extends PetClinicTestBase {

    @Autowired
    private PetRepository petRepository;

    @Test
    @UseCase(id = "UC-007")
    void addingValidPetPersistsAndReturnsToOwnerDetails() {
        openAddPetForm(OWNER_DAVIS_HAROLD_ID);

        // Step 2: the form names the owner the pet is added to.
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Owner: Harold Davis").single(),
                "Expected the owner's name on the pet creation form");

        test(find(TextField.class).withCaption("Name").single()).setValue("Buddy");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2022, 6, 1));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test(find(Button.class).withText("Add Pet").single()).click();

        // Returned to Harold's details page
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID, activePath());

        // The freshly rendered OwnerDetailsView should list Buddy alongside
        // Harold's seed pet Iggy — alphabetical order (Buddy, Iggy) proves
        // the insert landed and the rendering sort is intact, without going
        // through the PetRepository.
        assertEquals(List.of("Buddy", "Iggy"), petNamesOnCurrentDetailsView());
    }

    @Test
    @UseCase(id = "UC-007")
    void petTypesCannotBeMaintainedFromTheForm() {
        // C-011: pet types are loaded directly into the database, so step 2
        // offers exactly the configured types and no way to add another.
        openAddPetForm(OWNER_DAVIS_HAROLD_ID);

        ComboBox<?> type = find(ComboBox.class).withCaption("Type").single();
        assertFalse(type.isAllowCustomValue(), "C-011: expected the Type field to reject a typed-in new type");
        List<String> offered = type.getListDataView().getItems()
                .map(item -> ((PetType) item).name())
                .sorted()
                .toList();
        assertEquals(List.of("bird", "cat", "dog", "hamster", "lizard", "snake"), offered,
                "C-011: expected exactly the seeded pet types");
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-001")
    void petNameUsedByAnotherOwnerIsAccepted() {
        // Betty Davis owns Basil; BR-001 only forbids duplicates within one owner.
        openAddPetForm(OWNER_DAVIS_HAROLD_ID);

        test(find(TextField.class).withCaption("Name").single()).setValue("Basil");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2023, 2, 2));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("cat");
        test(find(Button.class).withText("Add Pet").single()).click();

        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID, activePath());
        assertEquals(List.of("Basil", "Iggy"), petNamesOnCurrentDetailsView());
    }

    @Test
    @UseCase(id = "UC-007", businessRules = {"BR-001", "BR-005"}, scenario = "A1: Duplicate Pet Name for Owner")
    void duplicatePetNameForOwnerIsRejected() {
        // Betty Davis already owns Basil in the seed data.
        openAddPetForm(OWNER_DAVIS_BETTY_ID);

        test(find(TextField.class).withCaption("Name").single()).setValue("Basil");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2023, 2, 2));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("cat");
        test(find(Button.class).withText("Add Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("already exists", nameField.getErrorMessage());
        // BR-005: the duplicate check runs after the binder accepted the form,
        // so it needs its own proof that every entry stays in place.
        assertEquals("Basil", nameField.getValue());
        assertEquals(LocalDate.of(2023, 2, 2), find(DatePicker.class).withCaption("Birth Date").single().getValue());
        assertEquals("cat", ((PetType) find(ComboBox.class).withCaption("Type").single().getValue()).name());
        // Still on the add-pet view (no navigation happened).
        assertEquals("owners/" + OWNER_DAVIS_BETTY_ID + "/pets/new", activePath());
        assertPetsOf(OWNER_DAVIS_BETTY_ID, List.of("Basil"));
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-001", scenario = "A1: Duplicate Pet Name for Owner")
    void duplicatePetNameIsRejectedWhateverTheCase() {
        openAddPetForm(OWNER_DAVIS_BETTY_ID);

        test(find(TextField.class).withCaption("Name").single()).setValue("bASIL");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2023, 2, 2));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("cat");
        test(find(Button.class).withText("Add Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("already exists", nameField.getErrorMessage());
        assertEquals("owners/" + OWNER_DAVIS_BETTY_ID + "/pets/new", activePath());
        assertPetsOf(OWNER_DAVIS_BETTY_ID, List.of("Basil"));
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-001", scenario = "A1: Duplicate Pet Name for Owner")
    void duplicatePetNameIsRejectedByTheDatabaseWhateverTheCase() {
        // BR-001 compares case-insensitively, so the database backstop has to as
        // well: pets_owner_name_unique indexes lower(name). Goes straight to the
        // repository — the form check above never lets a duplicate reach the DB,
        // which is exactly why the constraint needs its own test.
        PetType cat = petRepository.findAllTypes().stream()
                .filter(type -> type.name().equals("cat"))
                .findFirst()
                .orElseThrow();

        Pet duplicate = new Pet(null, "BASIL", LocalDate.of(2023, 2, 2), cat, OWNER_DAVIS_BETTY_ID);

        assertThrows(DataIntegrityViolationException.class, () -> petRepository.insert(duplicate));
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-002", scenario = "A2: Birth Date in the Future")
    void futureBirthDateIsRejected() {
        openAddPetForm(OWNER_DAVIS_HAROLD_ID);

        test(find(TextField.class).withCaption("Name").single()).setValue("Futuro");
        // Bypass the DatePickerTester (which enforces the component's max)
        // so we can simulate submitting a future date directly.
        find(DatePicker.class).withCaption("Birth Date").single().setValue(LocalDate.now().plusDays(7));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test(find(Button.class).withText("Add Pet").single()).click();

        DatePicker birthDate = find(DatePicker.class).withCaption("Birth Date").single();
        assertTrue(birthDate.isInvalid());
        assertEquals("invalid date", birthDate.getErrorMessage());
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/pets/new", activePath());
        assertPetsOf(OWNER_DAVIS_HAROLD_ID, List.of("Iggy"));
    }

    @Test
    @UseCase(id = "UC-007", scenario = "A3: Missing Required Field")
    void blankNameBlocksCreation() {
        openAddPetForm(OWNER_DAVIS_HAROLD_ID);

        test(find(TextField.class).withCaption("Name").single()).setValue("   ");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2022, 6, 1));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test(find(Button.class).withText("Add Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("required", nameField.getErrorMessage());
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/pets/new", activePath());
        assertPetsOf(OWNER_DAVIS_HAROLD_ID, List.of("Iggy"));
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-002", scenario = "A3: Missing Required Field")
    void missingBirthDateBlocksCreation() {
        openAddPetForm(OWNER_DAVIS_HAROLD_ID);

        test(find(TextField.class).withCaption("Name").single()).setValue("Buddy");
        // intentionally skip birth date
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test(find(Button.class).withText("Add Pet").single()).click();

        DatePicker birthDate = find(DatePicker.class).withCaption("Birth Date").single();
        assertTrue(birthDate.isInvalid());
        assertEquals("required", birthDate.getErrorMessage());
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/pets/new", activePath());
        assertPetsOf(OWNER_DAVIS_HAROLD_ID, List.of("Iggy"));
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-003", scenario = "A3: Missing Required Field")
    void missingTypeBlocksCreation() {
        openAddPetForm(OWNER_DAVIS_HAROLD_ID);

        test(find(TextField.class).withCaption("Name").single()).setValue("Buddy");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2022, 6, 1));
        // intentionally skip type
        test(find(Button.class).withText("Add Pet").single()).click();

        ComboBox<?> type = find(ComboBox.class).withCaption("Type").single();
        assertTrue(type.isInvalid());
        assertEquals("required", type.getErrorMessage());
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/pets/new", activePath());
        assertPetsOf(OWNER_DAVIS_HAROLD_ID, List.of("Iggy"));
    }

    @Test
    @UseCase(id = "UC-007", scenario = "A4: Pet Creation Cancelled")
    void cancellingDiscardsEnteredValuesAndReturnsToDetails() {
        openAddPetForm(OWNER_DAVIS_HAROLD_ID);

        test(find(TextField.class).withCaption("Name").single()).setValue("Buddy");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2022, 6, 1));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test(find(Button.class).withText("Cancel").single()).click();

        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID, activePath());
        assertEquals(List.of("Iggy"), petNamesOnCurrentDetailsView());
    }

    @Test
    @UseCase(id = "UC-007", businessRules = {"BR-004", "BR-005"}, scenario = "A6: Pet Name Too Long")
    void overLongNameIsAFieldErrorAndKeepsTheOtherEntries() {
        openAddPetForm(OWNER_DAVIS_HAROLD_ID);

        // Past the tester, which would stop at maxLength like the browser does.
        find(TextField.class).withCaption("Name").single().setValue("N".repeat(PetForm.NAME_LENGTH + 1));
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2022, 6, 1));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test(find(Button.class).withText("Add Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid(), "Expected an over-long name to be rejected on its field");
        assertEquals("At most " + PetForm.NAME_LENGTH + " characters", nameField.getErrorMessage());
        // BR-005: the other entries stay in the form.
        assertEquals(LocalDate.of(2022, 6, 1), find(DatePicker.class).withCaption("Birth Date").single().getValue());
        assertEquals("dog", ((PetType) find(ComboBox.class).withCaption("Type").single().getValue()).name());
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/pets/new", activePath());
        assertPetsOf(OWNER_DAVIS_HAROLD_ID, List.of("Iggy"));
    }

    @Test
    @UseCase(id = "UC-007", scenario = "A5: Owner Not Found")
    void unknownOwnerRoutesToErrorView() {
        // By location: the typed navigate would assert the view type the router reroutes away from.
        UI.getCurrent().navigate("owners/99999/pets/new");

        assertDoesNotThrow(() -> find(NotFoundErrorView.class).single(),
                "Expected the not-found error view for an owner id that resolves to nothing");
        assertTrue(find(PetForm.class).all().isEmpty(), "Expected no pet form to be shown");
    }

    private void openAddPetForm(int ownerId) {
        navigate(AddPetView.class, Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(ownerId)));
    }

    /** Failure postcondition "No pet is persisted": re-render the owner's details and compare the pet list. */
    private void assertPetsOf(int ownerId, List<String> expectedPetNames) {
        navigate(OwnerDetailsView.class, Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(ownerId)));
        assertEquals(expectedPetNames, petNamesOnCurrentDetailsView());
    }

    /**
     * Pet names are rendered as H3; the static "Pets and Visits" section
     * header is also an H3 and is filtered out.
     */
    private List<String> petNamesOnCurrentDetailsView() {
        OwnerDetailsView details = find(OwnerDetailsView.class).single();
        return find(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
    }

    private static String activePath() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }
}
