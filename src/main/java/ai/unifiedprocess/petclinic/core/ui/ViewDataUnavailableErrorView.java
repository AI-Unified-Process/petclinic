package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Error view for a {@link ViewDataUnavailableException}: a lazy grid fetch that
 * failed after navigation. {@code ErrorHandlerUtil} redirects only to the error
 * view whose handled type equals the exception's class, hence this dedicated
 * view next to {@link ApplicationErrorView}. Renders the same
 * {@link ErrorShellView} inside {@link MainLayout} with HTTP 500, showing the
 * generic message and logging the cause (UC-010 BR-003).
 */
@ParentLayout(MainLayout.class)
public class ViewDataUnavailableErrorView extends ErrorShellView
        implements HasErrorParameter<ViewDataUnavailableException> {

    private static final Logger LOG = LoggerFactory.getLogger(ViewDataUnavailableErrorView.class);

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<ViewDataUnavailableException> parameter) {
        LOG.error("Data of a view could not be loaded", parameter.getCaughtException());
        return show(parameter, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
