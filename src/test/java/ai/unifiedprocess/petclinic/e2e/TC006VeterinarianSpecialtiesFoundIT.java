package ai.unifiedprocess.petclinic.e2e;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.TestCase;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.SideNavigationElement;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executes test case TC-006 (docs/test_cases/TC-006-veterinarian-specialties-found.md),
 * the single path of the Veterinarian Lookup process
 * (docs/processes/veterinarian-lookup.bpmn): an anonymous visitor opens the
 * welcome page (UC-001) and browses the veterinarians (UC-002).
 *
 * <p>Read-only: the journey creates and changes nothing, so there is no cleanup.
 */
@TestCase(id = "TC-006", useCases = {"UC-001", "UC-002"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TC006VeterinarianSpecialtiesFoundIT extends AbstractBasePlaywrightIT {

    /** Accessible name of the drawer's navigation landmark. */
    private static final String NAV_LABEL = "Main menu";

    /** Column of the veterinarians grid holding the last name. */
    private static final int LAST_NAME_COLUMN = 1;

    @LocalServerPort
    private int port;

    @Override
    public String getUrl() {
        return "http://localhost:%d/".formatted(port);
    }

    @Override
    public String getView() {
        // Route of Flow step 1: the application's start page.
        return "";
    }

    @Test
    @DisplayName("TC-006: A visitor finds which veterinarian holds which specialty")
    void veterinarianSpecialtiesFound() {
        // Step 1: Open welcome page (UC-001)
        openWelcomePage();

        // Step 2: Verify navigation offered
        verifyNavigationOffered();

        // Step 3: View veterinarians (UC-002)
        viewVeterinarians();

        // Step 4: Verify specialties shown
        verifySpecialtiesShown();
    }

    // --- Flow steps ----------------------------------------------------------

    /** Step 1: the start page renders with the clinic logo and the decorative image. */
    private void openWelcomePage() {
        assertThat(page.getByRole(AriaRole.IMG,
                new Page.GetByRoleOptions().setName("PetClinic logo"))).isVisible();
        assertThat(page.getByRole(AriaRole.IMG,
                new Page.GetByRoleOptions().setName("Pets at the clinic"))).isVisible();
    }

    /** Step 2: the navigation offers Home, Find Owners, Veterinarians and Error. */
    private void verifyNavigationOffered() {
        SideNavigationElement mainMenu = SideNavigationElement.getByLabel(page, NAV_LABEL);
        mainMenu.assertVisible();
        for (String item : List.of("Home", "Find Owners", "Veterinarians", "Error")) {
            mainMenu.getItem(item).assertVisible();
        }
    }

    /** Step 3: follow the "Veterinarians" link. */
    private void viewVeterinarians() {
        SideNavigationElement.getByLabel(page, NAV_LABEL).clickItem("Veterinarians");
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Veterinarians").setExact(true))).isVisible();
    }

    /** Step 4 and Validations 1-2: all six vets, each with their specialties. */
    private void verifySpecialtiesShown() {
        GridElement vets = GridElement.get(page);
        vets.assertVisible();
        vets.waitForGridToStopLoading();

        assertEquals(6, vets.getTotalRowCount(), "All six seeded veterinarians are listed");
        assertSpecialties(vets, "Linda", "Douglas", "dentistry, surgery");
        assertSpecialties(vets, "Helen", "Leary", "radiology");
        assertSpecialties(vets, "James", "Carter", "none");
    }

    private void assertSpecialties(GridElement vets, String firstName, String lastName, String specialties) {
        List<Integer> rows = vets.findRowIndexesWithColumnText(LAST_NAME_COLUMN, lastName);
        assertEquals(1, rows.size(), () -> "Expected one veterinarian named " + lastName);
        GridElement.CellElement first = vets.findCell(rows.getFirst(), "First Name")
                .orElseThrow(() -> new AssertionError("No First Name cell for " + lastName));
        assertThat(first.getCellContentLocator()).hasText(firstName);
        GridElement.CellElement cell = vets.findCell(rows.getFirst(), "Specialties")
                .orElseThrow(() -> new AssertionError("No Specialties cell for " + lastName));
        assertThat(cell.getCellContentLocator()).hasText(specialties);
    }
}
