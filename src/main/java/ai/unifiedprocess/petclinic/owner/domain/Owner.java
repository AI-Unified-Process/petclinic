package ai.unifiedprocess.petclinic.owner.domain;

import java.io.Serializable;

/**
 * A pet owner registered with the clinic (UC-003). The id is {@code null} until
 * the owner is recorded; the database assigns it.
 */
public record Owner(Integer id, String firstName, String lastName, String address, String city,
        String telephone) implements Serializable {
}
