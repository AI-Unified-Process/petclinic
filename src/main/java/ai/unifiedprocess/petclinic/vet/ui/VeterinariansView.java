package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.core.ui.ViewDataUnavailableException;
import ai.unifiedprocess.petclinic.vet.domain.Specialty;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.ErrorHandlerUtil;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.dao.DataAccessException;

/**
 * UC-002 View Veterinarians.
 *
 * <p>Lists the clinic's veterinarians with their specialties in a lazily
 * loaded grid. A fetch that fails after navigation is redirected to the error
 * view as a {@link ViewDataUnavailableException} (process.md, Failure paths).
 */
// UC-002 BR-003: reachable without login; no access annotation (UC-001 BR-001).
@Route(value = "vets", layout = MainLayout.class)
@PageTitle("Veterinarians")
public class VeterinariansView extends VerticalLayout {

    /** Shown in the Specialties column for a vet who holds no specialty. */
    static final String NO_SPECIALTIES = "none";

    private final transient VetRepository vetRepository;

    public VeterinariansView(VetRepository vetRepository) {
        this.vetRepository = vetRepository;

        setSizeFull();

        H2 heading = new H2("Veterinarians");
        heading.addClassNames("page-heading");

        Grid<Vet> grid = new Grid<>(Vet.class, false);
        grid.setId("vets-grid");
        grid.addColumn(Vet::firstName).setHeader("First Name").setKey("firstName");
        grid.addColumn(Vet::lastName).setHeader("Last Name").setKey("lastName");
        grid.addColumn(VeterinariansView::specialties).setHeader("Specialties").setKey("specialties");
        grid.setSizeFull();
        // UC-002 BR-001: rows fetched lazily as the user scrolls, no page controls.
        grid.setItems(this::fetch, this::count);

        add(heading, grid);
    }

    private Stream<Vet> fetch(Query<Vet, Void> query) {
        try {
            return vetRepository.findPage(query.getOffset(), query.getLimit());
        } catch (DataAccessException e) {
            ErrorHandlerUtil.handleErrorByRedirectingToErrorView(new ViewDataUnavailableException(e));
            return Stream.empty();
        }
    }

    private int count(Query<Vet, Void> query) {
        try {
            return vetRepository.count();
        } catch (DataAccessException e) {
            ErrorHandlerUtil.handleErrorByRedirectingToErrorView(new ViewDataUnavailableException(e));
            return 0;
        }
    }

    private static String specialties(Vet vet) {
        if (vet.specialties().isEmpty()) {
            return NO_SPECIALTIES;
        }
        return vet.specialties().stream()
                .map(Specialty::name)
                .collect(Collectors.joining(", "));
    }
}
