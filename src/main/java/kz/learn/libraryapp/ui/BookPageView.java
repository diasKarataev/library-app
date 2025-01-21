package kz.learn.libraryapp.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import kz.learn.libraryapp.entity.AuthorEntity;
import kz.learn.libraryapp.entity.BookEntity;
import kz.learn.libraryapp.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Route("book/:id") // Маршрут с параметром id
@Component
public class BookPageView extends VerticalLayout implements HasUrlParameter<String> {

    private final LibraryService libraryService;

    private final Grid<AuthorEntity> authorGrid = new Grid<>(AuthorEntity.class);
    private final TextField authorNameField = new TextField("Имя автора");
    private UUID bookId;

    public BookPageView(LibraryService libraryService) {
        this.libraryService = libraryService;
        Button addAuthorButton = new Button("Добавить автора", event -> addAuthor());

        authorGrid.setColumns("id", "name");
        add(authorNameField, addAuthorButton, authorGrid);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        try {
            bookId = UUID.fromString(parameter);
            loadBookData(bookId);
        } catch (IllegalArgumentException e) {
            Notification.show("Неверный формат ID книги", 3000, Notification.Position.MIDDLE);
        }
    }

    private void loadBookData(UUID bookId) {
        libraryService.getAllAuthors()
                .flatMap(authors -> {
                    authorGrid.setItems(authors); // Заполнение таблицы авторами
                    return Mono.empty();
                })
                .onErrorResume(error -> {
                    Notification.show("Ошибка при загрузке данных: " + error.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    private void addAuthor() {
        String authorName = authorNameField.getValue();
        if (authorName == null || authorName.isEmpty()) {
            Notification.show("Имя автора не может быть пустым", 3000, Notification.Position.MIDDLE);
            return;
        }

        libraryService.createAuthor(authorName)
                .flatMap(author -> libraryService.addAuthorToBook(bookId, author.getId()))
                .flatMap(book -> {
                    Notification.show("Автор успешно добавлен к книге!", 3000, Notification.Position.MIDDLE);
                    return Mono.empty();
                })
                .onErrorResume(error -> {
                    Notification.show("Ошибка при добавлении автора: " + error.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}