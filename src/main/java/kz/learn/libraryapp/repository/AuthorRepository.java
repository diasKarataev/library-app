package kz.learn.libraryapp.repository;

import kz.learn.libraryapp.entity.AuthorEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AuthorRepository extends ReactiveMongoRepository<AuthorEntity, UUID> {
    Mono<AuthorEntity> findByName(String name);

}
