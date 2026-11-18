package ai.unifiedprocess.petclinic.owner.ui;

import com.vaadin.flow.router.RouteParameters;

/**
 * Names and builds the route parameters of every route under {@code /owners/…},
 * whichever feature owns the view.
 */
public final class OwnerRouteParameters {

    /** The owner's identifier in {@code /owners/:ownerId}. */
    public static final String OWNER_ID = "ownerId";

    private OwnerRouteParameters() {
    }

    /** Parameters addressing the owner with the given identifier. */
    public static RouteParameters forOwner(int ownerId) {
        return new RouteParameters(OWNER_ID, String.valueOf(ownerId));
    }
}
