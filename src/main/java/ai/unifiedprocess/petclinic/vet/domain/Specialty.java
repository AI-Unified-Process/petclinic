package ai.unifiedprocess.petclinic.vet.domain;

import java.io.Serializable;

/**
 * A veterinary specialty a vet may hold (radiology, surgery, ...).
 */
public record Specialty(Integer id, String name) implements Serializable {
}
