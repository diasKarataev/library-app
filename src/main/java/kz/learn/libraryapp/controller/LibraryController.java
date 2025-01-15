package kz.learn.libraryapp.controller;

import kz.learn.libraryapp.entity.AuthorEntity;
import kz.learn.libraryapp.entity.BookEntity;
import kz.learn.libraryapp.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
public class LibraryController {
    private final LibraryService libraryService;

    @GetMapping("/books")
    public Mono<List<BookEntity>> getAllBooks() {
        return libraryService.getAllBooks();
    }

    @GetMapping("/authors")
    public Mono<List<AuthorEntity>> getAllAuthors() {
        return libraryService.getAllAuthors();
    }

    @PostMapping("/authors")
    public Mono<AuthorEntity> createAuthor(@RequestParam String name) {
        return libraryService.createAuthor(name);
    }

    @PostMapping("/books")
    public Mono<BookEntity> createBook(@RequestParam String title) {
        return libraryService.createBook(title);
    }

    @GetMapping("/books/{title}")
    public Mono<BookEntity> getBookByTitle(@PathVariable String title) {
        return libraryService.getBookByTitle(title);
    }

    @GetMapping("/authors/{name}")
    public Mono<AuthorEntity> getAuthorByName(@PathVariable String name) {
        return libraryService.getAuthorByName(name);
    }

    @PutMapping("/books/{bookId}/author")
    public Mono<BookEntity> addAuthorToBook(@PathVariable UUID bookId, @RequestParam UUID authorId) {
        return libraryService.addAuthorToBook(bookId, authorId);
    }


    @PutMapping("/books/{bookId}/remove-author/{authorId}")
    public Mono<BookEntity> removeAuthorFromBook(@PathVariable UUID bookId, @PathVariable UUID authorId) {
        return libraryService.removeAuthorFromBook(bookId, authorId);
    }

    @DeleteMapping("/books/{bookId}")
    public Mono<Void> deleteBook(@PathVariable UUID bookId) {
        return libraryService.deleteBook(bookId);
    }

    @DeleteMapping("/authors/{authorId}")
    public Mono<Void> deleteAuthor(@PathVariable UUID authorId) {
        return libraryService.deleteAuthor(authorId);
    }

}
