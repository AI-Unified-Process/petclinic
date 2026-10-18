package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import jakarta.servlet.http.HttpServletResponse;

/**
 * UC-010 A2 for a failure after navigation: the same {@link ErrorShellView}
 * as {@link ApplicationErrorView}, with HTTP 500, for a
 * {@link ViewDataUnavailableException}. It exists because
 * {@code ErrorHandlerUtil} redirects a late failure only to the error view
 * whose handled type equals the exception's class, which
 * {@code HasErrorParameter<Exception>} never does.
 */
// UC-010 BR-002: the error view renders inside the shell, so drawer and header stay usable.
@ParentLayout(MainLayout.class)
public class ViewDataUnavailableErrorView extends ErrorShellView
        implements HasErrorParameter<ViewDataUnavailableException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<ViewDataUnavailableException> parameter) {
        // UC-010 BR-003: the cause is a data-access error, whose message names the SQL.
        return showUnexpected(parameter, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
