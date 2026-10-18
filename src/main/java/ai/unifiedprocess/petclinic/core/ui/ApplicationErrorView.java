package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import jakarta.servlet.http.HttpServletResponse;

/**
 * UC-010 A2: generic application error view. Catches any uncaught
 * {@link Exception} raised during navigation — including the deliberate
 * throw from {@link CrashView} — and renders the {@link ErrorShellView}
 * inside {@link MainLayout} with HTTP 500. Only the demonstration message is
 * shown verbatim; every other failure gets the generic message.
 *
 * <p>More specific errors (currently only {@code NotFoundException}) are
 * handled by {@link NotFoundErrorView}, which wins over this view by
 * virtue of its narrower generic type.
 */
// UC-010 BR-002: the error view renders inside the shell, so drawer and header stay usable.
@ParentLayout(MainLayout.class)
public class ApplicationErrorView extends ErrorShellView implements HasErrorParameter<Exception> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        // UC-010 BR-004: the /oups message is the application's own and is shown as is;
        // UC-010 BR-003: any other message may carry internals and is not.
        if (parameter.getException() instanceof DemonstrationException) {
            return showMessage(parameter, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return showUnexpected(parameter, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
