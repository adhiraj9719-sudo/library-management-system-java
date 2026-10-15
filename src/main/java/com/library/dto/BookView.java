package com.library.dto;

import com.library.model.Book;

/** A book plus how many copies are currently on the shelf. */
public class BookView {
    private final Book book;
    private final long onLoan;

    public BookView(Book book, long onLoan) {
        this.book = book;
        this.onLoan = onLoan;
    }

    public Book getBook() { return book; }
    public long getOnLoan() { return onLoan; }
    public long getAvailable() { return book.getTotalCopies() - onLoan; }
}
