package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.core.ui.ViewDataUnavailableException;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.ErrorHandlerUtil;
import org.springframework.dao.DataAccessException;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * UC-002: View Veterinarians.
 *
 * <p>Lazy-loaded list of vets with their specialties. The grid uses a
 * callback data provider with a count (UC-002 BR-001: rows fetched while
 * scrolling, no page controls, no fixed page size).
 */
// UC-002 BR-003: no access annotation and no security; any visitor may open the list.
@Route(value = "vets", layout = MainLayout.class)
@PageTitle("Veterinarians")
public class VetsView extends VerticalLayout {

    private final transient VetRepository vetRepository;

    public VetsView(VetRepository vetRepository) {
        this.vetRepository = vetRepository;
        setSizeFull();

        H2 heading = new H2("Veterinarians");
        heading.addClassNames("page-heading");

        Grid<Vet> vetsGrid = new Grid<>(Vet.class, false);
        vetsGrid.addColumn(Vet::firstName).setHeader("First Name").setAutoWidth(true);
        vetsGrid.addColumn(Vet::lastName).setHeader("Last Name").setAutoWidth(true);
        vetsGrid.addColumn(Vet::specialtiesLabel).setHeader("Specialties").setAutoWidth(true);
        vetsGrid.setSizeFull();
        // UC-002 BR-001: the grid fetches vets chunk by chunk while scrolling, never all at once;
        // the count only sizes the scrollbar, it loads no rows.
        vetsGrid.setItems(this::page, this::count);

        add(heading, vetsGrid);
        setFlexGrow(1, vetsGrid);
    }

    /** One scroll step of the grid; reads the field, not the constructor parameter. */
    private Stream<Vet> page(Query<Vet, Void> query) {
        return read(() -> vetRepository.findPage(query.getOffset(), query.getLimit()), Stream.empty());
    }

    /** The total number of vets, so the grid knows its size before it is scrolled. */
    private int count(Query<Vet, Void> query) {
        return read(vetRepository::count, 0);
    }

    /**
     * The grid reads after navigation has finished, where the router no longer
     * turns an exception into the error view, so a data-access error is
     * redirected to it here (UC-010 A2).
     */
    private <T> T read(Supplier<T> query, T whenRedirected) {
        try {
            return query.get();
        } catch (DataAccessException e) {
            if (!ErrorHandlerUtil.handleErrorByRedirectingToErrorView(
                    new ViewDataUnavailableException(e.getMessage(), e))) {
                throw e;
            }
            return whenRedirected;
        }
    }
}
