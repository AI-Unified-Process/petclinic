package ai.unifiedprocess.petclinic.visit.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.core.ui.NotFoundErrorView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-009: Book Visit for Pet.
 *
 * <p>Books a visit on Jean Coleman's {@link #PET_MAX_ID}. The seed data
 * already gives Max two visits (out of chronological order), so the
 * newly booked visit must be asserted as "contained in" the visit list
 * rather than by exact contents. The mismatched-owner case uses
 * {@link #OWNER_FRANKLIN_ID} — Franklin doesn't own Max — to trigger A2.
 */
class UC009BookVisitForPetTest extends PetClinicTestBase {

    /** Max's visits in V2__seed_reference_data.sql, as the form renders them. */
    private static final List<String> MAX_SEEDED_VISITS = List.of(
            "2011-03-04 \u2014 rabies shot",
            "2009-06-04 \u2014 neutered");

    @Test
    @UseCase(id = "UC-009", businessRules = "BR-002")
    void bookingVisitPersistsAndReturnsToOwnerDetails() {
        openVisitFormForMax();

        // Step 2: the pet's name and previous visits are shown for context
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Pet: Max").single(),
                "Expected the visit form to name the pet");
        assertTrue(renderedVisitLines(find(AddVisitView.class).single()).containsAll(MAX_SEEDED_VISITS),
                "Expected Max's seeded visits in the previous visits list");

        // BR-002: date pre-populated with today
        assertEquals(LocalDate.now(), find(DatePicker.class).withCaption("Date").single().getValue());

        test(find(TextField.class).withCaption("Description").single()).setValue("Annual check-up");
        test(find(Button.class).withText("Add Visit").single()).click();

        // Returned to Coleman's details page
        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // The newly booked visit should be rendered on the details page.
        // Visits are rendered as Paragraph("yyyy-MM-dd — description"); a
        // fresh paragraph matching today + "Annual check-up" proves the
        // insert landed in the database without touching VisitRepository.
        String expectedVisit = LocalDate.now() + " \u2014 Annual check-up";
        List<String> visitLines = renderedVisitLines(find(OwnerDetailsView.class).single());
        assertTrue(visitLines.contains(expectedVisit),
                "Expected to see '" + expectedVisit + "' in visits, got: " + visitLines);
    }

    @Test
    @UseCase(id = "UC-009", businessRules = "BR-001", scenario = "A1: Missing Description")
    void blankDescriptionIsRejectedThenBookedAfterCorrection() {
        openVisitFormForMax();

        test(find(Button.class).withText("Add Visit").single()).click();

        // A1 step 1: the form is shown again with the error on description
        TextField description = find(TextField.class).withCaption("Description").single();
        assertTrue(description.isInvalid());
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/visits/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // A1 steps 2-3: enter a description and continue at step 3
        test(description).setValue("Limping on the left hind leg");
        test(find(Button.class).withText("Add Visit").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
        String expectedVisit = LocalDate.now() + " \u2014 Limping on the left hind leg";
        List<String> visitLines = renderedVisitLines(find(OwnerDetailsView.class).single());
        assertTrue(visitLines.contains(expectedVisit),
                "Expected to see '" + expectedVisit + "' in visits, got: " + visitLines);
    }

    @Test
    @UseCase(id = "UC-009", businessRules = "BR-001", scenario = "A1: Missing Description")
    void blankDescriptionPersistsNoVisit() {
        openVisitFormForMax();
        test(find(Button.class).withText("Add Visit").single()).click();
        assertTrue(find(TextField.class).withCaption("Description").single().isInvalid());

        // Failure postcondition: no visit is persisted. Reopen the form, which
        // reloads Max's visits from the database, and expect only the seeded ones.
        openVisitFormForMax();
        assertEquals(MAX_SEEDED_VISITS.stream().sorted().toList(),
                renderedVisitLines(find(AddVisitView.class).single()).stream().sorted().toList());
    }

    @Test
    @UseCase(id = "UC-009", scenario = "A4: Visit Booking Cancelled")
    void cancellingDiscardsEnteredValuesAndReturnsToDetails() {
        openVisitFormForMax();

        test(find(TextField.class).withCaption("Description").single()).setValue("Annual check-up");
        test(find(Button.class).withText("Cancel").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Failure postcondition "No visit is persisted": reopen the form, which
        // reloads Max's visits from the database, and expect only the seeded ones.
        openVisitFormForMax();
        assertEquals(MAX_SEEDED_VISITS.stream().sorted().toList(),
                renderedVisitLines(find(AddVisitView.class).single()).stream().sorted().toList());
    }

    @Test
    @UseCase(id = "UC-009", businessRules = "BR-003", scenario = "A2: Pet Not Owned by Given Owner")
    void mismatchedOwnerAndPetRoutesToErrorView() {
        // Trying to book a visit for Coleman's Max under Franklin's ownership
        // fails the BeforeEnter consistency check and is rerouted to the
        // UC-010 error view. Use UI.navigate to bypass the test base's
        // target-type enforcement, since routing ends up on NotFoundErrorView.
        UI.getCurrent().navigate(AddVisitView.class,
                OwnerRouteParameters.forPet(OWNER_FRANKLIN_ID, PET_MAX_ID));

        assertDoesNotThrow(
                () -> find(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView for mismatched owner/pet");
    }

    @Test
    @UseCase(id = "UC-009", scenario = "A3: Owner Not Found")
    void unknownOwnerRoutesToErrorView() {
        // Navigate by location string: the typed navigate(Class, …) asserts the
        // resulting view type and would throw once the router reroutes to the
        // UC-010 error view.
        UI.getCurrent().navigate("owners/99999/pets/" + PET_MAX_ID + "/visits/new");

        assertDoesNotThrow(
                () -> find(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView when the owner id resolves to nothing");
        Paragraph message = find(Paragraph.class)
                .from(find(NotFoundErrorView.class).single()).single();
        assertTrue(message.getText().contains("99999"),
                "Expected the message to name the missing owner id, got: " + message.getText());
    }

    private void openVisitFormForMax() {
        navigate(AddVisitView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
    }

    /** Visits are rendered as Paragraph("yyyy-MM-dd — description"). */
    private List<String> renderedVisitLines(Component view) {
        return find(Paragraph.class).from(view).all().stream()
                .map(Paragraph::getText)
                .filter(t -> t.contains("\u2014"))
                .toList();
    }
}
