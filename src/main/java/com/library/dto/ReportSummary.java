package com.library.dto;

public class ReportSummary {
    private final long titles;
    private final long copies;
    private final long onLoan;
    private final long overdue;
    private final long members;

    public ReportSummary(long titles, long copies, long onLoan, long overdue, long members) {
        this.titles = titles;
        this.copies = copies;
        this.onLoan = onLoan;
        this.overdue = overdue;
        this.members = members;
    }

    public long getTitles() { return titles; }
    public long getCopies() { return copies; }
    public long getOnLoan() { return onLoan; }
    public long getAvailable() { return copies - onLoan; }
    public long getOverdue() { return overdue; }
    public long getMembers() { return members; }
}
