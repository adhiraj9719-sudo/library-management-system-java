package com.library.controller;

import com.library.service.BookService;
import com.library.service.ReportService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LibrarianReportController {

    private final ReportService reports;
    private final BookService books;

    public LibrarianReportController(ReportService reports, BookService books) {
        this.reports = reports;
        this.books = books;
    }

    @GetMapping("/librarian/reports")
    public String page(Model model) {
        model.addAttribute("stats", reports.summary());
        model.addAttribute("genres", reports.genreBreakdown());
        model.addAttribute("top", reports.topBooks(5));
        return "librarian/reports";
    }

    @GetMapping("/librarian/reports/data")
    @ResponseBody
    public Map<String, Object> data() {
        return reports.chartData();
    }

    @GetMapping("/librarian/reports/inventory.csv")
    public ResponseEntity<byte[]> inventoryCsv() {
        String csv = reports.inventoryCsv(books.all());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
