package library.model;

import java.time.LocalDate;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class Loan {
    private final int id;
    private final int bookId;
    private final int userId;
    private final LocalDate loanDate;
    private final LocalDate dueDate;
    private final LocalDate returnDate;
    private final String status;
    private final double dailyFee;
    private final double accruedFee;

    public Loan(int id,
                int bookId,
                int userId,
                LocalDate loanDate,
                LocalDate dueDate,
                LocalDate returnDate,
                String status,
                double dailyFee,
                double accruedFee) {
        this.id = id;
        this.bookId = bookId;
        this.userId = userId;
        this.loanDate = requireNonNull(loanDate);
        this.dueDate = requireNonNull(dueDate);
        this.returnDate = returnDate;
        this.status = requireNonNull(status);
        this.dailyFee = dailyFee;
        this.accruedFee = accruedFee;
    }

    public int getId() {
        return id;
    }

    public int getBookId() {
        return bookId;
    }

    public int getUserId() {
        return userId;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return "BORROWED".equalsIgnoreCase(status);
    }

    public double getDailyFee() {
        return dailyFee;
    }

    public double getAccruedFee() {
        return accruedFee;
    }

    public Loan markReturned(LocalDate date) {
        return new Loan(id, bookId, userId, loanDate, dueDate, date, "RETURNED", dailyFee, accruedFee);
    }

    public Loan withStatus(String newStatus, double newAccruedFee) {
        return new Loan(id, bookId, userId, loanDate, dueDate, returnDate, newStatus, dailyFee, newAccruedFee);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Loan loan)) {
            return false;
        }
        return id == loan.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
