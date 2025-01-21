package kz.learn.libraryapp.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import kz.learn.libraryapp.entity.AuthorEntity;
import kz.learn.libraryapp.service.LibraryService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;


@Route("authors")
@RequiredArgsConstructor
public class AuthorView extends VerticalLayout {

    private final LibraryService libraryService;
    private final Grid<AuthorEntity> authorGrid = new Grid<>(AuthorEntity.class, false);
    private final TextField nameField = new TextField("Name");
    private final Button addButton = new Button("Add Author");
    private final Button refreshButton = new Button("Refresh");

    @PostConstruct
    public void init() {
        setupGrid();
        setupForm();
        setupListeners();
        loadAuthors();
    }

    private void setupGrid() {
        authorGrid.addColumn(AuthorEntity::getId).setHeader("ID");
        authorGrid.addColumn(AuthorEntity::getName).setHeader("Name");
        add(authorGrid);
    }

    private void setupForm() {
        HorizontalLayout formLayout = new HorizontalLayout(nameField, addButton, refreshButton);
        add(formLayout);
    }

    private void setupListeners() {
        addButton.addClickListener(event -> {
            String name = nameField.getValue();
            if (name == null || name.isEmpty()) {
                Notification.show("Name cannot be empty", 3000, Notification.Position.MIDDLE);
                return;
            }
            addAuthor(name);
        });

        refreshButton.addClickListener(event -> loadAuthors());
    }

    private void loadAuthors() {
        libraryService.getAllAuthors().subscribe(authors -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                authorGrid.setItems(authors);
                Notification.show("Authors loaded successfully");
            }));
        }, error -> {
            getUI().ifPresent(ui -> ui.access(() ->
                    Notification.show("Failed to load authors: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
            ));
        });
    }

    private void addAuthor(String name) {
        libraryService.createAuthor(name).subscribe(author -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Author added successfully: " + author.getName());
                nameField.clear();
                loadAuthors();
            }));
        }, error -> {
            getUI().ifPresent(ui -> ui.access(() ->
                    Notification.show("Failed to add author: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
            ));
        });
    }
}
