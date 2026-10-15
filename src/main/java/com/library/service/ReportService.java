package com.library.service;

import com.library.dto.GenreStat;
import com.library.dto.ReportSummary;
import com.library.dto.TopBook;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Role;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final BookRepository books;
    private final LoanRepository loans;
    private final UserRepository users;

    public ReportService(BookRepository books, LoanRepository loans, UserRepository users) {
        this.books = books;
        this.loans = loans;
        this.users = users;
    }

    public ReportSummary summary() {
        long copies = books.findAll().stream().mapToLong(Book::getTotalCopies).sum();
        return new ReportSummary(
                books.count(),
                copies,
                loans.countByReturnDateIsNull(),
                loans.countByReturnDateIsNullAndDueDateBefore(LocalDate.now()),
                users.countByRole(Role.MEMBER));
    }

    public List<GenreStat> genreBreakdown() {
        Map<Long, Long> active = new HashMap<>();
        for (Object[] row : loans.activeCountsByBook()) {
            active.put((Long) row[0], (Long) row[1]);
        }
        Map<String, long[]> byGenre = new LinkedHashMap<>(); // [titles, copies, onLoan]
        for (Book b : books.findAllByOrderByTitleAsc()) {
            long[] totals = byGenre.computeIfAbsent(b.getGenre(), g -> new long[3]);
            totals[0] += 1;
            totals[1] += b.getTotalCopies();
            totals[2] += active.getOrDefault(b.getId(), 0L);
        }
        List<GenreStat> result = new ArrayList<>();
        byGenre.forEach((genre, t) -> result.add(new GenreStat(genre, t[0], t[1], t[2])));
        result.sort(Comparator.comparingLong(GenreStat::getCopies).reversed());
        return result;
    }

    public List<TopBook> topBooks(int limit) {
        Map<Long, Long> counts = new HashMap<>();
        Map<Long, Book> byId = new HashMap<>();
        for (Loan l : loans.findAll()) {
            counts.merge(l.getBook().getId(), 1L, Long::sum);
            byId.put(l.getBook().getId(), l.getBook());
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed()
                        .thenComparing(e -> byId.get(e.getKey()).getTitle()))
                .limit(limit)
                .map(e -> new TopBook(byId.get(e.getKey()).getTitle(), byId.get(e.getKey()).getAuthor(),
                        e.getValue()))
                .toList();
    }

    /** Borrowed/returned counts for the last {@code months} months, oldest first. */
    public Map<String, Object> monthlyTrends(int months) {
        YearMonth now = YearMonth.now();
        List<Loan> all = loans.findAll();
        List<String> labels = new ArrayList<>();
        List<Long> borrowed = new ArrayList<>();
        List<Long> returned = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            labels.add(ym.toString());
            borrowed.add(all.stream()
                    .filter(l -> l.getBorrowDate() != null && YearMonth.from(l.getBorrowDate()).equals(ym))
                    .count());
            returned.add(all.stream()
                    .filter(l -> l.getReturnDate() != null && YearMonth.from(l.getReturnDate()).equals(ym))
                    .count());
        }
        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("labels", labels);
        trends.put("borrowed", borrowed);
        trends.put("returned", returned);
        return trends;
    }

    public Map<String, Object> chartData() {
        List<GenreStat> genres = genreBreakdown();
        Map<String, Object> genreData = new LinkedHashMap<>();
        genreData.put("labels", genres.stream().map(GenreStat::getGenre).toList());
        genreData.put("copies", genres.stream().map(GenreStat::getCopies).toList());
        genreData.put("on_loan", genres.stream().map(GenreStat::getOnLoan).toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("trends", monthlyTrends(6));
        data.put("genres", genreData);
        return data;
    }

    /** Inventory as CSV text. */
    public String inventoryCsv(List<com.library.dto.BookView> rows) {
        StringBuilder sb = new StringBuilder("Title,Author,ISBN,Genre,Total copies,Available,On loan\n");
        for (com.library.dto.BookView v : rows) {
            Book b = v.getBook();
            sb.append(csv(b.getTitle())).append(',').append(csv(b.getAuthor())).append(',')
                    .append(csv(b.getIsbn())).append(',').append(csv(b.getGenre())).append(',')
                    .append(b.getTotalCopies()).append(',').append(v.getAvailable()).append(',')
                    .append(v.getOnLoan()).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value) {
        String v = value == null ? "" : value;
        // Neutralise spreadsheet formula injection, then quote.
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
