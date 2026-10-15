package com.library.dto;

public class TopBook {
    private final String title;
    private final String author;
    private final long borrows;

    public TopBook(String title, String author, long borrows) {
        this.title = title;
        this.author = author;
        this.borrows = borrows;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public long getBorrows() { return borrows; }
}
