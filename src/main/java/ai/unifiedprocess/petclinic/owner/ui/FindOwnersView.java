package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * The Find Owners view, reached from "Find Owners" in the main menu.
 *
 * <p>Holds the "Add Owner" action from which the Clinic User starts UC-003
 * Register New Owner (step 1), and to which a cancelled registration returns
 * (UC-003 A2). The last-name search of UC-004 is not implemented yet.
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
