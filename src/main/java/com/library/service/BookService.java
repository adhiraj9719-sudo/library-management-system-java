package com.library.service;

import com.library.dto.BookForm;
import com.library.dto.BookView;
import com.library.model.Book;
import com.library.model.NotificationKind;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private static final Pattern ISBN = Pattern.compile("\\d{13}|\\d{9}[\\dX]");

    private final BookRepository books;
    private final LoanRepository loans;
    private final NotificationService notifications;

    public BookService(BookRepository books, LoanRepository loans, NotificationService notifications) {
        this.books = books;
        this.loans = loans;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<BookView> search(String query, String field) {
        List<Book> found;
        if (query == null || query.isBlank()) {
            found = books.findAllByOrderByTitleAsc();
        } else {
            String q = query.trim();
            String f = field == null ? "all" : field;
            found = switch (f) {
                case "title" -> books.findByTitleContainingIgnoreCaseOrderByTitleAsc(q);
                case "author" -> books.findByAuthorContainingIgnoreCaseOrderByTitleAsc(q);
                case "genre" -> books.findByGenreContainingIgnoreCaseOrderByTitleAsc(q);
                default -> books.searchAll(q);
            };
        }
        Map<Long, Long> active = new HashMap<>();
        for (Object[] row : loans.activeCountsByBook()) {
            active.put((Long) row[0], (Long) row[1]);
        }
        return found.stream().map(b -> new BookView(b, active.getOrDefault(b.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public List<BookView> all() {
        return search(null, null);
    }

    @Transactional(readOnly = true)
    public Book get(Long id) {
        return books.findById(id).orElseThrow(() -> new LibraryException("That book does not exist."));
    }

    @Transactional(readOnly = true)
    public long onLoan(Long bookId) {
        return loans.countByBookIdAndReturnDateIsNull(bookId);
    }

    @Transactional
    public Book create(BookForm form) {
        int copies = validate(form, 1);
        String isbn = normaliseIsbn(form.getIsbn());
        if (books.findByIsbn(isbn).isPresent()) {
            throw new LibraryException("A book with that ISBN is already in the catalog.");
        }
        Book book = new Book();
        apply(book, form, isbn, copies);
        books.save(book);
        notifications.sendToAllMembers(NotificationKind.NEW_ARRIVAL,
                "New arrival: “" + book.getTitle() + "” by " + book.getAuthor() + ".");
        return book;
    }

    @Transactional
    public Book update(Long id, BookForm form) {
        Book book = get(id);
        int copies = validate(form, (int) onLoan(id));
        String isbn = normaliseIsbn(form.getIsbn());
        books.findByIsbn(isbn).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new LibraryException("Another book already uses that ISBN.");
            }
        });
        apply(book, form, isbn, copies);
        return books.save(book);
    }

    @Transactional
    public String delete(Long id) {
        Book book = get(id);
        if (onLoan(id) > 0) {
            throw new LibraryException("“" + book.getTitle()
                    + "” is on loan. Wait for it to be returned before deleting.");
        }
        loans.deleteByBookId(id);
        books.delete(book);
        return book.getTitle();
    }

    private void apply(Book book, BookForm form, String isbn, int copies) {
        book.setTitle(form.getTitle().trim());
        book.setAuthor(form.getAuthor().trim());
        book.setIsbn(isbn);
        book.setGenre(form.getGenre().trim());
        book.setTotalCopies(copies);
    }

    private int validate(BookForm form, int minCopies) {
        if (isBlank(form.getTitle())) {
            throw new LibraryException("Enter the book’s title.");
        }
        if (isBlank(form.getAuthor())) {
            throw new LibraryException("Enter the book’s author.");
        }
        if (isBlank(form.getGenre())) {
            throw new LibraryException("Enter the book’s genre.");
        }
        if (normaliseIsbn(form.getIsbn()) == null) {
            throw new LibraryException("Enter a valid ISBN (10 or 13 digits).");
        }
        int copies;
        try {
            copies = Integer.parseInt(form.getTotalCopies().trim());
        } catch (NumberFormatException e) {
            throw new LibraryException("Copies must be a whole number.");
        }
        if (copies < Math.max(minCopies, 1)) {
            throw new LibraryException("Copies must be at least " + Math.max(minCopies, 1) + ".");
        }
        return copies;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    static String normaliseIsbn(String raw) {
        if (raw == null) {
            return null;
        }
        String isbn = raw.replaceAll("[\\s-]", "").toUpperCase();
        return ISBN.matcher(isbn).matches() ? isbn : null;
    }
}
