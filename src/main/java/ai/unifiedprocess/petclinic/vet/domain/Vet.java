package ai.unifiedprocess.petclinic.vet.domain;

import java.io.Serializable;
import java.util.List;

/**
 * A veterinarian of the clinic with the specialties they hold, as listed in the
 * veterinarians grid (UC-002).
 */
public record Vet(Integer id, String firstName, String lastName, List<Specialty> specialties)
        implements Serializable {

    public Vet {
        specialties = List.copyOf(specialties);
    }
}
