package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-005 View Owner Details.
 *
 * <p>So far it shows the owner's name, address, city, and telephone — the
 * details page UC-003 Register New Owner opens after a successful registration
 * (step 6) — and the application error view for an unknown owner (A1). The
 * pets with their visits and the action links are still to be built by UC-005.
 */
@Route(value = "owners/:" + OwnerRouteParameters.OWNER_ID, layout = MainLayout.class)
@PageTitle("Owner Details")
public class OwnerDetailsView extends VerticalLayout implements BeforeEnterObserver {

    static final String NOT_FOUND_MESSAGE = "Owner not found";

    private final transient OwnerRepository ownerRepository;
    private final Paragraph name = new Paragraph();
    private final Paragraph address = new Paragraph();
    private final Paragraph city = new Paragraph();
    private final Paragraph telephone = new Paragraph();

    public OwnerDetailsView(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;

        H2 heading = new H2("Owner Information");
        heading.addClassNames("page-heading");

        add(heading, name, address, city, telephone);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Owner owner = event.getRouteParameters().getInteger(OwnerRouteParameters.OWNER_ID)
                .flatMap(ownerRepository::findById)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        name.setText("Name: " + owner.firstName() + " " + owner.lastName());
        address.setText("Address: " + owner.address());
        city.setText("City: " + owner.city());
        telephone.setText("Telephone: " + owner.telephone());
    }
}
