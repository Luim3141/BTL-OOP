package library.model;

import java.util.Objects;

public class Book {
    private final int id;
    private final String title;
    private final String author;
    private final String category;
    private final boolean available;

    public Book(int id, String title, String author, String category, boolean available) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.available = available;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public boolean isAvailable() {
        return available;
    }

    public Book withAvailability(boolean availability) {
        return new Book(id, title, author, category, availability);
    }

    public Book withDetails(String newTitle, String newAuthor, String newCategory) {
        return new Book(id, newTitle, newAuthor, newCategory, available);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Book book)) {
            return false;
        }
        return id == book.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
