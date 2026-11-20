package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.function.ValueProvider;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The owner form of UC-003: first name, last name, address, city, and
 * telephone, with the field-level rules of the use case. A rejected value is
 * reported on its field and every entered value stays in the form.
 */
public class OwnerForm extends FormLayout {

    static final int FIRST_NAME_MAX = 30;
    static final int LAST_NAME_MAX = 30;
    static final int ADDRESS_MAX = 255;
    static final int CITY_MAX = 80;
    /** Column length of owners.telephone in the entity model. */
    static final int TELEPHONE_MAX = 20;

    static final String TELEPHONE_MESSAGE = "Telephone must be exactly 10 digits";

    private static final Pattern TEN_DIGITS = Pattern.compile("[0-9]{10}");

    private final TextField firstName = new TextField("First Name");
    private final TextField lastName = new TextField("Last Name");
    private final TextField address = new TextField("Address");
    private final TextField city = new TextField("City");
    private final TextField telephone = new TextField("Telephone");

    private final Binder<Owner> binder = new Binder<>();

    public OwnerForm() {
        bindText(firstName, "First name", FIRST_NAME_MAX, Owner::firstName);
        bindText(lastName, "Last name", LAST_NAME_MAX, Owner::lastName);
        bindText(address, "Address", ADDRESS_MAX, Owner::address);
        bindText(city, "City", CITY_MAX, Owner::city);

        telephone.setMaxLength(TELEPHONE_MAX);
        telephone.setI18n(new TextField.TextFieldI18n().setMaxLengthErrorMessage(TELEPHONE_MESSAGE));
        binder.forField(telephone)
                // UC-003 BR-001: telephone is required; only spaces counts as blank.
                .asRequired(notBlank("Telephone is required"))
                // UC-003 BR-006: leading and trailing spaces are removed.
                .withConverter(String::strip, value -> value)
                // UC-003 BR-002: an owner's telephone number is exactly 10 digits.
                .withValidator(value -> TEN_DIGITS.matcher(value).matches(), TELEPHONE_MESSAGE)
                .bind(Owner::telephone, null);

        add(firstName, lastName, address, city, telephone);
    }

    private void bindText(TextField field, String name, int maxLength,
            ValueProvider<Owner, String> getter) {
        String tooLong = name + " may be at most " + maxLength + " characters";
        // UC-003 BR-004: the field holds at most the length of the entity model.
        field.setMaxLength(maxLength);
        field.setI18n(new TextField.TextFieldI18n().setMaxLengthErrorMessage(tooLong));
        binder.forField(field)
                // UC-003 BR-001: the field is required; only spaces counts as blank.
                .asRequired(notBlank(name + " is required"))
                // UC-003 BR-006: leading and trailing spaces are removed.
                .withConverter(String::strip, value -> value)
                // UC-003 BR-004: longer input is rejected on the field.
                .withValidator(value -> value.length() <= maxLength, tooLong)
                .bind(getter, null);
    }

    private static Validator<String> notBlank(String message) {
        return (value, context) -> value == null || value.isBlank()
                ? ValidationResult.error(message)
                : ValidationResult.ok();
    }

    /**
     * Validates every field and returns the owner with the trimmed values, or
     * an empty result with the errors shown on the offending fields. The
     * entered values stay in the form either way (UC-003 BR-005).
     */
    public Optional<Owner> validateAndRead() {
        // UC-003 BR-005: errors go on the fields; entered values are left as they are.
        if (!binder.validate().isOk()) {
            return Optional.empty();
        }
        return Optional.of(new Owner(null,
                firstName.getValue().strip(),
                lastName.getValue().strip(),
                address.getValue().strip(),
                city.getValue().strip(),
                telephone.getValue().strip()));
    }
}
