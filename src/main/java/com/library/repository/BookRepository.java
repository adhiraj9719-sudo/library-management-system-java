package com.library.repository;

import com.library.model.Book;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    List<Book> findAllByOrderByTitleAsc();

    List<Book> findByTitleContainingIgnoreCaseOrderByTitleAsc(String title);

    List<Book> findByAuthorContainingIgnoreCaseOrderByTitleAsc(String author);

    List<Book> findByGenreContainingIgnoreCaseOrderByTitleAsc(String genre);

    @Query("select b from Book b where lower(b.title) like lower(concat('%', :q, '%')) "
            + "or lower(b.author) like lower(concat('%', :q, '%')) "
            + "or lower(b.genre) like lower(concat('%', :q, '%')) "
            + "order by lower(b.title)")
    List<Book> searchAll(@Param("q") String q);
}
