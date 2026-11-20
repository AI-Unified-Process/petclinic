package ai.unifiedprocess.petclinic.owner.domain;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access to the clinic's pet owners.
 */
@Repository
@Transactional(readOnly = true)
public class OwnerRepository {

    private final DSLContext dsl;

    public OwnerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Records a new owner and returns the identifier the database assigned.
     * The id of the given owner is ignored.
     */
    // UC-003 BR-003: the id comes from owners_seq, never from the user.
    @Transactional
    public Integer insert(Owner owner) {
        return dsl.insertInto(OWNERS)
                .set(OWNERS.FIRST_NAME, owner.firstName())
                .set(OWNERS.LAST_NAME, owner.lastName())
                .set(OWNERS.ADDRESS, owner.address())
                .set(OWNERS.CITY, owner.city())
                .set(OWNERS.TELEPHONE, owner.telephone())
                .returning(OWNERS.ID)
                .fetchOne(OWNERS.ID);
    }
}
