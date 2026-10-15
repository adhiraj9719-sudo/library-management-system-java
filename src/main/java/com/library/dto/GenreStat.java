package com.library.dto;

public class GenreStat {
    private final String genre;
    private final long titles;
    private final long copies;
    private final long onLoan;

    public GenreStat(String genre, long titles, long copies, long onLoan) {
        this.genre = genre;
        this.titles = titles;
        this.copies = copies;
        this.onLoan = onLoan;
    }

    public String getGenre() { return genre; }
    public long getTitles() { return titles; }
    public long getCopies() { return copies; }
    public long getOnLoan() { return onLoan; }
}
