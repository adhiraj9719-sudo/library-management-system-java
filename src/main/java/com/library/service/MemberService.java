package com.library.service;

import com.library.dto.MemberForm;
import com.library.model.Role;
import com.library.model.SearchHistory;
import com.library.model.User;
import com.library.repository.LoanRepository;
import com.library.repository.NotificationRepository;
import com.library.repository.SearchHistoryRepository;
import com.library.repository.UserRepository;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD = 8;

    private final UserRepository users;
    private final LoanRepository loans;
    private final NotificationRepository notifications;
    private final SearchHistoryRepository searches;
    private final PasswordEncoder encoder;

    public MemberService(UserRepository users, LoanRepository loans, NotificationRepository notifications,
                         SearchHistoryRepository searches, PasswordEncoder encoder) {
        this.users = users;
        this.loans = loans;
        this.notifications = notifications;
        this.searches = searches;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<User> list(String q) {
        return users.search(Role.MEMBER, q == null ? "" : q.trim());
    }

    @Transactional(readOnly = true)
    public List<User> all() {
        return users.findByRoleOrderByNameAsc(Role.MEMBER);
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findByIdAndRole(id, Role.MEMBER)
                .orElseThrow(() -> new LibraryException("That member does not exist."));
    }

    @Transactional(readOnly = true)
    public User byEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new LibraryException("Account not found."));
    }

    @Transactional(readOnly = true)
    public long activeLoans(Long memberId) {
        return loans.countByMemberIdAndReturnDateIsNull(memberId);
    }

    /** Librarian creating a member; a blank membership ID is generated. */
    @Transactional
    public User create(MemberForm form) {
        String name = trim(form.getName());
        String email = trim(form.getEmail()).toLowerCase();
        String membershipId = trim(form.getMembershipId()).toUpperCase();
        validateNameAndEmail(name, email);
        if (form.getPassword() == null || form.getPassword().length() < MIN_PASSWORD) {
            throw new LibraryException("Set a password with at least " + MIN_PASSWORD + " characters.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new LibraryException("An account with that email already exists.");
        }
        if (!membershipId.isEmpty() && users.existsByMembershipIdIgnoreCase(membershipId)) {
            throw new LibraryException("That membership ID is already in use.");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(Role.MEMBER);
        user.setPasswordHash(encoder.encode(form.getPassword()));
        if (!membershipId.isEmpty()) {
            user.setMembershipId(membershipId);
        }
        users.save(user);
        if (membershipId.isEmpty()) {
            user.setMembershipId(String.format("LIB%05d", user.getId()));
            users.save(user);
        }
        return user;
    }

    /** Self-registration from the public sign-up page. */
    @Transactional
    public User register(String name, String email, String password) {
        MemberForm form = new MemberForm();
        form.setName(name == null ? "" : name);
        form.setEmail(email == null ? "" : email);
        form.setPassword(password == null ? "" : password);
        return create(form);
    }

    @Transactional
    public User update(Long id, MemberForm form) {
        User user = get(id);
        String name = trim(form.getName());
        String email = trim(form.getEmail()).toLowerCase();
        String membershipId = trim(form.getMembershipId()).toUpperCase();
        validateNameAndEmail(name, email);
        String password = form.getPassword() == null ? "" : form.getPassword();
        if (!password.isEmpty() && password.length() < MIN_PASSWORD) {
            throw new LibraryException("New passwords need at least " + MIN_PASSWORD + " characters.");
        }
        if (users.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new LibraryException("That email is already in use.");
        }
        if (!membershipId.isEmpty() && users.existsByMembershipIdIgnoreCaseAndIdNot(membershipId, id)) {
            throw new LibraryException("That membership ID is already in use.");
        }
        user.setName(name);
        user.setEmail(email);
        if (!membershipId.isEmpty()) {
            user.setMembershipId(membershipId);
        }
        if (!password.isEmpty()) {
            user.setPasswordHash(encoder.encode(password));
        }
        return users.save(user);
    }

    @Transactional
    public String delete(Long id) {
        User user = get(id);
        if (loans.countByMemberIdAndReturnDateIsNull(id) > 0) {
            throw new LibraryException(user.getName() + " still has books on loan.");
        }
        loans.deleteByMemberId(id);
        notifications.deleteByUserId(id);
        searches.deleteByUserId(id);
        users.delete(user);
        return user.getName();
    }

    /** Member editing their own profile. Returns true if the email (their login) changed. */
    @Transactional
    public boolean updateProfile(Long userId, String name, String email, String currentPassword,
                                 String newPassword) {
        User user = users.findById(userId).orElseThrow(() -> new LibraryException("Account not found."));
        String cleanName = trim(name);
        String cleanEmail = trim(email).toLowerCase();
        validateNameAndEmail(cleanName, cleanEmail);
        if (users.existsByEmailIgnoreCaseAndIdNot(cleanEmail, userId)) {
            throw new LibraryException("Another account already uses that email.");
        }
        boolean changingPassword = newPassword != null && !newPassword.isEmpty();
        if (changingPassword) {
            if (currentPassword == null || !encoder.matches(currentPassword, user.getPasswordHash())) {
                throw new LibraryException("Enter your current password to set a new one.");
            }
            if (newPassword.length() < MIN_PASSWORD) {
                throw new LibraryException("New passwords need at least " + MIN_PASSWORD + " characters.");
            }
            user.setPasswordHash(encoder.encode(newPassword));
        }
        boolean emailChanged = !user.getEmail().equalsIgnoreCase(cleanEmail);
        user.setName(cleanName);
        user.setEmail(cleanEmail);
        users.save(user);
        return emailChanged;
    }

    @Transactional
    public void savePreferences(Long userId, boolean notifyDue, boolean notifyNew) {
        User user = users.findById(userId).orElseThrow(() -> new LibraryException("Account not found."));
        user.setNotifyDue(notifyDue);
        user.setNotifyNew(notifyNew);
        users.save(user);
    }

    @Transactional
    public void recordSearch(User user, String query, String field, int resultCount) {
        SearchHistory h = new SearchHistory();
        h.setUser(user);
        h.setQuery(query);
        h.setField(field);
        h.setResultCount(resultCount);
        searches.save(h);
    }

    @Transactional(readOnly = true)
    public List<SearchHistory> recentSearches(Long userId) {
        return searches.findTop8ByUserIdOrderByIdDesc(userId);
    }

    private void validateNameAndEmail(String name, String email) {
        if (name.isEmpty()) {
            throw new LibraryException("Enter a name.");
        }
        if (!EMAIL.matcher(email).matches()) {
            throw new LibraryException("Enter a valid email address.");
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
