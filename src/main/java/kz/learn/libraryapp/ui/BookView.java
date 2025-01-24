package kz.learn.libraryapp.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import kz.learn.libraryapp.entity.AuthorEntity;
import kz.learn.libraryapp.entity.BookEntity;
import kz.learn.libraryapp.repository.AuthorRepository;
import kz.learn.libraryapp.service.LibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Route("books")
@Slf4j
@RequiredArgsConstructor
public class BookView extends VerticalLayout {

    private final LibraryService libraryService;
    private final Grid<BookEntity> bookGrid = new Grid<>(BookEntity.class, false);
    private final AuthorRepository authorRepository;

    @PostConstruct
    public void init() {
        setupGrid();
        setupListeners();
        Notification.show("Loading books...", 2000, Notification.Position.MIDDLE);
        loadBooks();
    }

    private void setupGrid() {
        bookGrid.addComponentColumn(book -> {
            Anchor anchor = new Anchor("/book/" + book.getId().toString(), book.getTitle());
            anchor.getStyle().set("text-decoration", "none");
            anchor.getStyle().set("color", "blue");
            return anchor;
        }).setHeader("Title");

        bookGrid.addColumn(book -> {
            if (book.getAuthorIds() == null || book.getAuthorIds().isEmpty()) {
                return "No authors";
            }
            return book.getAuthorIds().stream()
                    .map(authorId -> authorRepository.findById(authorId)
                            .map(AuthorEntity::getName)
                            .defaultIfEmpty("Unknown Author")
                            .block())
                    .filter(name -> name != null)
                    .reduce((name1, name2) -> name1 + ", " + name2)
                    .orElse("No authors");
        }).setHeader("Authors");

        bookGrid.addComponentColumn(book -> {
            Button deleteButton = new Button("Delete", event -> {
                deleteBook(book);
            });
            deleteButton.getStyle().set("color", "red");
            return deleteButton;
        }).setHeader("Actions");

        add(bookGrid);
    }

    private void setupListeners() {
        Button openAddDialogButton = new Button("Add Book", event -> openAddBookDialog());
        Button refreshButton = new Button("Refresh", event -> loadBooks());
        HorizontalLayout buttonsLayout = new HorizontalLayout(openAddDialogButton, refreshButton);
        add(buttonsLayout);
    }

    private void loadBooks() {
        libraryService.getAllBooks()
                .subscribe(books -> {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        if (books.isEmpty()) {
                            Notification.show("No books found.", 3000, Notification.Position.MIDDLE);
                        } else {
                            bookGrid.setItems(books);
                        }
                    }));
                });
    }

    private void openAddBookDialog() {
        Dialog dialog = new Dialog();

        TextField titleField = new TextField("Title");

        Button addButton = new Button("Add", event -> {
            String title = titleField.getValue();
            if (title == null || title.isEmpty()) {
                Notification.show("Title cannot be empty", 3000, Notification.Position.MIDDLE);
                return;
            }
            libraryService.createBook(title).subscribe(book -> {
                getUI().ifPresent(ui -> ui.access(() -> {
                    Notification.show("Book added successfully: " + book.getTitle());
                    titleField.clear();
                    dialog.close();
                    loadBooks();
                }));
            }, error -> {
                getUI().ifPresent(ui -> ui.access(() ->
                        Notification.show("Failed to add book: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
                ));
            });
        });

        Button cancelButton = new Button("Cancel", event -> dialog.close());

        HorizontalLayout buttonsLayout = new HorizontalLayout(addButton, cancelButton);

        VerticalLayout dialogLayout = new VerticalLayout(titleField, buttonsLayout);
        dialog.add(dialogLayout);

        dialog.open();
    }

    private void deleteBook(BookEntity book) {
        libraryService.deleteBook(book.getId()).subscribe(i -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Book deleted successfully: " + book.getTitle(), 3000, Notification.Position.MIDDLE);
                loadBooks();
            }));
        }, error -> {
            getUI().ifPresent(ui -> ui.access(() ->
                    Notification.show("Failed to delete book: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
            ));
        });
    }
}
