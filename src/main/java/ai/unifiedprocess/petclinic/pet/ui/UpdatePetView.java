package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.core.ui.ResourceNotFoundException;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import ai.unifiedprocess.petclinic.owner.ui.ViewOwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-008 Update Pet.
 *
 * <p>Route {@code /owners/:ownerId/pets/:petId/edit}. Pre-fills the form
 * with the pet, enforces name uniqueness excluding the pet itself (BR-001),
 * and returns to the Owner Details view on success with the notification
 * "Pet details has been edited".
 */
@Route(value = "owners/:ownerId/pets/:petId/edit", layout = MainLayout.class)
@PageTitle("Edit Pet")
public class UpdatePetView extends VerticalLayout implements BeforeEnterObserver {

    static final String SUCCESS_MESSAGE = "Pet details has been edited";

    private final PetForm petForm;

    private final transient OwnerRepository ownerRepository;
    private final transient PetRepository petRepository;
    private Integer ownerId;
    private Integer petId;

    public UpdatePetView(OwnerRepository ownerRepository, PetRepository petRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;

        setSizeFull();

        H2 heading = new H2("Edit Pet");
        heading.addClassNames("page-heading");

        petForm = new PetForm(petRepository.findAllTypes());

        Button saveButton = new Button("Update Pet", click -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", click -> getUI().ifPresent(ui ->
                ui.navigate(ViewOwnerDetailsView.class, OwnerRouteParameters.forOwner(ownerId))));

        add(heading, petForm, new HorizontalLayout(saveButton, cancelButton));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ownerId = event.getRouteParameters()
                .getInteger(OwnerRouteParameters.OWNER_ID)
                .orElseThrow(ResourceNotFoundException::owner);
        petId = event.getRouteParameters()
                .getInteger(OwnerRouteParameters.PET_ID)
                .orElseThrow(ResourceNotFoundException::pet);

        // A5: owner not found → UC-010 A1
        ownerRepository.findById(ownerId)
                .orElseThrow(ResourceNotFoundException::owner);
        // A6: pet not found, or not owned by this owner → UC-010 A1
        Pet pet = petRepository.findById(petId)
                .orElseThrow(ResourceNotFoundException::pet);
        if (!pet.ownerId().equals(ownerId)) {
            throw ResourceNotFoundException.pet();
        }
        petForm.read(pet);
    }

    private void save() {
        petForm.validateAndRead(petId, ownerId).ifPresent(pet -> {
            // UC-008 BR-001: unique pet name per owner, without comparing the pet being edited against itself.
            if (petRepository.existsByOwnerAndName(ownerId, pet.name(), petId)) {
                petForm.rejectName("already exists");
                return;
            }
            // UC-008 A8: another Clinic User took that name since the check above.
            if (!petRepository.update(pet)) {
                petForm.rejectName("already exists");
                return;
            }
            Notification.show(SUCCESS_MESSAGE);
            getUI().ifPresent(ui -> ui.navigate(
                    ViewOwnerDetailsView.class, OwnerRouteParameters.forOwner(ownerId)));
        });
    }
}
