package library.model;

import java.time.LocalDate;
import java.util.Objects;

public class Reservation {
    private final int id;
    private final int bookId;
    private final int userId;
    private final LocalDate reservationDate;
    private final String status;

    public Reservation(int id, int bookId, int userId, LocalDate reservationDate, String status) {
        this.id = id;
        this.bookId = bookId;
        this.userId = userId;
        this.reservationDate = reservationDate;
        this.status = status;
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

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reservation that)) {
            return false;
        }
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
