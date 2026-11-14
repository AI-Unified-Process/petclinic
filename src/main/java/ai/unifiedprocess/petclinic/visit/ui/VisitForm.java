package ai.unifiedprocess.petclinic.visit.ui;

import ai.unifiedprocess.petclinic.visit.domain.Visit;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ReadOnlyHasValue;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Visit form of Book Visit for Pet (UC-009).
 *
 * <p>Validation:
 * <ul>
 *   <li>surrounding spaces are removed from the description before it is
 *       validated, and the trimmed value is recorded (BR-007)</li>
 *   <li>description required (BR-001) and at most as long as VISIT.description
 *       in {@code docs/entity_model.md} (BR-005)</li>
 *   <li>date pre-populated with today (BR-002) and required, past or future
 *       (BR-004) — clearing it is a change, not a way back to the default</li>
 *   <li>a rejected submit leaves every entered value in its field (BR-006)</li>
 * </ul>
 *
 * <p>Validation and field population are delegated to a {@link Binder}
 * configured for the immutable {@link Visit} record
 * ({@code readRecord} / {@code writeRecord}).
 */
public class VisitForm extends FormLayout {

    /** Column length of VISIT.description in {@code docs/entity_model.md}. */
    static final int DESCRIPTION_LENGTH = 255;

    private static final String REQUIRED = "required";

    private final DatePicker date;
    private final TextField description;
    private final Binder<Visit> binder = new Binder<>(Visit.class);

    public VisitForm() {
        date = new DatePicker("Date");

        // The browser caps the field at the column length; the component's own maxLength
        // check is switched off below because it sees the untrimmed value.
        description = new TextField("Description");
        description.setMaxLength(DESCRIPTION_LENGTH);

        // UC-009 BR-004: every visit has a date; any calendar date is accepted, so no min or max.
        binder.forField(date)
                .asRequired(REQUIRED)
                .bind("visitDate");
        binder.forField(description)
                .withDefaultValidator(false)
                // UC-009 BR-007: surrounding spaces are removed before validation and recording.
                .withConverter(String::trim, s -> s)
                // UC-009 BR-001: every visit has a non-blank description; only spaces is blank.
                .asRequired(REQUIRED)
                // UC-009 BR-005: at most 255 characters, rejected on the field like any other validation error.
                .withValidator(new StringLengthValidator(
                        "At most " + DESCRIPTION_LENGTH + " characters", 0, DESCRIPTION_LENGTH))
                .bind("description");
        // id and petId are record components but not form fields. Binder
        // requires a binding for every record property so writeRecord can
        // call the canonical constructor.
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("id");
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("petId");

        // UC-009 BR-002: the date defaults to today.
        binder.readRecord(new Visit(null, LocalDate.now(ZoneId.systemDefault()), "", null));

        setResponsiveSteps(new ResponsiveStep("0", 1));
        add(date, description);
    }

    /**
     * Validate the form. Returns the visit to book for {@code petId} on
     * success, or empty with field-level error messages attached by the
     * Binder.
     */
    public Optional<Visit> validateAndRead(Integer petId) {
        try {
            Visit visit = binder.writeRecord();
            return Optional.of(new Visit(null, visit.visitDate(), visit.description(), petId));
        } catch (ValidationException _) {
            // UC-009 BR-006: the fields keep what the user entered; only the errors are added.
            return Optional.empty();
        }
    }
}
