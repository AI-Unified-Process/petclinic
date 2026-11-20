package ai.unifiedprocess.petclinic.owner.ui;

import com.vaadin.flow.router.RouteParameters;

/**
 * Route parameters of every route under {@code /owners/…}, whichever feature
 * owns the view.
 */
public final class OwnerRouteParameters {

    /** The owner id in {@code owners/:ownerId}. */
    public static final String OWNER_ID = "ownerId";

    private OwnerRouteParameters() {
    }

    /** Parameters that address the owner with the given id. */
    public static RouteParameters forOwner(Integer ownerId) {
        return new RouteParameters(OWNER_ID, String.valueOf(ownerId));
    }
}
