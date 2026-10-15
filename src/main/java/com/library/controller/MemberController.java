package com.library.controller;

import com.library.dto.BookView;
import com.library.model.Loan;
import com.library.model.Notification;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.LibraryException;
import com.library.service.LoanService;
import com.library.service.MemberService;
import com.library.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MemberController {

    private static final Set<String> FIELDS = Set.of("all", "title", "author", "genre");

    private final BookService books;
    private final LoanService loans;
    private final MemberService members;
    private final NotificationService notifications;

    public MemberController(BookService books, LoanService loans, MemberService members,
                            NotificationService notifications) {
        this.books = books;
        this.loans = loans;
        this.members = members;
        this.notifications = notifications;
    }

    private User me(Principal principal) {
        return members.byEmail(principal.getName());
    }

    @GetMapping("/member")
    public String dashboard(Principal principal, Model model) {
        User user = me(principal);
        model.addAttribute("active", loans.activeForMember(user.getId()));
        model.addAttribute("loans", loans.forMember(user.getId()).stream().limit(8).toList());
        model.addAttribute("alerts", notifications.latestForUser(user.getId()));
        model.addAttribute("searches", members.recentSearches(user.getId()));
        return "member/dashboard";
    }

    @GetMapping("/member/search")
    public String search(@RequestParam(required = false, defaultValue = "") String q,
                         @RequestParam(defaultValue = "all") String field,
                         @RequestParam(defaultValue = "1") String rec,
                         Principal principal, Model model) {
        User user = me(principal);
        String f = FIELDS.contains(field) ? field : "all";
        List<BookView> results = books.search(q, f);
        if (!q.isBlank() && !"0".equals(rec)) {
            members.recordSearch(user, q.trim(), f, results.size());
        }
        Set<Long> borrowed = loans.activeForMember(user.getId()).stream()
                .map(l -> l.getBook().getId()).collect(Collectors.toSet());
        model.addAttribute("results", results);
        model.addAttribute("q", q);
        model.addAttribute("field", f);
        model.addAttribute("borrowed", borrowed);
        return "member/search";
    }

    @PostMapping("/member/borrow/{bookId}")
    public String borrow(@PathVariable Long bookId,
                         @RequestParam(required = false, defaultValue = "") String q,
                         @RequestParam(defaultValue = "all") String field,
                         Principal principal, RedirectAttributes flash) {
        try {
            Loan loan = loans.borrow(bookId, me(principal).getId(), null);
            flash.addFlashAttribute("flashSuccess", "“" + loan.getBook().getTitle()
                    + "” borrowed. Due " + loan.getDueDate() + ".");
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
        }
        flash.addAttribute("q", q);
        flash.addAttribute("field", field);
        flash.addAttribute("rec", "0");
        return "redirect:/member/search";
    }

    @PostMapping("/member/return/{loanId}")
    public String returnBook(@PathVariable Long loanId,
                             @RequestParam(defaultValue = "history") String next,
                             Principal principal, RedirectAttributes flash) {
        try {
            Loan loan = loans.returnLoan(loanId, me(principal).getId(), null);
            flash.addFlashAttribute("flashSuccess", "“" + loan.getBook().getTitle() + "” returned.");
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
        }
        return "dashboard".equals(next) ? "redirect:/member" : "redirect:/member/history";
    }

    @GetMapping("/member/history")
    public String history(Principal principal, Model model) {
        model.addAttribute("loans", loans.forMember(me(principal).getId()));
        return "member/history";
    }

    @GetMapping("/member/profile")
    public String profile() {
        return "member/profile";
    }

    @PostMapping("/member/profile")
    public String updateProfile(@RequestParam String name, @RequestParam String email,
                                @RequestParam(defaultValue = "") String currentPassword,
                                @RequestParam(defaultValue = "") String password,
                                Principal principal, HttpServletRequest request,
                                RedirectAttributes flash) {
        try {
            boolean emailChanged = members.updateProfile(me(principal).getId(), name, email,
                    currentPassword, password);
            if (emailChanged) {
                // The email is the login name, so the session must be re-established.
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                return "redirect:/login?emailChanged";
            }
            flash.addFlashAttribute("flashSuccess", "Profile updated.");
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/member/profile";
    }

    @GetMapping("/member/notifications")
    public String notificationsPage(Principal principal, Model model) {
        model.addAttribute("notifications", notifications.forUser(me(principal).getId()));
        return "member/notifications";
    }

    @PostMapping("/member/notifications/preferences")
    public String preferences(@RequestParam(required = false) String notifyDue,
                              @RequestParam(required = false) String notifyNew,
                              Principal principal, RedirectAttributes flash) {
        members.savePreferences(me(principal).getId(), notifyDue != null, notifyNew != null);
        flash.addFlashAttribute("flashSuccess", "Notification preferences saved.");
        return "redirect:/member/notifications";
    }

    @PostMapping("/member/notifications/read-all")
    public String readAll(Principal principal) {
        notifications.markAllRead(me(principal).getId());
        return "redirect:/member/notifications";
    }

    @GetMapping("/member/api/notifications")
    @ResponseBody
    public Map<String, Object> apiNotifications(Principal principal) {
        Long id = me(principal).getId();
        List<Notification> latest = notifications.unreadForUser(id);
        Map<String, Object> body = new HashMap<>();
        body.put("unread", notifications.unreadCount(id));
        body.put("latest", latest.stream().map(Notification::getMessage).toList());
        return body;
    }
}
