package com.library.config;

import com.library.model.User;
import com.library.repository.NotificationRepository;
import com.library.repository.UserRepository;
import java.security.Principal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the signed-in user and unread-alert count to every page's model. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository users;
    private final NotificationRepository notifications;

    public GlobalModelAdvice(UserRepository users, NotificationRepository notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @ModelAttribute
    public void addCurrentUser(Model model, Principal principal) {
        if (principal == null) {
            return;
        }
        users.findByEmailIgnoreCase(principal.getName()).ifPresent(user -> addUser(model, user));
    }

    private void addUser(Model model, User user) {
        model.addAttribute("currentUser", user);
        if (!user.isLibrarian()) {
            model.addAttribute("unreadCount", notifications.countByUserIdAndSeenFalse(user.getId()));
        }
    }
}
