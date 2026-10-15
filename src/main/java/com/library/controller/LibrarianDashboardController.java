package com.library.controller;

import com.library.service.LoanService;
import com.library.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LibrarianDashboardController {

    private final ReportService reports;
    private final LoanService loans;

    public LibrarianDashboardController(ReportService reports, LoanService loans) {
        this.reports = reports;
        this.loans = loans;
    }

    @GetMapping("/librarian")
    public String dashboard(Model model) {
        model.addAttribute("stats", reports.summary());
        model.addAttribute("recent", loans.recent(8));
        return "librarian/dashboard";
    }
}
