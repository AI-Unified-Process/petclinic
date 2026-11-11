package ai.unifiedprocess.petclinic.core.ui;

import java.io.Serial;

/**
 * A data read that failed after navigation had finished — a lazy grid fetch —
 * where the router no longer turns an exception into an error view. The view
 * wraps the cause in this type and redirects to
 * {@link ViewDataUnavailableErrorView} (process.md, Failure paths).
 *
 * <p>Its message is the generic one of UC-010 BR-003; the cause's message may
 * carry technical details and goes to the server log only.
 */
public class ViewDataUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The generic message UC-010 BR-003 shows for an unexpected failure. */
    public static final String MESSAGE = "An unexpected error occurred. Please try again later.";

    public ViewDataUnavailableException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
