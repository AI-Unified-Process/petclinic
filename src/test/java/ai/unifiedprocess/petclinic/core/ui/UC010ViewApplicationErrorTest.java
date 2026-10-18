package ai.unifiedprocess.petclinic.core.ui;

import ai.unifiedprocess.demo.petclinic.database.tables.records.OwnersRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.PetsRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.SpecialtiesRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.TypesRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.VetSpecialtiesRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.VetsRecord;
import ai.unifiedprocess.demo.petclinic.database.tables.records.VisitsRecord;
import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import ai.unifiedprocess.petclinic.vet.ui.VetsView;
import ai.unifiedprocess.petclinic.welcome.ui.WelcomeView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.SPECIALTIES;
import static ai.unifiedprocess.demo.petclinic.database.Tables.TYPES;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VET_SPECIALTIES;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VISITS;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-010: View Application Error.
 *
 * <p>Covers the main flow from the drawer's "Error" link, the two alternative
 * flows — A1 (unknown owner → not-found variant) and A2 (the {@code /oups}
 * demonstration and a genuinely unexpected failure → generic variant) — and
 * the shell around both variants. The unexpected failure is a real one: the
 * owners table is renamed inside the test transaction, which is rolled back.
 */
class UC010ViewApplicationErrorTest extends PetClinicTestBase {

    /** BR-003: the text every failure gets whose message the application did not write. */
    private static final String GENERIC_MESSAGE = "An unexpected error occurred. Please try again later.";

    @Autowired
    private DSLContext dsl;

    @Test
    @UseCase(id = "UC-010")
    void errorDrawerLinkShowsTheErrorView() {
        navigate(WelcomeView.class);

        test(find(SideNav.class).single()).clickItem("Error");

        assertEquals("oups", UI.getCurrent().getInternals().getActiveViewLocation().getPath(),
                "Expected the \"Error\" drawer link to lead to the /oups demonstration route");
        ApplicationErrorView errorView = find(ApplicationErrorView.class).single();
        assertEquals("Something happened...", find(H2.class).from(errorView).single().getText());
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Unexpected Error")
    void navigatingToOupsShowsApplicationErrorView() {
        UI.getCurrent().navigate("oups");

        assertDoesNotThrow(
                () -> find(ApplicationErrorView.class).single(),
                "Expected ApplicationErrorView to be rendered for /oups");
        ApplicationErrorView errorView = find(ApplicationErrorView.class).single();
        H2 heading = find(H2.class).from(errorView).single();
        assertEquals("Something happened...", heading.getText());
    }

    @Test
    @UseCase(id = "UC-010")
    void backToHomeLinkReturnsToTheWelcomePage() {
        UI.getCurrent().navigate("oups");

        ApplicationErrorView errorView = find(ApplicationErrorView.class).single();
        RouterLink backLink = find(RouterLink.class).from(errorView).single();
        assertEquals("Back to Home", backLink.getText());

        test(backLink).click();

        assertEquals("", UI.getCurrent().getInternals().getActiveViewLocation().getPath(),
                "Expected \"Back to Home\" to return to the welcome page");
        assertDoesNotThrow(() -> find(WelcomeView.class).single(),
                "Expected the welcome page after following \"Back to Home\"");
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A1: Resource Not Found")
    void unknownOwnerRoutesToNotFoundErrorView() {
        // Bypass the test base's navigate(Class, ...) which validates target
        // type equality — routing lands on NotFoundErrorView here.
        UI.getCurrent().navigate(OwnerDetailsView.class,
                OwnerRouteParameters.forOwner(99999));

        assertDoesNotThrow(
                () -> find(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView for unknown owner id");
        NotFoundErrorView errorView = find(NotFoundErrorView.class).single();
        H2 heading = find(H2.class).from(errorView).single();
        assertEquals("Something happened...", heading.getText());
        Paragraph message = find(Paragraph.class).from(errorView).single();
        assertTrue(message.getText().contains("99999"),
                "Expected the not-found message to include the missing owner id, got: "
                        + message.getText());
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Unexpected Error", businessRules = "BR-002")
    void genericErrorViewRendersInsideTheShell() {
        UI.getCurrent().navigate("oups");

        assertRenderedInsideTheShell(ApplicationErrorView.class);
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A1: Resource Not Found", businessRules = "BR-003")
    void urlMatchingNoViewRoutesToNotFoundErrorView() {
        // The router itself raises this not-found failure, so its message is
        // not one a view wrote; BR-003 still forbids technical details in it.
        UI.getCurrent().navigate("no-such-page");

        assertDoesNotThrow(
                () -> find(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView for a URL that matches no view");
        NotFoundErrorView errorView = find(NotFoundErrorView.class).single();
        assertEquals("Something happened...", find(H2.class).from(errorView).single().getText());
        String message = find(Paragraph.class).from(errorView).single().getText();
        assertFalse(message.isBlank(), "Expected the failure message on the not-found variant");
        assertFalse(message.contains("Exception"),
                "Expected no exception type on the error view, got: " + message);
        assertFalse(message.contains("\tat "),
                "Expected no stack trace on the error view, got: " + message);
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A1: Resource Not Found", businessRules = "BR-002")
    void notFoundErrorViewRendersInsideTheShell() {
        UI.getCurrent().navigate(OwnerDetailsView.class,
                OwnerRouteParameters.forOwner(99999));

        assertRenderedInsideTheShell(NotFoundErrorView.class);
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Unexpected Error", businessRules = "BR-003")
    void unexpectedFailureShowsTheGenericMessageWithoutTechnicalDetails() {
        // The owner details query now fails with a database error whose own
        // message names the missing relation — exactly what BR-003 keeps away.
        dsl.alterTable(OWNERS).renameTo(name("owners_unavailable")).execute();

        UI.getCurrent().navigate(OwnerDetailsView.class,
                OwnerRouteParameters.forOwner(OWNER_COLEMAN_ID));

        ApplicationErrorView errorView = find(ApplicationErrorView.class).single();
        String message = find(Paragraph.class).from(errorView).single().getText();
        assertEquals(GENERIC_MESSAGE, message,
                "Expected the generic message for a failure the application did not describe");
        assertFalse(message.toLowerCase().contains("owners"),
                "Expected none of the database error on the error view, got: " + message);
        assertFalse(message.toLowerCase().contains("relation"),
                "Expected none of the database error on the error view, got: " + message);
    }

    @Test
    @UseCase(id = "UC-010", businessRules = "BR-001")
    void errorViewIsReachableWithoutAuthentication() {
        assertDoesNotThrow(() -> UI.getCurrent().navigate("oups"),
                "Expected /oups to be reachable without authentication");
        assertDoesNotThrow(() -> find(ApplicationErrorView.class).single(),
                "Expected the error view to render anonymously, as for UC-001 and UC-002");
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Unexpected Error", businessRules = {"BR-003", "BR-004"})
    void oupsRouteAlwaysFailsWithTheShowcaseMessage() {
        UI.getCurrent().navigate("oups");

        ApplicationErrorView errorView = find(ApplicationErrorView.class).single();
        Paragraph message = find(Paragraph.class).from(errorView).single();
        // BR-004 pins the exact wording, which the original Spring PetClinic
        // CrashController uses — an approximate match would let it drift.
        // BR-003: the application wrote this message, so it is shown as is.
        assertEquals(
                "Expected: controller used to showcase what happens when an exception is thrown",
                message.getText());
    }

    @Test
    @UseCase(id = "UC-010")
    void errorFlowsModifyNoData() {
        List<Result<?>> before = snapshot();

        UI.getCurrent().navigate("oups");
        UI.getCurrent().navigate(OwnerDetailsView.class,
                OwnerRouteParameters.forOwner(99999));

        assertEquals(before, snapshot(),
                "Success postcondition: expected no data to be modified by the error flows");
    }

    /** BR-002, NFR-005: the drawer and header surround the error view and still lead away from it. */
    private void assertRenderedInsideTheShell(Class<? extends Component> errorViewType) {
        MainLayout shell = find(MainLayout.class).single();
        assertDoesNotThrow(() -> find(errorViewType).from(shell).single(),
                "Expected the error view inside the application shell");
        assertDoesNotThrow(() -> find(H1.class).withText("PetClinic").from(shell).single(),
                "Expected the header to remain on the error view");
        assertEquals(List.of("Home", "Find Owners", "Veterinarians", "Error"),
                find(SideNavItem.class).from(shell).all().stream().map(SideNavItem::getLabel).toList(),
                "Expected the navigation drawer to remain on the error view");

        test(find(SideNav.class).from(shell).single()).clickItem("Veterinarians");

        assertEquals("vets", UI.getCurrent().getInternals().getActiveViewLocation().getPath(),
                "Expected a drawer link to lead away from the error view");
        assertDoesNotThrow(() -> find(VetsView.class).single(),
                "Expected the veterinarians list after leaving the error view");
    }

    private List<Result<?>> snapshot() {
        Result<OwnersRecord> owners = dsl.selectFrom(OWNERS).orderBy(OWNERS.ID).fetch();
        Result<PetsRecord> pets = dsl.selectFrom(PETS).orderBy(PETS.ID).fetch();
        Result<VisitsRecord> visits = dsl.selectFrom(VISITS).orderBy(VISITS.ID).fetch();
        Result<TypesRecord> types = dsl.selectFrom(TYPES).orderBy(TYPES.ID).fetch();
        Result<VetsRecord> vets = dsl.selectFrom(VETS).orderBy(VETS.ID).fetch();
        Result<SpecialtiesRecord> specialties = dsl.selectFrom(SPECIALTIES).orderBy(SPECIALTIES.ID).fetch();
        Result<VetSpecialtiesRecord> assignments = dsl.selectFrom(VET_SPECIALTIES)
                .orderBy(VET_SPECIALTIES.VET_ID, VET_SPECIALTIES.SPECIALTY_ID).fetch();
        return List.of(owners, pets, visits, types, vets, specialties, assignments);
    }
}
