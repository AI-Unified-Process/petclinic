package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.ValueProvider;
import java.util.Optional;

/**
 * The owner's contact details form, the validation boundary for writing an owner
 * (ADR-005). Used by UC-003 Register New Owner.
 */
public class OwnerForm extends FormLayout {

    static final String BLANK_MESSAGE = "Must not be blank";
    static final String TELEPHONE_MESSAGE = "Telephone must be exactly 10 digits";

    private final TextField firstName = textField("First Name", 30);
    private final TextField lastName = textField("Last Name", 30);
    private final TextField address = textField("Address", 255);
    private final TextField city = textField("City", 80);
    private final TextField telephone = new TextField("Telephone");

    private final Binder<Owner> binder = new Binder<>();

    public OwnerForm() {
        telephone.setId("telephone");
        telephone.setRequiredIndicatorVisible(true);

        bindMandatory(firstName, Owner::firstName);
        bindMandatory(lastName, Owner::lastName);
        bindMandatory(address, Owner::address);
        bindMandatory(city, Owner::city);
        binder.forField(telephone)
                // UC-003 BR-001: telephone is required; only spaces count as blank.
                .asRequired(BLANK_MESSAGE)
                .withValidator(value -> !value.strip().isEmpty(), BLANK_MESSAGE)
                // UC-003 BR-002: telephone is exactly 10 digits.
                .withValidator(value -> value.strip().isEmpty() || value.strip().matches("\\d{10}"),
                        TELEPHONE_MESSAGE)
                .bind(Owner::telephone, (owner, value) -> {
                });

        add(firstName, lastName, address, city, telephone);
    }

    /**
     * Validates every field and returns the owner to record, or shows the error
     * on each offending field and returns empty. The entered values stay in the
     * form either way.
     */
    // UC-003 BR-005: rejection marks the offending fields and clears no entry.
    public Optional<Owner> validateAndRead() {
        if (!binder.validate().isOk()) {
            return Optional.empty();
        }
        return Optional.of(new Owner(null, trimmed(firstName), trimmed(lastName),
                trimmed(address), trimmed(city), trimmed(telephone)));
    }

    // UC-003 BR-006: surrounding spaces are removed before recording.
    private static String trimmed(TextField field) {
        return field.getValue().strip();
    }

    private static TextField textField(String label, int maxLength) {
        TextField field = new TextField(label);
        field.setId(label.toLowerCase().replace(' ', '-'));
        field.setRequiredIndicatorVisible(true);
        // UC-003 BR-004: maximum length, reported on the field like any other error.
        field.setMaxLength(maxLength);
        field.setI18n(new TextField.TextFieldI18n()
                .setMaxLengthErrorMessage("Must be at most " + maxLength + " characters"));
        return field;
    }

    private void bindMandatory(TextField field, ValueProvider<Owner, String> getter) {
        binder.forField(field)
                // UC-003 BR-001: required; with BR-006 a value of only spaces is blank.
                .asRequired(BLANK_MESSAGE)
                .withValidator(value -> !value.strip().isEmpty(), BLANK_MESSAGE)
                .bind(getter, (owner, value) -> {
                });
    }
}
