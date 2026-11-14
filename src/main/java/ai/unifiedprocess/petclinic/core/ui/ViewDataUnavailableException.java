package ai.unifiedprocess.petclinic.core.ui;

/**
 * Raised when a view's data cannot be read after navigation has finished —
 * for example by a grid's lazy fetch — where the router no longer turns an
 * exception into an error view. {@link ViewDataUnavailableErrorView} handles
 * exactly this type, because Vaadin redirects such a late failure only to the
 * error view registered for the exception's own class.
 */
public class ViewDataUnavailableException extends RuntimeException {

    public ViewDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
