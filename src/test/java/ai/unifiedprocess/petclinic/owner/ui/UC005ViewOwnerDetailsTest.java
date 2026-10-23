package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.demo.petclinic.database.tables.records.OwnersRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.PetsRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.VisitsRecord;
import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.core.ui.NotFoundErrorView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static ai.unifiedprocess.demo.petclinic.database.Sequences.VISITS_SEQ;
import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VISITS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-005: View Owner Details.
 *
 * <p>Uses the seed owner "Jean Coleman" ({@link #OWNER_COLEMAN_ID}) who
 * has 2 pets (Max, Samantha) and 2 visits on each inserted out of
 * chronological order in {@code V2__seed_reference_data.sql} so the
 * UC-005 BR-001 sort is actually exercised.
 */
class UC005ViewOwnerDetailsTest extends PetClinicTestBase {

    @Autowired
    private DSLContext dsl;

    @Test
    @UseCase(id = "UC-005")
    void ownerDetailsShowNameAddressCityAndTelephone() {
        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Jean Coleman").single(),
                "Expected owner name to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("105 N. Lake St.").single(),
                "Expected address to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Monona").single(),
                "Expected city to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("6085552654").single(),
                "Expected telephone to be rendered");
    }

    @Test
    @UseCase(id = "UC-005", businessRules = "BR-002", requirements = "NFR-003")
    void petsAreListedAlphabeticallyByName() {
        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        List<String> names = find(H3.class).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max", "Samantha"), names);
    }

    @Test
    @UseCase(id = "UC-005", businessRules = "BR-001", requirements = "NFR-003")
    void visitsForEachPetAreListedChronologically() {
        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        // Max's two seed visits were inserted newest-first (2011-03-04 before
        // 2009-06-04), so this test actually exercises the ORDER BY in
        // VisitRepository. Max is alphabetically first, so its visits lead.
        List<String> visitTexts = find(Paragraph.class).all().stream()
                .map(Paragraph::getText)
                .filter(t -> t.contains("\u2014"))
                .toList();
        assertTrue(visitTexts.get(0).startsWith("2009-06-04"),
                "Expected chronological order, got: " + visitTexts);
        assertTrue(visitTexts.get(1).startsWith("2011-03-04"));
    }

    @Test
    @UseCase(id = "UC-005", businessRules = "BR-001", requirements = "NFR-003")
    void visitsOnTheSameDateAreListedInBookingOrder() {
        // Two visits for Max on one date. The later-booked one (higher id) is
        // stored first, so physical row order disagrees with booking order and
        // only the id tie-break puts them right. Rolled back with the test.
        long booked1st = dsl.nextval(VISITS_SEQ);
        long booked2nd = dsl.nextval(VISITS_SEQ);
        LocalDate sameDay = LocalDate.of(2012, 1, 10);
        dsl.insertInto(VISITS, VISITS.ID, VISITS.VISIT_DATE, VISITS.DESCRIPTION, VISITS.PET_ID)
                .values((int) booked2nd, sameDay, "booked second", PET_MAX_ID)
                .execute();
        dsl.insertInto(VISITS, VISITS.ID, VISITS.VISIT_DATE, VISITS.DESCRIPTION, VISITS.PET_ID)
                .values((int) booked1st, sameDay, "booked first", PET_MAX_ID)
                .execute();

        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        List<String> maxVisits = find(Paragraph.class).from(petCard("Max")).all().stream()
                .map(Paragraph::getText)
                .filter(t -> t.contains("\u2014"))
                .toList();
        assertEquals(List.of(
                "2009-06-04 \u2014 neutered",
                "2011-03-04 \u2014 rabies shot",
                "2012-01-10 \u2014 booked first",
                "2012-01-10 \u2014 booked second"), maxVisits);
    }

    @Test
    @UseCase(id = "UC-005")
    void viewingOwnerDetailsModifiesNoData() {
        Result<OwnersRecord> ownersBefore = dsl.selectFrom(OWNERS).orderBy(OWNERS.ID).fetch();
        Result<PetsRecord> petsBefore = dsl.selectFrom(PETS).orderBy(PETS.ID).fetch();
        Result<VisitsRecord> visitsBefore = dsl.selectFrom(VISITS).orderBy(VISITS.ID).fetch();

        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Jean Coleman").single(),
                "Expected the owner details to be rendered");
        assertEquals(ownersBefore, dsl.selectFrom(OWNERS).orderBy(OWNERS.ID).fetch(),
                "Success postcondition: expected the owners to be unchanged");
        assertEquals(petsBefore, dsl.selectFrom(PETS).orderBy(PETS.ID).fetch(),
                "Success postcondition: expected the pets to be unchanged");
        assertEquals(visitsBefore, dsl.selectFrom(VISITS).orderBy(VISITS.ID).fetch(),
                "Success postcondition: expected the visits to be unchanged");
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A1: Owner Not Found")
    void unknownOwnerModifiesNoData() {
        Result<OwnersRecord> ownersBefore = dsl.selectFrom(OWNERS).orderBy(OWNERS.ID).fetch();
        Result<PetsRecord> petsBefore = dsl.selectFrom(PETS).orderBy(PETS.ID).fetch();
        Result<VisitsRecord> visitsBefore = dsl.selectFrom(VISITS).orderBy(VISITS.ID).fetch();

        UI.getCurrent().navigate(ViewOwnerDetailsView.class, OwnerRouteParameters.forOwner(99999));

        assertDoesNotThrow(
                () -> find(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView to be rendered for unknown owner id");
        assertEquals(ownersBefore, dsl.selectFrom(OWNERS).orderBy(OWNERS.ID).fetch(),
                "Failure postcondition: expected the owners to be unchanged");
        assertEquals(petsBefore, dsl.selectFrom(PETS).orderBy(PETS.ID).fetch(),
                "Failure postcondition: expected the pets to be unchanged");
        assertEquals(visitsBefore, dsl.selectFrom(VISITS).orderBy(VISITS.ID).fetch(),
                "Failure postcondition: expected the visits to be unchanged");
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A1: Owner Not Found")
    void unknownOwnerRoutesToErrorView() {
        // UC-005 A1 now flows into UC-010: a missing owner id throws a
        // NotFoundException inside ViewOwnerDetailsView.beforeEnter(), which
        // is rerouted to NotFoundErrorView. The test base's navigate()
        // enforces target-type equality, so we bypass it via UI.navigate.
        com.vaadin.flow.component.UI.getCurrent().navigate(
                ViewOwnerDetailsView.class, OwnerRouteParameters.forOwner(99999));

        assertDoesNotThrow(
                () -> find(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView to be rendered for unknown owner id");
        assertTrue(find(ViewOwnerDetailsView.class).all().isEmpty(),
                "Expected ViewOwnerDetailsView not to be rendered for unknown owner id");
        NotFoundErrorView errorView = find(NotFoundErrorView.class).single();
        H2 heading = find(H2.class).from(errorView).single();
        assertEquals("Something happened...", heading.getText());
        // UC-010 BR-003: the message the application wrote for an unknown owner.
        assertEquals("Owner not found", find(Paragraph.class).from(errorView).single().getText());
    }

    @Test
    @UseCase(id = "UC-005")
    void editOwnerNavigatesToEditOwnerForm() {
        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        assertDoesNotThrow(
                () -> test(find(Button.class).withText("Edit Owner").single()).click(),
                "Expected exactly one \"Edit Owner\" action");
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/edit", activePath());
    }

    @Test
    @UseCase(id = "UC-005")
    void addNewPetNavigatesToAddPetForm() {
        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        assertDoesNotThrow(
                () -> test(find(Button.class).withText("Add New Pet").single()).click(),
                "Expected exactly one \"Add New Pet\" action");
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/new", activePath());
    }

    @Test
    @UseCase(id = "UC-005")
    void editPetNavigatesToEditPetFormForThatPet() {
        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        Component maxCard = petCard("Max");
        assertDoesNotThrow(
                () -> test(find(Button.class).withText("Edit Pet").from(maxCard).single()).click(),
                "Expected exactly one \"Edit Pet\" action on Max's card");
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit", activePath());
    }

    @Test
    @UseCase(id = "UC-005")
    void addVisitNavigatesToAddVisitFormForThatPet() {
        navigate(ViewOwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        Component maxCard = petCard("Max");
        assertDoesNotThrow(
                () -> test(find(Button.class).withText("Add Visit").from(maxCard).single()).click(),
                "Expected exactly one \"Add Visit\" action on Max's card");
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/visits/new", activePath());
    }

    private Component petCard(String petName) {
        H3 name = assertDoesNotThrow(
                () -> find(H3.class).withText(petName).single(),
                "Expected a pet heading for " + petName);
        return name.getParent().orElseThrow();
    }

    private static String activePath() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }
}
