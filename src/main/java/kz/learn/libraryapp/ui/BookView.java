package kz.learn.libraryapp.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.PostConstruct;
import kz.learn.libraryapp.entity.AuthorEntity;
import kz.learn.libraryapp.entity.BookEntity;
import kz.learn.libraryapp.repository.AuthorRepository;
import kz.learn.libraryapp.service.LibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Route("books")
@Slf4j
@RequiredArgsConstructor
public class BookView extends VerticalLayout {

    private final LibraryService libraryService;
    private final Grid<BookEntity> bookGrid = new Grid<>(BookEntity.class, false);
    private final AuthorRepository authorRepository;

    private final TextField filterField = new TextField("Filter by Title"); // Поле для фильтрации

    @PostConstruct
    public void init() {
        setupFilter();
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

    private void setupFilter() {
        // Добавляем поле фильтрации и кнопку
        Button filterButton = new Button("Apply Filter", event -> applyFilter());
        filterButton.getStyle().set("margin-left", "10px");

        HorizontalLayout filterLayout = new HorizontalLayout(filterField, filterButton);
        add(filterLayout);
    }

    private void applyFilter() {
        String filterText = filterField.getValue();
        if (filterText == null || filterText.trim().isEmpty()) {
            // Если фильтр пустой, загружаем все книги
            loadBooks();
        } else {
            // Фильтруем книги по названию
            libraryService.getAllBooks()
                    .subscribe(books -> {
                        getUI().ifPresent(ui -> ui.access(() -> {
                            var filteredBooks = books.stream()
                                    .filter(book -> book.getTitle() != null && book.getTitle().toLowerCase().contains(filterText.toLowerCase()))
                                    .toList();
                            if (filteredBooks.isEmpty()) {
                                Notification.show("No books match the filter.", 3000, Notification.Position.MIDDLE);
                            }
                            bookGrid.setItems(filteredBooks);
                        }));
                    });
        }
    }

    private void setupListeners() {
        Button openAddDialogButton = new Button("Add Book", event -> openAddBookDialog());
        Button refreshButton = new Button("Refresh", event -> loadBooks());
        Button exportButton = new Button("Export to Excel", event -> openDateRangeDialog());
        HorizontalLayout buttonsLayout = new HorizontalLayout(openAddDialogButton, exportButton);
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


    private void openDateRangeDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Select Date Range");

        DatePicker startDatePicker = new DatePicker("Date From");
        DatePicker endDatePicker = new DatePicker("Date To");
        endDatePicker.setValue(LocalDate.now()); // По умолчанию сегодняшняя дата

        Button exportButton = new Button("Export", event -> {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate == null || endDate == null) {
                Notification.show("Please select both dates", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (endDate.isBefore(startDate)) {
                Notification.show("End date must be after start date", 3000, Notification.Position.MIDDLE);
                return;
            }

            StreamResource resource = createExcelResource(startDate, endDate);
            Anchor downloadLink = new Anchor(resource, "Download Excel");
            downloadLink.getElement().setAttribute("download", true);
            add(downloadLink);

            Notification.show("Excel file created. Click the download link.", 3000, Notification.Position.MIDDLE);
            dialog.close();
        });

        Button cancelButton = new Button("Cancel", event -> dialog.close());
        HorizontalLayout buttonLayout = new HorizontalLayout(exportButton, cancelButton);

        VerticalLayout dialogLayout = new VerticalLayout(startDatePicker, endDatePicker, buttonLayout);
        dialog.add(dialogLayout);
        dialog.open();
    }

    private StreamResource createExcelResource(LocalDate startDate, LocalDate endDate) {
        return new StreamResource("books.xlsx", () -> {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Books");

                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("Title");
                headerRow.createCell(2).setCellValue("Added Date");

                List<BookEntity> books = libraryService.getAllBooks()
                        .block()
                        .stream()
                        .filter(book -> {
                            LocalDate addedDate = book.getAddedDate();
                            return addedDate != null && !addedDate.isBefore(startDate) && !addedDate.isAfter(endDate);
                        })
                        .collect(Collectors.toList());

                int rowNum = 1;
                for (BookEntity book : books) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(book.getId().toString());
                    row.createCell(1).setCellValue(book.getTitle());
                    row.createCell(2).setCellValue(book.getAddedDate().toString());
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                return new ByteArrayInputStream(outputStream.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Error while creating Excel file", e);
            }
        });
    }
}
