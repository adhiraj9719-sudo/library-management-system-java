package com.library.controller;

import com.library.model.NotificationKind;
import com.library.service.LibraryException;
import com.library.service.MemberService;
import com.library.service.NotificationService;
import com.library.service.NotificationService.SendResult;
import com.library.model.User;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LibrarianNotificationController {

    private final NotificationService notifications;
    private final MemberService members;

    public LibrarianNotificationController(NotificationService notifications, MemberService members) {
        this.notifications = notifications;
        this.members = members;
    }

    @GetMapping("/librarian/notifications")
    public String page(Model model) {
        model.addAttribute("members", members.all());
        model.addAttribute("recent", notifications.recent());
        model.addAttribute("kinds", NotificationKind.values());
        return "librarian/notifications";
    }

    @PostMapping("/librarian/notifications")
    public String send(@RequestParam(defaultValue = "GENERAL") NotificationKind kind,
                       @RequestParam(defaultValue = "") String message,
                       @RequestParam(defaultValue = "all") String audience,
                       @RequestParam(required = false) List<Long> recipients,
                       RedirectAttributes flash) {
        try {
            if (message.isBlank()) {
                throw new LibraryException("Write a message to send.");
            }
            if (message.length() > 500) {
                throw new LibraryException("Keep the message under 500 characters.");
            }
            List<Long> ids = "all".equals(audience)
                    ? members.all().stream().map(User::getId).toList()
                    : (recipients == null ? List.<Long>of() : recipients);
            if (ids.isEmpty()) {
                throw new LibraryException("Choose at least one recipient.");
            }
            SendResult result = notifications.send(kind, message.trim(), ids);
            String note = result.getSkipped() > 0
                    ? " " + result.getSkipped() + " member(s) opted out of this type." : "";
            flash.addFlashAttribute("flashSuccess",
                    "Notification sent to " + result.getSent() + " member(s)." + note);
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/librarian/notifications";
    }

    @PostMapping("/librarian/notifications/due-reminders")
    public String dueReminders(RedirectAttributes flash) {
        SendResult result = notifications.sendDueReminders();
        if (result.getSent() == 0 && result.getSkipped() == 0) {
            flash.addFlashAttribute("flashSuccess", "No new reminders needed: nothing is overdue or due in the next 2 days.");
        } else {
            flash.addFlashAttribute("flashSuccess", "Sent " + result.getSent() + " due-date reminder(s). "
                    + result.getSkipped() + " skipped (opted out).");
        }
        return "redirect:/librarian/notifications";
    }
}
