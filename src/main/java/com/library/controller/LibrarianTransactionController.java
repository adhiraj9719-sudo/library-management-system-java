package com.library.controller;

import com.library.model.Loan;
import com.library.service.BookService;
import com.library.service.LibraryException;
import com.library.service.LoanService;
import com.library.service.MemberService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LibrarianTransactionController {

    private final LoanService loans;
    private final BookService books;
    private final MemberService members;

    public LibrarianTransactionController(LoanService loans, BookService books, MemberService members) {
        this.loans = loans;
        this.books = books;
        this.members = members;
    }

    @GetMapping("/librarian/transactions")
    public String list(@RequestParam(defaultValue = "all") String status, Model model) {
        model.addAttribute("rows", loans.filtered(status));
        model.addAttribute("status", status);
        model.addAttribute("books", books.all());
        model.addAttribute("members", members.all());
        model.addAttribute("today", LocalDate.now());
        return "librarian/transactions";
    }

    @PostMapping("/librarian/transactions")
    public String record(@RequestParam String action,
                         @RequestParam(required = false) Long bookId,
                         @RequestParam(required = false) Long memberId,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         RedirectAttributes flash) {
        try {
            if ("borrow".equals(action)) {
                Loan loan = loans.borrow(bookId, memberId, date);
                flash.addFlashAttribute("flashSuccess", "“" + loan.getBook().getTitle() + "” borrowed by "
                        + loan.getMember().getName() + ". Due " + loan.getDueDate() + ".");
            } else if ("return".equals(action)) {
                Loan loan = loans.returnByPair(bookId, memberId, date);
                flash.addFlashAttribute("flashSuccess", "“" + loan.getBook().getTitle() + "” returned.");
            } else {
                throw new LibraryException("Choose borrow or return.");
            }
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/librarian/transactions";
    }

    @PostMapping("/librarian/transactions/{id}/return")
    public String markReturned(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Loan loan = loans.returnLoan(id, null, null);
            flash.addFlashAttribute("flashSuccess", "“" + loan.getBook().getTitle() + "” returned.");
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/librarian/transactions";
    }
}
