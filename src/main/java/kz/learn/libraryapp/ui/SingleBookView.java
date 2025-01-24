package kz.learn.libraryapp.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import kz.learn.libraryapp.entity.AuthorEntity;
import kz.learn.libraryapp.entity.BookEntity;
import kz.learn.libraryapp.service.LibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Route("book")
@Slf4j
@RequiredArgsConstructor
public class SingleBookView extends VerticalLayout implements HasUrlParameter<String> {

    private final LibraryService libraryService;

    private UUID currentBookId;
    private final Grid<AuthorEntity> authorGrid = new Grid<>(AuthorEntity.class, false);

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        Span statusSpan = new Span("Loading book details...");
        add(statusSpan, authorGrid);

        try {
            currentBookId = UUID.fromString(parameter);
            log.info("Requested book ID: {}", currentBookId);
            Notification.show("Loading book...", 2000, Notification.Position.MIDDLE);

            libraryService.getBookById(currentBookId).subscribe(book -> {
                getUI().ifPresent(ui -> ui.access(() -> {
                    if (book != null) {
                        statusSpan.setText("Title: " + book.getTitle());
                        add(new Span("Added Date: " + book.getAddedDate()));

                        loadAuthorsByBook(book);
                    } else {
                        statusSpan.setText("Book not found.");
                        Notification.show("Book not found.", 2000, Notification.Position.MIDDLE);
                    }
                }));
            }, error -> {
                log.error("Error loading book: {}", error.getMessage());
                getUI().ifPresent(ui -> ui.access(() -> {
                    statusSpan.setText("Error loading book: " + error.getMessage());
                    Notification.show("Error loading book.", 2000, Notification.Position.MIDDLE);
                }));
            });

        } catch (IllegalArgumentException e) {
            log.error("Invalid book ID: {}", parameter);
            statusSpan.setText("Invalid book ID: " + parameter);
            Notification.show("Invalid book ID.", 2000, Notification.Position.MIDDLE);
        }
    }

    private void loadAuthorsByBook(BookEntity book) {
        if (book.getAuthorIds() == null || book.getAuthorIds().isEmpty()) {
            Notification.show("No authors found for this book.", 3000, Notification.Position.MIDDLE);
            return;
        }

        libraryService.getAllAuthors().subscribe(allAuthors -> {
            var bookAuthors = allAuthors.stream()
                    .filter(author -> book.getAuthorIds().contains(author.getId()))
                    .toList();

            getUI().ifPresent(ui -> ui.access(() -> {
                if (bookAuthors.isEmpty()) {
                    Notification.show("No authors found for this book.", 3000, Notification.Position.MIDDLE);
                } else {
                    authorGrid.setItems(bookAuthors);
                    setupAuthorGrid();
                }
            }));
        });
    }

    private void setupAuthorGrid() {
        if (authorGrid.getColumns().isEmpty()) {
            authorGrid.addColumn(AuthorEntity::getName).setHeader("Name");

            authorGrid.addComponentColumn(author -> {
                Button removeButton = new Button("Remove", event -> {
                    removeAuthorFromBook(author);
                });
                removeButton.getStyle().set("color", "red");
                return removeButton;
            }).setHeader("Actions");
        }
    }

    private void removeAuthorFromBook(AuthorEntity author) {
        libraryService.removeAuthorFromBook(currentBookId, author.getId()).subscribe(book -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Author removed successfully: " + author.getName(), 2000, Notification.Position.MIDDLE);
                loadAuthorsByBook(book);
            }));
        }, error -> {
            getUI().ifPresent(ui -> ui.access(() ->
                    Notification.show("Failed to remove author: " + error.getMessage(), 3000, Notification.Position.MIDDLE)
            ));
        });
    }
}
