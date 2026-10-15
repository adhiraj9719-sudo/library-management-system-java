package com.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.library.dto.BookForm;
import com.library.dto.MemberForm;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.LibraryException;
import com.library.service.LoanService;
import com.library.service.MemberService;
import com.library.service.NotificationService;
import com.library.service.NotificationService.SendResult;
import com.library.service.ReportService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LibraryServiceTest {

    @Autowired BookService books;
    @Autowired MemberService members;
    @Autowired LoanService loans;
    @Autowired NotificationService notifications;
    @Autowired ReportService reports;

    private Book book(String isbn, int copies) {
        BookForm f = new BookForm();
        f.setTitle("Book " + isbn);
        f.setAuthor("Some Author");
        f.setIsbn(isbn);
        f.setGenre("Fiction");
        f.setTotalCopies(String.valueOf(copies));
        return books.create(f);
    }

    private User member(String email) {
        MemberForm f = new MemberForm();
        f.setName("Member " + email);
        f.setEmail(email);
        f.setPassword("password123");
        return members.create(f);
    }

    @Test
    void createBookNormalisesIsbnAndRejectsDuplicatesAndBadInput() {
        Book b = book("978-0-441-47812-5", 2);
        assertEquals("9780441478125", b.getIsbn());
        assertThrows(LibraryException.class, () -> book("9780441478125", 1));
        assertThrows(LibraryException.class, () -> book("123", 1));
        BookForm bad = new BookForm();
        bad.setTitle("");
        assertThrows(LibraryException.class, () -> books.create(bad));
    }

    @Test
    void borrowRulesAreEnforced() {
        Book one = book("9780132350884", 1);
        Book two = book("9780135957059", 2);
        Book three = book("9780553380163", 2);
        User ann = member("ann@example.com");
        User bob = member("bob@example.com");

        Loan loan = loans.borrow(one.getId(), ann.getId(), null);
        assertEquals(LocalDate.now().plusDays(14), loan.getDueDate());

        LibraryException noCopies = assertThrows(LibraryException.class,
                () -> loans.borrow(one.getId(), bob.getId(), null));
        assertTrue(noCopies.getMessage().contains("No copies"));

        loans.borrow(two.getId(), ann.getId(), null);
        LibraryException dup = assertThrows(LibraryException.class,
                () -> loans.borrow(two.getId(), ann.getId(), null));
        assertTrue(dup.getMessage().contains("already has"));

        LibraryException limit = assertThrows(LibraryException.class,
                () -> loans.borrow(three.getId(), ann.getId(), null));
        assertTrue(limit.getMessage().contains("limit of 2"));
    }

    @Test
    void returningLoans() {
        Book b = book("9780141439518", 1);
        User ann = member("ann@example.com");
        User bob = member("bob@example.com");
        Loan loan = loans.borrow(b.getId(), ann.getId(), null);

        assertThrows(LibraryException.class, () -> loans.returnLoan(loan.getId(), bob.getId(), null));
        loans.returnLoan(loan.getId(), ann.getId(), null);
        assertThrows(LibraryException.class, () -> loans.returnLoan(loan.getId(), ann.getId(), null));

        loans.borrow(b.getId(), ann.getId(), null);
        loans.returnByPair(b.getId(), ann.getId(), null);
        assertThrows(LibraryException.class, () -> loans.returnByPair(b.getId(), ann.getId(), null));
    }

    @Test
    void cannotDeleteBookOrMemberWhileOnLoan() {
        Book b = book("9780547928227", 1);
        User ann = member("ann@example.com");
        Loan loan = loans.borrow(b.getId(), ann.getId(), null);

        assertThrows(LibraryException.class, () -> books.delete(b.getId()));
        assertThrows(LibraryException.class, () -> members.delete(ann.getId()));

        loans.returnLoan(loan.getId(), null, null);
        books.delete(b.getId());
        members.delete(ann.getId());
        assertThrows(LibraryException.class, () -> books.get(b.getId()));
    }

    @Test
    void newArrivalNotifiesOnlyOptedInMembers() {
        User optedIn = member("in@example.com");
        User optedOut = member("out@example.com");
        members.savePreferences(optedOut.getId(), true, false);

        book("9780735211292", 1);

        assertEquals(1, notifications.unreadCount(optedIn.getId()));
        assertEquals(0, notifications.unreadCount(optedOut.getId()));
        notifications.markAllRead(optedIn.getId());
        assertEquals(0, notifications.unreadCount(optedIn.getId()));
    }

    @Test
    void dueRemindersAreSentOncePerDay() {
        Book b = book("9780374533557", 1);
        User ann = member("ann@example.com");
        loans.borrow(b.getId(), ann.getId(), LocalDate.now().minusDays(30));

        SendResult first = notifications.sendDueReminders();
        assertEquals(1, first.getSent());
        SendResult second = notifications.sendDueReminders();
        assertEquals(0, second.getSent());
    }

    @Test
    void profileUpdatesAndPasswordChecks() {
        User u = members.register("Cy", "cy@example.com", "password123");
        assertTrue(u.getMembershipId().startsWith("LIB"));
        assertThrows(LibraryException.class,
                () -> members.register("Cy2", "CY@example.com", "password123"));
        assertThrows(LibraryException.class,
                () -> members.updateProfile(u.getId(), "Cy", "cy@example.com", "wrong", "newpassword1"));
        assertEquals(false,
                members.updateProfile(u.getId(), "Cy B", "cy@example.com", "password123", "newpassword1"));
        assertEquals(true,
                members.updateProfile(u.getId(), "Cy B", "cy2@example.com", "", ""));
    }

    @Test
    void reportsReflectLoans() {
        Book b = book("9780062316097", 3);
        User ann = member("ann@example.com");
        loans.borrow(b.getId(), ann.getId(), null);

        assertEquals(1, reports.summary().getOnLoan());
        assertEquals(1, reports.topBooks(5).size());
        assertEquals(6, ((java.util.List<?>) reports.monthlyTrends(6).get("labels")).size());
        assertTrue(reports.inventoryCsv(books.all()).contains("Book 9780062316097"));
    }
}
