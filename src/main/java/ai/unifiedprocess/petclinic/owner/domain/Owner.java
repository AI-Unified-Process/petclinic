package ai.unifiedprocess.petclinic.owner.domain;

import java.io.Serializable;

/**
 * A pet owner registered with the clinic. The id is {@code null} until the owner
 * is recorded (UC-003 BR-003).
 */
public record Owner(
        Integer id, String firstName, String lastName, String address, String city, String telephone)
        implements Serializable {
}
