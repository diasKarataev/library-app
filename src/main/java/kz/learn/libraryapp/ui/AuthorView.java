package kz.learn.libraryapp.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import kz.learn.libraryapp.entity.AuthorEntity;
import kz.learn.libraryapp.service.LibraryService;
import lombok.RequiredArgsConstructor;


@Route("authors")
@RequiredArgsConstructor
public class AuthorView extends VerticalLayout {

    private final LibraryService libraryService;
    private final Grid<AuthorEntity> authorGrid = new Grid<>(AuthorEntity.class, false);
    private final TextField nameField = new TextField("Name");
    private final TextField filterField = new TextField("Filter by Name"); // Поле для фильтрации
    private final Button filterButton = new Button("Filter"); // Кнопка фильтрации
    private final Button addButton = new Button("Add Author");
    private final Button refreshButton = new Button("Refresh");

    @PostConstruct
    public void init() {
        setupGrid();
        setupForm();
        setupListeners();
        setupFilter();
        Notification.show("Loading authors...", 2000, Notification.Position.MIDDLE);
        loadAuthors();
    }

    private void setupGrid() {
        authorGrid.addComponentColumn(author -> {
            Anchor anchor = new Anchor("author/" + author.getId().toString(), author.getName());
            anchor.getStyle().set("text-decoration", "none");
            anchor.getStyle().set("color", "blue");
            return anchor;
        }).setHeader("Name");

        authorGrid.addComponentColumn(author -> {
            Button deleteButton = new Button("Delete", event -> {
                deleteAuthor(author);
            });
            deleteButton.getStyle().set("color", "red");
            return deleteButton;
        }).setHeader("Actions");

        add(authorGrid);
    }

    private void setupFilter() {
        // Фильтрация авторов
        filterButton.addClickListener(event -> {
            String filterText = filterField.getValue();
            if (filterText == null || filterText.trim().isEmpty()) {
                Notification.show("Please enter a name to filter", 3000, Notification.Position.MIDDLE);
                loadAuthors(); // Если поле фильтра пустое, загружаем всех авторов
                return;
            }

            filterAuthors(filterText.trim());
        });

        HorizontalLayout filterLayout = new HorizontalLayout(filterField, filterButton);
        add(filterLayout);
    }

    private void filterAuthors(String name) {
        libraryService.getAllAuthors()
                .subscribe(authors -> {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        var filteredAuthors = authors.stream()
                                .filter(author -> author.getName() != null && author.getName().toLowerCase().contains(name.toLowerCase()))
                                .toList();
                        if (filteredAuthors.isEmpty()) {
                            Notification.show("No authors found for the name: " + name, 3000, Notification.Position.MIDDLE);
                            authorGrid.setItems(); // Очищаем таблицу
                        } else {
                            authorGrid.setItems(filteredAuthors);
                        }
                    }));
                }, error -> {
                    getUI().ifPresent(ui -> ui.access(() ->
                            Notification.show("Failed to filter authors: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
                    ));
                });
    }

    private void deleteAuthor(AuthorEntity author) {
        libraryService.deleteAuthor(author.getId()).subscribe(i -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Author deleted successfully: " + author.getName(), 3000, Notification.Position.MIDDLE);
                loadAuthors();
            }));
        }, error -> {
            getUI().ifPresent(ui -> ui.access(() ->
                    Notification.show("Failed to delete author: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
            ));
        });
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
