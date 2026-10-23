package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ReadOnlyHasValue;
import com.vaadin.flow.data.binder.ValidationException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Shared pet form used by Add Pet (UC-007) and Update Pet (UC-008).
 *
 * <p>Validation (BR-001 uniqueness is enforced in the enclosing view
 * because it needs query access):
 * <ul>
 *   <li>name required and at most as long as PET.name in {@code docs/entity_model.md}
 *       (UC-007/008 BR-004)</li>
 *   <li>birthDate required and not in the future (UC-007/008 BR-002)</li>
 *   <li>type required (UC-007 BR-003); on update the current type stays selected
 *       and cannot be removed (UC-008 BR-003)</li>
 *   <li>a rejected submit leaves every entered value in its field (UC-003 BR-005)</li>
 * </ul>
 *
 * <p>Validation and field population are delegated to a {@link Binder}
 * configured for the immutable {@link Pet} record
 * ({@code readRecord} / {@code writeRecord}).
 */
public class PetForm extends FormLayout {

    private final TextField name;
    private final DatePicker birthDate;
    private final ComboBox<PetType> type;
    private final Binder<Pet> binder = new Binder<>(Pet.class);

    private static final String REQUIRED = "required";
    private static final String INVALID_DATE = "invalid date";

    /** Column length of PET.name in {@code docs/entity_model.md}. */
    static final int NAME_LENGTH = 30;

    public PetForm(List<PetType> availableTypes) {
        name = new TextField("Name");
        // UC-007 BR-004: at most 30 characters, rejected on the field like any other validation error.
        // A constraint the component validates on the server too; it only needs a message.
        name.setMaxLength(NAME_LENGTH);
        name.setI18n(new TextField.TextFieldI18n()
                .setMaxLengthErrorMessage("At most " + NAME_LENGTH + " characters"));

        birthDate = new DatePicker("Birth Date");
        birthDate.setMax(LocalDate.now(ZoneId.systemDefault()));
        birthDate.setI18n(new DatePicker.DatePickerI18n().setMaxErrorMessage(INVALID_DATE));

        type = new ComboBox<>("Type");
        type.setItems(availableTypes);
        type.setItemLabelGenerator(PetType::name);
        type.setRequiredIndicatorVisible(true);

        binder.forField(name)
                .asRequired(REQUIRED)
                .withValidator(s -> !s.trim().isEmpty(), REQUIRED)
                .bind("name");
        // UC-007 BR-002: a pet's birth date is always recorded and lies on or before today.
        // UC-008 BR-002: the same rule applies on update.
        binder.forField(birthDate)
                .asRequired(REQUIRED)
                .withValidator(d -> !d.isAfter(LocalDate.now(ZoneId.systemDefault())), INVALID_DATE)
                .bind("birthDate");
        // UC-007 BR-003: a type must be chosen when a pet is first recorded.
        // UC-008 BR-003: on update the current type stays selected and cannot be removed.
        binder.forField(type)
                .asRequired(REQUIRED)
                .bind("type");
        // id and ownerId are record components but not form fields. Binder
        // requires a binding for every record property so writeRecord can
        // call the canonical constructor.
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("id");
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("ownerId");

        binder.readRecord(Pet.empty(null));

        add(name, birthDate, type);
    }

    public void read(Pet pet) {
        binder.readRecord(pet);
    }

    /**
     * Validate the form. Returns the populated Pet on success or an empty
     * optional on failure, with field-level error messages attached by the
     * Binder.
     */
    public Optional<Pet> validateAndRead(Integer existingId, Integer ownerId) {
        try {
            Pet pet = binder.writeRecord();
            return Optional.of(new Pet(existingId, pet.name().trim(), pet.birthDate(), pet.type(), ownerId));
        } catch (ValidationException _) {
            // UC-003 BR-005: the fields keep what the user entered; only the errors are added.
            return Optional.empty();
        }
    }

    public void rejectName(String message) {
        name.setInvalid(true);
        name.setErrorMessage(message);
    }
}
