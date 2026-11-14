package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-010 View Application Error.
 *
 * The {@code /oups} demonstration route. Always throws a
 * {@link RuntimeException} so the navigation drawer's "Error" link has a
 * target that exercises {@link ApplicationErrorView}. Mirrors Spring
 * PetClinic's {@code CrashController}, including the exception message.
 */
// UC-010 BR-001 (UC-001 BR-001): no access annotation and no security; any visitor reaches the error view.
@Route(value = "oups", layout = MainLayout.class)
@PageTitle("Error")
public class ViewApplicationErrorView extends Div implements BeforeEnterObserver {

    // UC-010 BR-004: /oups always fails with the showcase message.
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        throw new DemonstrationException(
                "Expected: controller used to showcase what happens when an exception is thrown");
    }
}
