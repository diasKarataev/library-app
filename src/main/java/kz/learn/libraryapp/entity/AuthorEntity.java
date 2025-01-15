package kz.learn.libraryapp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document(collection = "authors")
@Data
@NoArgsConstructor
public class AuthorEntity {
    @Id
    private UUID id;
    private String name;
}

