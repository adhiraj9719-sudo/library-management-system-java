package com.library.config;

import com.library.dto.MemberForm;
import com.library.model.Book;
import com.library.model.Role;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import com.library.service.MemberService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the default librarian (and optional demo data) on startup. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository users;
    private final BookRepository books;
    private final MemberService members;
    private final PasswordEncoder encoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;
    private final boolean seedDemo;

    public DataInitializer(UserRepository users, BookRepository books, MemberService members,
                           PasswordEncoder encoder,
                           @Value("${library.admin.name}") String adminName,
                           @Value("${library.admin.email}") String adminEmail,
                           @Value("${library.admin.password}") String adminPassword,
                           @Value("${library.seed-demo:false}") boolean seedDemo) {
        this.users = users;
        this.books = books;
        this.members = members;
        this.encoder = encoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.seedDemo = seedDemo;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByRole(Role.LIBRARIAN)) {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPasswordHash(encoder.encode(adminPassword));
            admin.setRole(Role.LIBRARIAN);
            users.save(admin);
        }
        if (seedDemo) {
            seedDemoData();
        }
    }

    private void seedDemoData() {
        if (books.count() == 0) {
            addBook("The Left Hand of Darkness", "Ursula K. Le Guin", "9780441478125", "Science Fiction", 3);
            addBook("Kitchen", "Banana Yoshimoto", "9780802142443", "Fiction", 2);
            addBook("Sapiens", "Yuval Noah Harari", "9780062316097", "History", 4);
            addBook("The Pragmatic Programmer", "Andrew Hunt", "9780135957059", "Technology", 2);
            addBook("Clean Code", "Robert C. Martin", "9780132350884", "Technology", 3);
            addBook("A Brief History of Time", "Stephen Hawking", "9780553380163", "Science", 2);
            addBook("Pride and Prejudice", "Jane Austen", "9780141439518", "Classic", 5);
            addBook("The Hobbit", "J.R.R. Tolkien", "9780547928227", "Fantasy", 3);
            addBook("Atomic Habits", "James Clear", "9780735211292", "Self-Help", 4);
            addBook("Thinking, Fast and Slow", "Daniel Kahneman", "9780374533557", "Psychology", 2);
        }
        if (!users.existsByEmailIgnoreCase("member@library.local")) {
            MemberForm form = new MemberForm();
            form.setName("Demo Member");
            form.setEmail("member@library.local");
            form.setPassword("member12345");
            members.create(form);
        }
    }

    private void addBook(String title, String author, String isbn, String genre, int copies) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setIsbn(isbn);
        b.setGenre(genre);
        b.setTotalCopies(copies);
        books.save(b);
    }
}
