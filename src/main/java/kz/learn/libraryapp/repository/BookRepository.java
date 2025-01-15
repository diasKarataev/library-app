package kz.learn.libraryapp.repository;

import kz.learn.libraryapp.entity.BookEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BookRepository extends ReactiveMongoRepository<BookEntity, UUID> {
    Mono<BookEntity> findByTitle(String title);
}
