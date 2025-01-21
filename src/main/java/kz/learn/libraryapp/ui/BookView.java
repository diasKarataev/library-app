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
import kz.learn.libraryapp.entity.BookEntity;
import kz.learn.libraryapp.repository.AuthorRepository;
import kz.learn.libraryapp.service.LibraryService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Route("books")
@RequiredArgsConstructor
public class BookView extends VerticalLayout {

    private final LibraryService libraryService;
    private final Grid<BookEntity> bookGrid = new Grid<>(BookEntity.class, false);
    private final TextField titleField = new TextField("Title");
    private final TextField authorField = new TextField("Author Name");
    private final Button addButton = new Button("Add Book");
    private final Button searchButton = new Button("Search by Author");
    private final Button refreshButton = new Button("Refresh");
    private final AuthorRepository authorRepository;

    @PostConstruct
    public void init() {
        setupGrid();
        setupForm();
        setupListeners();
        loadBooks();
    }

    private void setupGrid() {
        bookGrid.addColumn(BookEntity::getTitle).setHeader("Title");

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

        add(bookGrid);
    }

    private void setupForm() {
        HorizontalLayout formLayout = new HorizontalLayout(titleField, addButton, refreshButton, authorField, searchButton);
        add(formLayout);
    }

    private void setupListeners() {
        addButton.addClickListener(event -> {
            String title = titleField.getValue();
            if (title == null || title.isEmpty()) {
                Notification.show("Title cannot be empty", 3000, Notification.Position.MIDDLE);
                return;
            }
            addBook(title);
        });

        refreshButton.addClickListener(event -> loadBooks());

        searchButton.addClickListener(event -> {
            String authorName = authorField.getValue();
            if (authorName == null || authorName.isEmpty()) {
                Notification.show("Author name cannot be empty", 3000, Notification.Position.MIDDLE);
                return;
            }
            searchBooksByAuthor(authorName);
        });
    }

    private void loadBooks() {
        libraryService.getAllBooks().subscribe(books -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                bookGrid.setItems(books);
            }));
        }, error -> {
            getUI().ifPresent(ui -> ui.access(() ->
                    Notification.show("Failed to load books: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
            ));
        });
    }

    private void addBook(String title) {
        libraryService.createBook(title).subscribe(book -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Book added successfully: " + book.getTitle());
                titleField.clear();
                loadBooks(); // Обновляем список после добавления
            }));
        }, error -> {
            getUI().ifPresent(ui -> ui.access(() ->
                    Notification.show("Failed to add book: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
            ));
        });
    }

    private void searchBooksByAuthor(String authorName) {
        libraryService.getAuthorByName(authorName).flatMapMany(author -> {
            UUID authorId = author.getId();
            return libraryService.getAllBooks().map(books ->
                    books.stream().filter(book -> book.getAuthorIds() != null && book.getAuthorIds().contains(authorId)).toList()
            );
        }).subscribe(books -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                bookGrid.setItems(books);
                if (books.isEmpty()) {
                    Notification.show("No books found for author: " + authorName, 3000, Notification.Position.MIDDLE);
                }
            }));
        }, error -> {
            getUI().ifPresent(ui -> ui.access(() ->
                    Notification.show("Failed to search books: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
            ));
        });
    }
}