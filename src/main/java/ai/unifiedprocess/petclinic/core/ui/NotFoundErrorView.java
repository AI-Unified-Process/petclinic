package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import jakarta.servlet.http.HttpServletResponse;

/**
 * UC-010 A1: "resource not found" variant of the application error view.
 * Overrides Vaadin's default {@code HasErrorParameter<NotFoundException>}
 * so that bad owner / pet / visit ids (as produced by UC-005, UC-006,
 * UC-007, UC-008, UC-009) and unknown routes all land on the same
 * {@link ErrorShellView} as the generic {@link ApplicationErrorView},
 * with HTTP 404.
 */
// UC-010 BR-002: the error view renders inside the shell, so drawer and header stay usable.
@ParentLayout(MainLayout.class)
public class NotFoundErrorView extends ErrorShellView implements HasErrorParameter<NotFoundException> {

    static final String PAGE_NOT_FOUND = "Page not found";

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        // UC-010 BR-003: the views write "Owner not found" and "Pet not found" for the user;
        // the router's own not-found failure carries framework text and is never shown.
        if (parameter.getException() instanceof ResourceNotFoundException notFound) {
            return show(notFound.getMessage(), HttpServletResponse.SC_NOT_FOUND);
        }
        return show(PAGE_NOT_FOUND, HttpServletResponse.SC_NOT_FOUND);
    }
}
