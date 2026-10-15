package com.library.service;

import com.library.model.Loan;
import com.library.model.Notification;
import com.library.model.NotificationKind;
import com.library.model.Role;
import com.library.model.User;
import com.library.repository.LoanRepository;
import com.library.repository.NotificationRepository;
import com.library.repository.UserRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    /** How many notifications were delivered and how many members had opted out. */
    public static class SendResult {
        private final int sent;
        private final int skipped;

        public SendResult(int sent, int skipped) {
            this.sent = sent;
            this.skipped = skipped;
        }

        public int getSent() { return sent; }
        public int getSkipped() { return skipped; }
    }

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final LoanRepository loans;

    public NotificationService(NotificationRepository notifications, UserRepository users,
                               LoanRepository loans) {
        this.notifications = notifications;
        this.users = users;
        this.loans = loans;
    }

    @Transactional
    public SendResult send(NotificationKind kind, String message, Collection<Long> recipientIds) {
        int sent = 0;
        int skipped = 0;
        for (Long id : recipientIds) {
            Optional<User> found = users.findByIdAndRole(id, Role.MEMBER);
            if (found.isEmpty()) {
                continue;
            }
            User user = found.get();
            boolean optedOut = (kind == NotificationKind.DUE_DATE && !user.isNotifyDue())
                    || (kind == NotificationKind.NEW_ARRIVAL && !user.isNotifyNew());
            if (optedOut) {
                skipped++;
                continue;
            }
            Notification n = new Notification();
            n.setUser(user);
            n.setKind(kind);
            n.setMessage(message);
            notifications.save(n);
            sent++;
        }
        return new SendResult(sent, skipped);
    }

    @Transactional
    public SendResult sendToAllMembers(NotificationKind kind, String message) {
        List<Long> ids = users.findByRoleOrderByNameAsc(Role.MEMBER).stream().map(User::getId).toList();
        return send(kind, message, ids);
    }

    /** Reminds members whose loans are overdue or due within two days (once per day per message). */
    @Transactional
    public SendResult sendDueReminders() {
        LocalDate today = LocalDate.now();
        List<Loan> due = loans.findByReturnDateIsNullAndDueDateLessThanEqual(today.plusDays(2));
        int sent = 0;
        int skipped = 0;
        for (Loan loan : due) {
            String message = loan.getDueDate().isBefore(today)
                    ? "Overdue: “" + loan.getBook().getTitle() + "” was due on " + loan.getDueDate()
                            + ". Please return it."
                    : "Reminder: “" + loan.getBook().getTitle() + "” is due on " + loan.getDueDate() + ".";
            Long memberId = loan.getMember().getId();
            if (notifications.existsByUserIdAndMessageAndCreatedAtAfter(memberId, message,
                    today.atStartOfDay())) {
                continue;
            }
            SendResult result = send(NotificationKind.DUE_DATE, message, List.of(memberId));
            sent += result.getSent();
            skipped += result.getSkipped();
        }
        return new SendResult(sent, skipped);
    }

    @Transactional(readOnly = true)
    public List<Notification> recent() {
        return notifications.findTop15ByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Notification> forUser(Long userId) {
        return notifications.findTop50ByUserIdOrderByIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> latestForUser(Long userId) {
        return notifications.findTop5ByUserIdOrderByIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> unreadForUser(Long userId) {
        return notifications.findTop10ByUserIdAndSeenFalseOrderByIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndSeenFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notifications.markAllSeen(userId);
    }
}
