package ai.unifiedprocess.petclinic.welcome.ui;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UC-001: View Welcome Page.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC001ViewWelcomePageTest extends SpringBrowserlessTest {

    @Test
    @UseCase(id = "UC-001", businessRules = "BR-001")
    void welcomePageRendersAtRootRoute() {
        // No authentication is performed, covering BR-001 (Anonymous Access).
        assertDoesNotThrow(() -> navigate(ViewWelcomePageView.class),
                "Expected root route '/' to resolve to ViewWelcomePageView without error");
    }

    @Test
    @UseCase(id = "UC-001")
    void welcomePageShowsDecorativeImage() {
        navigate(ViewWelcomePageView.class);

        assertDoesNotThrow(() -> find(Image.class)
                .withPropertyValue(Image::getSrc, "images/pets.png")
                .single(),
                "Expected exactly one decorative image with src 'images/pets.png'");
    }

    @Test
    @UseCase(id = "UC-001")
    void clinicLogoIsRendered() {
        navigate(ViewWelcomePageView.class);

        assertDoesNotThrow(() -> find(Image.class)
                .withPropertyValue(Image::getSrc, "images/petclinic-logo.svg")
                .single(),
                "Expected exactly one clinic logo image with src 'images/petclinic-logo.svg'");
    }

    @Test
    @UseCase(id = "UC-001")
    void unifiedProcessLinkIsRendered() {
        navigate(ViewWelcomePageView.class);

        Anchor link = assertDoesNotThrow(() -> find(Anchor.class)
                .withPropertyValue(Anchor::getHref, "https://unifiedprocess.ai")
                .single(),
                "Expected a link to https://unifiedprocess.ai");
        assertEquals("https://unifiedprocess.ai", link.getText());
    }

    @Test
    @UseCase(id = "UC-001")
    void mainNavigationMenuHasRequiredItems() {
        navigate(ViewWelcomePageView.class);

        assertDoesNotThrow(() -> find(SideNavItem.class)
                .withPropertyValue(SideNavItem::getLabel, "Home").single(),
                "Missing Home navigation item");
        assertDoesNotThrow(() -> find(SideNavItem.class)
                .withPropertyValue(SideNavItem::getLabel, "Find Owners").single(),
                "Missing Find Owners navigation item");
        assertDoesNotThrow(() -> find(SideNavItem.class)
                .withPropertyValue(SideNavItem::getLabel, "Veterinarians").single(),
                "Missing Veterinarians navigation item");
        assertDoesNotThrow(() -> find(SideNavItem.class)
                .withPropertyValue(SideNavItem::getLabel, "Error").single(),
                "Missing Error navigation item");
    }
}
