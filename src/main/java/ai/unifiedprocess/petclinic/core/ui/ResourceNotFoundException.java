package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.router.NotFoundException;

/**
 * A not-found failure whose message the application wrote for the user
 * (UC-010 BR-003): the views throw it for an owner or a pet they cannot
 * resolve, and {@link NotFoundErrorView} shows its message. Any other
 * {@link NotFoundException} — the router's own, for a URL that matches no
 * view — carries framework text and is shown as "Page not found" instead.
 */
public class ResourceNotFoundException extends NotFoundException {

    static final String OWNER_NOT_FOUND = "Owner not found";
    static final String PET_NOT_FOUND = "Pet not found";

    private ResourceNotFoundException(String message) {
        super(message);
    }

    /** No owner exists with the requested identifier. */
    public static ResourceNotFoundException owner() {
        return new ResourceNotFoundException(OWNER_NOT_FOUND);
    }

    /** The requested pet does not exist or belongs to another owner. */
    public static ResourceNotFoundException pet() {
        return new ResourceNotFoundException(PET_NOT_FOUND);
    }
}
