package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Find Owners view, the screen of UC-004 Find Owners by Last Name.
 *
 * <p>So far it holds only the "Add Owner" entry point of UC-003 Register New
 * Owner (step 1); the last-name search is still to be built by UC-004.
 */
@Route(value = "owners/find", layout = MainLayout.class)
@PageTitle("Find Owners")
public class FindOwnersView extends VerticalLayout {

    public FindOwnersView() {
        H2 heading = new H2("Find Owners");
        heading.addClassNames("page-heading");

        Button addOwner = new Button("Add Owner",
                event -> getUI().ifPresent(ui -> ui.navigate(RegisterNewOwnerView.class)));

        add(heading, addOwner);
    }
}
