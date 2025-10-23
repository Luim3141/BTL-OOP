package library.model;

import java.util.Objects;

public class Book {
    private final int id;
    private final String title;
    private final String author;
    private final String category;
    private final int totalCopies;
    private final int availableCopies;

    public Book(int id,
                String title,
                String author,
                String category,
                int totalCopies,
                int availableCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
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

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public Book withDetails(String newTitle,
                            String newAuthor,
                            String newCategory,
                            int newTotalCopies,
                            int newAvailableCopies) {
        return new Book(id, newTitle, newAuthor, newCategory, newTotalCopies, newAvailableCopies);
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
