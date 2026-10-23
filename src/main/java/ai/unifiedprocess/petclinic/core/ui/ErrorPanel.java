package ai.unifiedprocess.petclinic.core.ui;

import ai.unifiedprocess.petclinic.welcome.ui.ViewWelcomePageView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

/**
 * Shared content for UC-010 error views. {@link ApplicationErrorView},
 * {@link NotFoundErrorView} and {@link ViewDataUnavailableErrorView} mount one
 * of these so the error flows render identically — only the HTTP status differs.
 *
 * <p>Test assertions on the rendered content use {@code find()} locators
 * scoped to the containing error view, so there's no need to expose
 * fields on this helper.
 */
class ErrorPanel extends VerticalLayout {

    private final Paragraph messagePara;

    ErrorPanel() {
        setPadding(true);
        setSpacing(true);
        addClassNames("error-panel");

        H2 heading = new H2("Something happened...");
        heading.addClassNames("page-heading");

        messagePara = new Paragraph();
        messagePara.addClassNames("secondary-text");

        RouterLink backLink = new RouterLink("Back to Home", ViewWelcomePageView.class);

        add(heading, messagePara, backLink);
    }

    void setMessage(String message) {
        messagePara.setText(message == null ? "" : message);
    }
}
