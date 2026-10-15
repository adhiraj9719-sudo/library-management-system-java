package com.library.service;

import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Role;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private final LoanRepository loans;
    private final BookRepository books;
    private final UserRepository users;
    private final int loanDays;
    private final int maxActiveLoans;

    public LoanService(LoanRepository loans, BookRepository books, UserRepository users,
                       @Value("${library.loan-days:14}") int loanDays,
                       @Value("${library.max-active-loans:5}") int maxActiveLoans) {
        this.loans = loans;
        this.books = books;
        this.users = users;
        this.loanDays = loanDays;
        this.maxActiveLoans = maxActiveLoans;
    }

    @Transactional
    public Loan borrow(Long bookId, Long memberId, LocalDate date) {
        if (bookId == null || memberId == null) {
            throw new LibraryException("Choose a book and a member.");
        }
        LocalDate borrowDate = date != null ? date : LocalDate.now();
        Book book = books.findById(bookId)
                .orElseThrow(() -> new LibraryException("That book does not exist."));
        User member = users.findByIdAndRole(memberId, Role.MEMBER)
                .orElseThrow(() -> new LibraryException("That member does not exist."));

        if (loans.existsByBookIdAndMemberIdAndReturnDateIsNull(bookId, memberId)) {
            throw new LibraryException(member.getName() + " already has “" + book.getTitle() + "” on loan.");
        }
        long onLoan = loans.countByBookIdAndReturnDateIsNull(bookId);
        if (book.getTotalCopies() - onLoan < 1) {
            throw new LibraryException("No copies of “" + book.getTitle() + "” are available right now.");
        }
        if (loans.countByMemberIdAndReturnDateIsNull(memberId) >= maxActiveLoans) {
            throw new LibraryException(member.getName() + " has reached the limit of " + maxActiveLoans
                    + " books on loan.");
        }
        Loan loan = new Loan();
        loan.setBook(book);
        loan.setMember(member);
        loan.setBorrowDate(borrowDate);
        loan.setDueDate(borrowDate.plusDays(loanDays));
        return loans.save(loan);
    }

    /** Returns a loan. If memberId is not null the loan must belong to that member. */
    @Transactional
    public Loan returnLoan(Long loanId, Long memberId, LocalDate date) {
        Loan loan = loans.findById(loanId)
                .filter(l -> l.getReturnDate() == null)
                .filter(l -> memberId == null || l.getMember().getId().equals(memberId))
                .orElseThrow(() -> new LibraryException("That loan was not found or has already been returned."));
        LocalDate returnDate = date != null ? date : LocalDate.now();
        if (returnDate.isBefore(loan.getBorrowDate())) {
            throw new LibraryException("The return date cannot be before the borrow date.");
        }
        loan.setReturnDate(returnDate);
        return loans.save(loan);
    }

    @Transactional
    public Loan returnByPair(Long bookId, Long memberId, LocalDate date) {
        if (bookId == null || memberId == null) {
            throw new LibraryException("Choose a book and a member.");
        }
        Loan loan = loans.findFirstByBookIdAndMemberIdAndReturnDateIsNull(bookId, memberId)
                .orElseThrow(() -> new LibraryException("That member does not have this book on loan."));
        return returnLoan(loan.getId(), null, date);
    }

    @Transactional(readOnly = true)
    public List<Loan> all() {
        return loans.findAllByOrderByIdDesc();
    }

    /** Filters: all, active, overdue, returned. Limited to the 200 most recent. */
    @Transactional(readOnly = true)
    public List<Loan> filtered(String status) {
        return loans.findAllByOrderByIdDesc().stream()
                .filter(l -> switch (status == null ? "all" : status) {
                    case "active" -> !l.isReturned();
                    case "overdue" -> l.isOverdue();
                    case "returned" -> l.isReturned();
                    default -> true;
                })
                .limit(200)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Loan> recent(int count) {
        return loans.findAllByOrderByIdDesc().stream().limit(count).toList();
    }

    @Transactional(readOnly = true)
    public List<Loan> forMember(Long memberId) {
        return loans.findByMemberIdOrderByIdDesc(memberId);
    }

    @Transactional(readOnly = true)
    public List<Loan> activeForMember(Long memberId) {
        return loans.findByMemberIdAndReturnDateIsNullOrderByIdDesc(memberId);
    }
}
