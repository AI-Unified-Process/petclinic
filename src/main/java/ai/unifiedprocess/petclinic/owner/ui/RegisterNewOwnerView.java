package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Optional;

/**
 * UC-003 Register New Owner.
 *
 * <p>An empty owner form; a valid submission records the owner and opens their
 * details, an invalid one keeps every entry and marks the offending fields,
 * and Cancel returns to the Find Owners view.
 */
@Route(value = "owners/new", layout = MainLayout.class)
@PageTitle("New Owner")
public class RegisterNewOwnerView extends VerticalLayout {

    static final String CREATED_MESSAGE = "New Owner Created";
    static final String ERROR_MESSAGE = "There was an error in creating the owner.";

    private final transient OwnerRepository ownerRepository;
    private final OwnerForm form = new OwnerForm();

    public RegisterNewOwnerView(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;

        H2 heading = new H2("New Owner");
        heading.addClassNames("page-heading");

        Button addOwner = new Button("Add Owner", event -> save());
        addOwner.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancel = new Button("Cancel", event -> cancel());

        add(heading, form, new HorizontalLayout(addOwner, cancel));
    }

    private void save() {
        Optional<Owner> owner = form.validateAndRead();
        if (owner.isEmpty()) {
            Notification.show(ERROR_MESSAGE);
            return;
        }
        Integer id = ownerRepository.insert(owner.get());
        // Owner Details (UC-005) is addressed by location until its view exists.
        getUI().ifPresent(ui -> ui.navigate("owners/" + id));
        Notification.show(CREATED_MESSAGE);
    }

    private void cancel() {
        getUI().ifPresent(ui -> ui.navigate(FindOwnersView.class));
    }
}
