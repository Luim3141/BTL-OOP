package library.model;

import java.time.LocalDate;

/**
 * Snapshot of a currently borrowed book enriched with user and inventory data.
 */
public class BorrowedBookSnapshot {
    private final int loanId;
    private final int userId;
    private final String username;
    private final int bookId;
    private final String bookTitle;
    private final LocalDate loanDate;
    private final LocalDate dueDate;
    private final long daysRemaining;
    private final String status;
    private final int remainingCopies;

    public BorrowedBookSnapshot(int loanId,
                                int userId,
                                String username,
                                int bookId,
                                String bookTitle,
                                LocalDate loanDate,
                                LocalDate dueDate,
                                long daysRemaining,
                                String status,
                                int remainingCopies) {
        this.loanId = loanId;
        this.userId = userId;
        this.username = username;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.daysRemaining = daysRemaining;
        this.status = status;
        this.remainingCopies = remainingCopies;
    }

    public int getLoanId() {
        return loanId;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public String getStatus() {
        return status;
    }

    public int getRemainingCopies() {
        return remainingCopies;
    }
}
