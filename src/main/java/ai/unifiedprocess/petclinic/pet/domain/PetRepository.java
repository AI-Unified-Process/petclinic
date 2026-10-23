package ai.unifiedprocess.petclinic.pet.domain;

import org.jooq.DSLContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.TYPES;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.row;

/**
 * jOOQ-backed persistence for {@link Pet} and {@link PetType}. Feeds UC-005
 * and UC-007/UC-008.
 */
@Repository
@Transactional(readOnly = true)
public class PetRepository {

    private final DSLContext dsl;

    public PetRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<PetType> findAllTypes() {
        return dsl.select(TYPES.ID, TYPES.NAME)
                .from(TYPES)
                .orderBy(TYPES.NAME.asc())
                .fetch(mapping(PetType::new));
    }

    /** An owner's pets, alphabetically by name (UC-005 BR-002). */
    public List<Pet> findByOwnerId(Integer ownerId) {
        return dsl.select(
                        PETS.ID, PETS.NAME, PETS.BIRTH_DATE,
                        row(TYPES.ID, TYPES.NAME).mapping(PetType::new),
                        PETS.OWNER_ID)
                .from(PETS)
                .join(TYPES).on(TYPES.ID.eq(PETS.TYPE_ID))
                .where(PETS.OWNER_ID.eq(ownerId))
                // UC-005 BR-002: an owner's pets are listed alphabetically by name.
                .orderBy(PETS.NAME.asc())
                .fetch(mapping(Pet::new));
    }

    public Optional<Pet> findById(Integer id) {
        return dsl.select(
                        PETS.ID, PETS.NAME, PETS.BIRTH_DATE,
                        row(TYPES.ID, TYPES.NAME).mapping(PetType::new),
                        PETS.OWNER_ID)
                .from(PETS)
                .join(TYPES).on(TYPES.ID.eq(PETS.TYPE_ID))
                .where(PETS.ID.eq(id))
                .fetchOptional(mapping(Pet::new));
    }

    /**
     * True if the owner already has a pet with the given name (case-insensitive),
     * excluding a pet with {@code excludePetId} (so it can be passed for updates
     * without matching the pet being edited). Pass {@code null} for creation.
     */
    public boolean existsByOwnerAndName(Integer ownerId, String name, Integer excludePetId) {
        // UC-007 BR-001: an owner cannot have two pets with the same name, compared case-insensitively.
        var condition = PETS.OWNER_ID.eq(ownerId)
                .and(lower(PETS.NAME).eq(name == null ? "" : name.toLowerCase()));
        // UC-008 BR-001: on update the pet being edited is not compared against itself.
        if (excludePetId != null) {
            condition = condition.and(PETS.ID.ne(excludePetId));
        }
        return dsl.fetchExists(dsl.selectFrom(PETS).where(condition));
    }

    /**
     * Records a new pet and returns its id, or empty when the owner already
     * has a pet of that name — including one another Clinic User recorded
     * after the view's {@link #existsByOwnerAndName} check (UC-007 A7).
     */
    @Transactional
    public Optional<Integer> insert(Pet pet) {
        return unlessNameTaken(tx -> tx.insertInto(PETS)
                .set(PETS.NAME, pet.name())
                .set(PETS.BIRTH_DATE, pet.birthDate())
                .set(PETS.TYPE_ID, pet.type().id())
                .set(PETS.OWNER_ID, pet.ownerId())
                .returning(PETS.ID)
                .fetchOne()
                .getId());
    }

    /**
     * Store a pet's name, birth date and type (UC-008); the form guarantees a type (UC-008 BR-003).
     * Returns false, changing nothing, when another pet of the owner has that name by now (UC-008 A8).
     */
    @Transactional
    public boolean update(Pet pet) {
        return unlessNameTaken(tx -> tx.update(PETS)
                .set(PETS.NAME, pet.name())
                .set(PETS.BIRTH_DATE, pet.birthDate())
                .set(PETS.TYPE_ID, pet.type().id())
                .where(PETS.ID.eq(pet.id()))
                .execute())
                .isPresent();
    }

    /**
     * UC-007 BR-001: pets_owner_name_unique decides the race the view's check
     * leaves open. The write runs in a savepoint, so a rejected one leaves the
     * surrounding transaction usable.
     */
    private <T> Optional<T> unlessNameTaken(Function<DSLContext, T> write) {
        try {
            return Optional.of(dsl.transactionResult(configuration -> write.apply(configuration.dsl())));
        } catch (DuplicateKeyException _) {
            return Optional.empty();
        }
    }
}
