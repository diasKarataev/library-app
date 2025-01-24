package kz.learn.libraryapp.service;

import kz.learn.libraryapp.entity.AuthorEntity;
import kz.learn.libraryapp.entity.BookEntity;
import kz.learn.libraryapp.repository.AuthorRepository;
import kz.learn.libraryapp.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public Mono<AuthorEntity> createAuthor(String name) {
        AuthorEntity author = new AuthorEntity();
        author.setId(UUID.randomUUID());
        author.setName(name);
        return authorRepository.save(author);
    }

    public Mono<BookEntity> createBook(String title) {
        BookEntity book = new BookEntity();
        book.setId(UUID.randomUUID());
        book.setAddedDate(LocalDate.now());
        book.setTitle(title);
        return bookRepository.save(book);
    }

    public Mono<BookEntity> getBookByTitle(String title) {
        return bookRepository.findByTitle(title);
    }

    public Mono<BookEntity> getBookById(UUID uuid) {
        return bookRepository.findById(uuid);
    }

    public Mono<AuthorEntity> getAuthorById(UUID bookId) { return authorRepository.findById(bookId); }

    public Mono<AuthorEntity> getAuthorByName(String name) {
        return authorRepository.findByName(name);
    }

    public Mono<BookEntity> addAuthorToBook(UUID bookId, UUID authorId) {
        return bookRepository.findById(bookId)
                .flatMap(book -> {

                    List<UUID> authors = book.getAuthorIds();
                    if(authors == null){
                        authors = new ArrayList<>();
                    }
                    authors.add(authorId);

                    book.setAuthorIds(authors);

                    return bookRepository.save(book);
                });
    }

    public Mono<List<BookEntity>> getAllBooks() {
        return bookRepository.findAll()
                .collectList();
    }

    public Mono<List<AuthorEntity>> getAllAuthors() {
        return authorRepository.findAll()
                .collectList();
    }

    public Mono<BookEntity> removeAuthorFromBook(UUID bookId, UUID authorId) {
        return bookRepository.findById(bookId)
                .flatMap(book -> {
                    book.getAuthorIds().remove(authorId);
                    return bookRepository.save(book);
                });
    }

    public Mono<Void> deleteBook(UUID bookId) {
        return bookRepository.findById(bookId)
                .flatMap(bookRepository::delete);
    }

    public Mono<Void> deleteAuthor(UUID authorId) {
        return bookRepository.findAll()
                .filter(book -> book.getAuthorIds() != null && book.getAuthorIds().contains(authorId))
                .collectList()
                .flatMap(books -> {
                    if (!books.isEmpty()) {
                        return Flux.fromIterable(books)
                                .flatMap(book -> {
                                    book.getAuthorIds().remove(authorId);
                                    return bookRepository.save(book);
                                })
                                .then(authorRepository.deleteById(authorId));
                    } else {
                        return authorRepository.deleteById(authorId);
                    }
                });
    }




}
