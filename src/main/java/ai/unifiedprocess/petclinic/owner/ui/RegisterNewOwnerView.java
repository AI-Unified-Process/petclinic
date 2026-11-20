package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Optional;

/**
 * UC-003 Register New Owner.
 *
 * <p>Shows an empty owner form. A valid submission records the owner and opens
 * the Owner Details view of the new owner (UC-005); a rejected one keeps the
 * form open with the errors on the offending fields (A1). Cancel discards the
 * entries and returns to the Find Owners view (A2).
 */
@Route(value = "owners/new", layout = MainLayout.class)
@PageTitle("Add Owner")
public class RegisterNewOwnerView extends VerticalLayout {

    static final String CREATED_MESSAGE = "New Owner Created";
    static final String ERROR_MESSAGE = "There was an error in creating the owner.";

    /** Route of the Find Owners view (UC-004), as the main menu names it. */
    private static final String FIND_OWNERS_ROUTE = "owners/find";
    /** Route prefix of the Owner Details view (UC-005), {@code owners/:ownerId}. */
    private static final String OWNER_DETAILS_ROUTE = "owners/";

    private final transient OwnerRepository ownerRepository;
    private final OwnerForm form = new OwnerForm();

    public RegisterNewOwnerView(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;

        H2 heading = new H2("Owner");
        heading.addClassNames("page-heading");

        Button add = new Button("Add Owner", event -> submit());
        add.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancel = new Button("Cancel", event -> cancel());

        add(heading, form, new HorizontalLayout(add, cancel));
    }

    private void submit() {
        Optional<Owner> owner = form.validateAndRead();
        if (owner.isEmpty()) {
            Notification.show(ERROR_MESSAGE).addThemeVariants(NotificationVariant.ERROR);
            return;
        }
        Integer ownerId = ownerRepository.insert(owner.get());
        Notification.show(CREATED_MESSAGE).addThemeVariants(NotificationVariant.SUCCESS);
        getUI().ifPresent(ui -> ui.navigate(OWNER_DETAILS_ROUTE + ownerId));
    }

    private void cancel() {
        getUI().ifPresent(ui -> ui.navigate(FIND_OWNERS_ROUTE));
    }
}
