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
 * UC-005 View Owner Details — the owner's contact information only.
 *
 * <p>UC-003 step 6 opens this view for a newly created owner. It shows the
 * owner's name, address, city, and telephone (UC-005 step 3); an id that
 * resolves to no owner lands on the not-found error view (UC-005 A1). The
 * pets, visits, and action links of UC-005 are not built yet.
 */
@Route(value = "owners/:" + OwnerRouteParameters.OWNER_ID, layout = MainLayout.class)
@PageTitle("Owner Information")
public class OwnerDetailsView extends VerticalLayout implements BeforeEnterObserver {

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
        Owner owner = event.getRouteParameters()
                .getInteger(OwnerRouteParameters.OWNER_ID)
                .flatMap(ownerRepository::findById)
                .orElseThrow(() -> new NotFoundException("Owner not found."));
        name.setText("Name: " + owner.firstName() + " " + owner.lastName());
        address.setText("Address: " + owner.address());
        city.setText("City: " + owner.city());
        telephone.setText("Telephone: " + owner.telephone());
    }
}
