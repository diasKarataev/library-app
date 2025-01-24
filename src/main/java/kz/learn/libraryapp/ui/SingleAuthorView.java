package kz.learn.libraryapp.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import kz.learn.libraryapp.entity.BookEntity;
import kz.learn.libraryapp.service.LibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Route("author")
@Slf4j
@RequiredArgsConstructor
public class SingleAuthorView extends VerticalLayout implements HasUrlParameter<String> {

    private final LibraryService libraryService;

    private final Grid<BookEntity> bookGrid = new Grid<>(BookEntity.class, false);

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        Span statusSpan = new Span("Loading author details...");
        add(statusSpan, bookGrid);

        try {
            UUID authorId = UUID.fromString(parameter);
            log.info("Requested author ID: {}", authorId);
            Notification.show("Loading author...", 2000, Notification.Position.MIDDLE);

            libraryService.getAuthorById(authorId).subscribe(author -> {
                getUI().ifPresent(ui -> ui.access(() -> {
                    if (author != null) {
                        statusSpan.setText("Name: " + author.getName());
                        loadBooksByAuthor(authorId);
                    } else {
                        statusSpan.setText("Author not found.");
                        Notification.show("Author not found.", 2000, Notification.Position.MIDDLE);
                    }
                }));
            }, error -> {
                log.error("Error loading author: {}", error.getMessage());
                getUI().ifPresent(ui -> ui.access(() -> {
                    statusSpan.setText("Error loading author: " + error.getMessage());
                    Notification.show("Error loading author.", 2000, Notification.Position.MIDDLE);
                }));
            });

        } catch (IllegalArgumentException e) {
            log.error("Invalid author ID: {}", parameter);
            statusSpan.setText("Invalid author ID: " + parameter);
            Notification.show("Invalid author ID.", 2000, Notification.Position.MIDDLE);
        }
    }

    private void loadBooksByAuthor(UUID authorId) {
        libraryService.getAllBooks().subscribe(books -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                var booksByAuthor = books.stream()
                        .filter(book -> book.getAuthorIds() != null && book.getAuthorIds().contains(authorId))
                        .toList();
                if (booksByAuthor.isEmpty()) {
                    Notification.show("No books found for this author.", 3000, Notification.Position.MIDDLE);
                } else {
                    bookGrid.setItems(booksByAuthor);
                    setupGrid();
                }
            }));
        });
    }

    private void setupGrid() {
        // Настраиваем колонки таблицы
        if (bookGrid.getColumns().isEmpty()) {
            bookGrid.addColumn(BookEntity::getTitle).setHeader("Title");
            bookGrid.addColumn(BookEntity::getAddedDate).setHeader("Added Date");
        }
    }
}
