package com.library.controller;

import com.library.dto.BookForm;
import com.library.model.Book;
import com.library.service.BookService;
import com.library.service.LibraryException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/librarian/books")
public class LibrarianBookController {

    private final BookService books;

    public LibrarianBookController(BookService books) {
        this.books = books;
    }

    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "") String q, Model model) {
        model.addAttribute("books", books.search(q, "all"));
        model.addAttribute("q", q);
        return "librarian/books";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BookForm());
        model.addAttribute("editing", false);
        model.addAttribute("onLoan", 0L);
        return "librarian/book-form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("form") BookForm form, Model model, RedirectAttributes flash) {
        try {
            Book book = books.create(form);
            flash.addFlashAttribute("flashSuccess", "“" + book.getTitle() + "” added to the catalog.");
            return "redirect:/librarian/books";
        } catch (LibraryException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", false);
            model.addAttribute("onLoan", 0L);
            return "librarian/book-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            Book book = books.get(id);
            model.addAttribute("form", BookForm.of(book));
            model.addAttribute("editing", true);
            model.addAttribute("bookId", id);
            model.addAttribute("onLoan", books.onLoan(id));
            return "librarian/book-form";
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/librarian/books";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("form") BookForm form, Model model,
                         RedirectAttributes flash) {
        try {
            Book book = books.update(id, form);
            flash.addFlashAttribute("flashSuccess", "“" + book.getTitle() + "” updated.");
            return "redirect:/librarian/books";
        } catch (LibraryException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", true);
            model.addAttribute("bookId", id);
            model.addAttribute("onLoan", books.onLoan(id));
            return "librarian/book-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            String title = books.delete(id);
            flash.addFlashAttribute("flashSuccess", "“" + title + "” deleted from the catalog.");
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/librarian/books";
    }
}
