package kz.learn.libraryapp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document(collection = "books")
@Data
@NoArgsConstructor
public class BookEntity {
    @Id
    private UUID id;
    private String title;
    private List<UUID> authorIds;
}

