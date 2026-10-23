package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.ErrorParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one error screen of UC-010. Vaadin picks the error view whose
 * {@code HasErrorParameter} type parameter is closest to the thrown exception,
 * so {@link ApplicationErrorView}, {@link NotFoundErrorView} and
 * {@link ViewDataUnavailableErrorView} exist to carry that parameter and the
 * HTTP status that goes with it. Everything the user sees — the shell, the
 * heading, the message, the way home — is here and identical for all of them.
 *
 * <p>UC-010 BR-003: the browser only ever sees a message the application wrote
 * for the user. Any other failure is shown as {@link #UNEXPECTED_ERROR_MESSAGE},
 * and its message and stack trace go to the server log.
 */
abstract class ErrorShellView extends VerticalLayout {

    static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    private static final Logger LOG = LoggerFactory.getLogger(ErrorShellView.class);

    private final ErrorPanel panel;

    ErrorShellView() {
        setSizeFull();
        setPadding(false);
        panel = new ErrorPanel();
        add(panel);
    }

    /** Renders a failure whose message the application wrote for the user. */
    protected int showMessage(ErrorParameter<? extends Exception> parameter, int httpStatus) {
        return show(parameter.hasCustomMessage()
                ? parameter.getCustomMessage()
                : parameter.getException().getMessage(), httpStatus);
    }

    /** Renders a message the application wrote for the user. */
    protected int show(String message, int httpStatus) {
        panel.setMessage(message);
        return httpStatus;
    }

    /** Renders any other failure without its internals, which are logged instead. */
    protected int showUnexpected(ErrorParameter<? extends Exception> parameter, int httpStatus) {
        LOG.error("Unexpected failure shown as the application error view", parameter.getException());
        panel.setMessage(UNEXPECTED_ERROR_MESSAGE);
        return httpStatus;
    }
}
