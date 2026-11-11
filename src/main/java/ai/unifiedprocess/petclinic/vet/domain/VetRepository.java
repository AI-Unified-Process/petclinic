package ai.unifiedprocess.petclinic.vet.domain;

import static ai.unifiedprocess.demo.petclinic.database.Tables.SPECIALTIES;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VET_SPECIALTIES;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

import java.util.stream.Stream;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to the veterinarians and their specialties. Vets and specialties
 * are maintained directly in the database (C-011), so there are no writes.
 */
@Repository
@Transactional(readOnly = true)
public class VetRepository {

    private final DSLContext dsl;

    public VetRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * One chunk of the veterinarians grid, each vet with their specialties
     * loaded in the same query.
     */
    // UC-002 BR-001: rows are fetched in chunks of the size the grid asks for.
    public Stream<Vet> findPage(int offset, int limit) {
        return dsl.select(
                        VETS.ID,
                        VETS.FIRST_NAME,
                        VETS.LAST_NAME,
                        multiset(
                                select(SPECIALTIES.ID, SPECIALTIES.NAME)
                                        .from(VET_SPECIALTIES)
                                        .join(SPECIALTIES)
                                        .on(SPECIALTIES.ID.eq(VET_SPECIALTIES.SPECIALTY_ID))
                                        .where(VET_SPECIALTIES.VET_ID.eq(VETS.ID))
                                        // UC-002 BR-002: specialties alphabetically by name.
                                        .orderBy(SPECIALTIES.NAME.asc()))
                                .convertFrom(r -> r.map(mapping(Specialty::new))))
                .from(VETS)
                // UC-002 BR-004: by last name, then first name; id keeps ties stable.
                .orderBy(VETS.LAST_NAME.asc(), VETS.FIRST_NAME.asc(), VETS.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch(mapping(Vet::new))
                .stream();
    }

    /** Number of veterinarians, the size of the lazily loaded grid. */
    public int count() {
        return dsl.fetchCount(VETS);
    }
}
