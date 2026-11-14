package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ReadOnlyHasValue;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;

import java.util.Optional;

/**
 * Shared owner form used by Add Owner (UC-003) and Update Owner (UC-006).
 *
 * <p>Validation rules (UC-003 BR-001, BR-002, BR-004, BR-005 and BR-006, which UC-006 cites):
 * <ul>
 *   <li>surrounding spaces are removed from every field before it is
 *       validated, and the trimmed value is recorded</li>
 *   <li>first name, last name, address, city, telephone are all required and
 *       must not be blank — a value of only spaces counts as blank</li>
 *   <li>telephone must match {@code \d{10}}</li>
 *   <li>every field is at most as long as its OWNER column in
 *       {@code docs/entity_model.md}, so an over-long value is a field error
 *       and never a constraint violation</li>
 *   <li>a rejected submit leaves every entered value in its field</li>
 * </ul>
 *
 * <p>Validation and field population are delegated to a {@link Binder}
 * configured for the immutable {@link Owner} record. Because records are
 * immutable, the form uses {@code readRecord} / {@code writeRecord} rather
 * than {@code setBean}.
 */
public class OwnerForm extends FormLayout {

    /** Column lengths of OWNER in {@code docs/entity_model.md}. */
    static final int NAME_LENGTH = 30;
    static final int ADDRESS_LENGTH = 255;
    static final int CITY_LENGTH = 80;
    static final int TELEPHONE_LENGTH = 10;

    private final TextField firstName;
    private final TextField lastName;
    private final TextField address;
    private final TextField city;
    private final TextField telephone;
    private final Binder<Owner> binder = new Binder<>(Owner.class);

    public OwnerForm() {
        firstName = limitedTextField("First Name", NAME_LENGTH);
        lastName = limitedTextField("Last Name", NAME_LENGTH);
        address = limitedTextField("Address", ADDRESS_LENGTH);
        city = limitedTextField("City", CITY_LENGTH);
        telephone = limitedTextField("Telephone", TELEPHONE_LENGTH);
        telephone.setAllowedCharPattern("[0-9]");
        telephone.setHelperText("10 digits");

        trimmedAndRequired(firstName, "First name is required", NAME_LENGTH).bind("firstName");
        trimmedAndRequired(lastName, "Last name is required", NAME_LENGTH).bind("lastName");
        trimmedAndRequired(address, "Address is required", ADDRESS_LENGTH).bind("address");
        trimmedAndRequired(city, "City is required", CITY_LENGTH).bind("city");
        trimmedAndRequired(telephone, "Telephone is required", TELEPHONE_LENGTH)
                // UC-003 BR-002: a telephone number is exactly 10 digits.
                .withValidator(s -> s.matches("\\d{10}"), "Telephone must be exactly 10 digits")
                .bind("telephone");
        // id is a record component but not a form field. Binder requires a
        // binding for every record property so writeRecord can call the
        // canonical constructor.
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("id");

        binder.readRecord(Owner.empty());

        add(firstName, lastName, address, city, telephone);
    }

    /** A text field the browser caps at its column length. */
    static TextField limitedTextField(String label, int maxLength) {
        TextField field = new TextField(label);
        field.setMaxLength(maxLength);
        return field;
    }

    /**
     * Starts a binding that validates the trimmed value. The component's own
     * maxLength check is switched off because it sees the untrimmed value;
     * the same limit is checked here, after trimming.
     */
    private Binder.BindingBuilder<Owner, String> trimmedAndRequired(
            TextField field, String requiredMessage, int maxLength) {
        return binder.forField(field)
                .withDefaultValidator(false)
                // UC-003 BR-006: surrounding spaces are removed before validation and recording.
                .withConverter(String::trim, s -> s)
                // UC-003 BR-001: every owner field is required; only spaces is blank.
                .asRequired(requiredMessage)
                // UC-003 BR-004: at most the column length, rejected on the field.
                .withValidator(new StringLengthValidator(
                        "At most " + maxLength + " characters", 0, maxLength));
    }

    /** Fill the fields from an existing owner (used by UC-006). */
    public void read(Owner owner) {
        binder.readRecord(owner);
    }

    /**
     * Validate field contents. On success returns a new {@link Owner} preserving
     * {@code existingId} (null for create). On failure the Binder attaches
     * field-level error messages and this returns empty.
     */
    public Optional<Owner> validateAndRead(Integer existingId) {
        try {
            Owner owner = binder.writeRecord();
            return Optional.of(new Owner(
                    existingId,
                    owner.firstName(),
                    owner.lastName(),
                    owner.address(),
                    owner.city(),
                    owner.telephone()));
        } catch (ValidationException _) {
            // UC-003 BR-005: the fields keep what the user entered; only the errors are added.
            return Optional.empty();
        }
    }
}
