package com.library.dto;

import com.library.model.Book;

public class BookForm {
    private String title = "";
    private String author = "";
    private String isbn = "";
    private String genre = "";
    private String totalCopies = "1";

    public static BookForm of(Book book) {
        BookForm f = new BookForm();
        f.title = book.getTitle();
        f.author = book.getAuthor();
        f.isbn = book.getIsbn();
        f.genre = book.getGenre();
        f.totalCopies = String.valueOf(book.getTotalCopies());
        return f;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getTotalCopies() { return totalCopies; }
    public void setTotalCopies(String totalCopies) { this.totalCopies = totalCopies; }
}
