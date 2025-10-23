package library.service;

import library.database.SimpleSqlDatabase;
import library.model.Book;
import library.model.Loan;
import library.model.Reservation;
import library.model.User;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseManager {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SimpleSqlDatabase database;

    public DatabaseManager(Path storagePath) {
        this.database = new SimpleSqlDatabase(storagePath.toString());
        initialiseSchema();
        seedInitialData();
    }

    public SimpleSqlDatabase getDatabase() {
        return database;
    }

    private void initialiseSchema() {
        database.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "full_name TEXT," +
                "email TEXT" +
                ")");
        database.execute("CREATE TABLE IF NOT EXISTS books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "author TEXT NOT NULL," +
                "category TEXT," +
                "available BOOLEAN NOT NULL" +
                ")");
        database.execute("CREATE TABLE IF NOT EXISTS loans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "book_id INTEGER NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "loan_date TEXT NOT NULL," +
                "due_date TEXT NOT NULL," +
                "return_date TEXT," +
                "daily_fee REAL NOT NULL," +
                "accrued_fee REAL NOT NULL," +
                "status TEXT NOT NULL" +
                ")");
        database.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "book_id INTEGER NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "reservation_date TEXT NOT NULL," +
                "status TEXT NOT NULL" +
                ")");
    }

    private void seedInitialData() {
        List<Map<String, Object>> adminUsers = database.query(
                "SELECT * FROM users WHERE username = ?", "admin");
        if (adminUsers.isEmpty()) {
            database.update(
                    "INSERT INTO users (username, password, role, full_name, email) VALUES (?, ?, ?, ?, ?)",
                    "admin", "admin123", "ADMIN", "Quản trị viên", "admin@example.com");
        }

        List<Map<String, Object>> books = database.query("SELECT * FROM books");
        if (books.isEmpty()) {
            database.update("INSERT INTO books (title, author, category, available) VALUES (?, ?, ?, ?)",
                    "Lập trình Java", "Nguyễn Văn A", "Công nghệ", true);
            database.update("INSERT INTO books (title, author, category, available) VALUES (?, ?, ?, ?)",
                    "Cấu trúc dữ liệu", "Trần Thị B", "Khoa học", true);
            database.update("INSERT INTO books (title, author, category, available) VALUES (?, ?, ?, ?)",
                    "Thuật toán nâng cao", "Phạm Văn C", "Công nghệ", true);
        }
    }

    public User findUserByUsername(String username) {
        List<Map<String, Object>> results = database.query(
                "SELECT * FROM users WHERE username = ?", username);
        if (results.isEmpty()) {
            return null;
        }
        return toUser(results.get(0));
    }

    public User findUserById(int id) {
        List<Map<String, Object>> results = database.query("SELECT * FROM users WHERE id = ?", id);
        if (results.isEmpty()) {
            return null;
        }
        return toUser(results.get(0));
    }

    public List<User> findAllUsers() {
        List<Map<String, Object>> results = database.query("SELECT * FROM users");
        List<User> users = new ArrayList<>();
        for (Map<String, Object> row : results) {
            users.add(toUser(row));
        }
        return users;
    }

    public User createUser(String username, String password, String role, String fullName, String email) {
        database.update(
                "INSERT INTO users (username, password, role, full_name, email) VALUES (?, ?, ?, ?, ?)",
                username, password, role.toUpperCase(Locale.ROOT), fullName, email);
        return findUserByUsername(username);
    }

    public List<Book> findAllBooks() {
        List<Map<String, Object>> results = database.query("SELECT * FROM books");
        List<Book> books = new ArrayList<>();
        for (Map<String, Object> row : results) {
            books.add(toBook(row));
        }
        return books;
    }

    public Book createBook(String title, String author, String category, boolean available) {
        database.update("INSERT INTO books (title, author, category, available) VALUES (?, ?, ?, ?)",
                title, author, category, available);
        List<Map<String, Object>> result = database.query(
                "SELECT * FROM books WHERE title = ? AND author = ?", title, author);
        if (result.isEmpty()) {
            return null;
        }
        return toBook(result.get(result.size() - 1));
    }

    public void updateBook(Book book) {
        database.update("UPDATE books SET title = ?, author = ?, category = ?, available = ? WHERE id = ?",
                book.getTitle(), book.getAuthor(), book.getCategory(), book.isAvailable(), book.getId());
    }

    public void deleteBook(int bookId) {
        database.update("DELETE FROM books WHERE id = ?", bookId);
    }

    public void updateBookAvailability(int bookId, boolean available) {
        database.update("UPDATE books SET available = ? WHERE id = ?", available, bookId);
    }

    public List<Loan> findAllLoans() {
        refreshLoanStatuses();
        List<Map<String, Object>> results = database.query("SELECT * FROM loans");
        List<Loan> loans = new ArrayList<>();
        for (Map<String, Object> row : results) {
            loans.add(toLoan(row));
        }
        return loans;
    }

    public List<Loan> findLoansByUser(int userId) {
        refreshLoanStatuses();
        List<Map<String, Object>> results = database.query("SELECT * FROM loans WHERE user_id = ?", userId);
        List<Loan> loans = new ArrayList<>();
        for (Map<String, Object> row : results) {
            loans.add(toLoan(row));
        }
        return loans;
    }

    public Loan createLoan(int bookId, int userId, LocalDate loanDate, LocalDate dueDate, double dailyFee) {
        database.update("INSERT INTO loans (book_id, user_id, loan_date, due_date, return_date, daily_fee, accrued_fee, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                bookId,
                userId,
                loanDate.format(DATE_FORMATTER),
                dueDate.format(DATE_FORMATTER),
                null,
                dailyFee,
                0.0,
                "BORROWED");
        List<Map<String, Object>> rows = database.query(
                "SELECT * FROM loans WHERE book_id = ? AND user_id = ?", bookId, userId);
        return toLoan(rows.get(rows.size() - 1));
    }

    public void markLoanReturned(int loanId, LocalDate returnDate) {
        database.update("UPDATE loans SET status = ?, return_date = ? WHERE id = ?",
                "RETURNED", returnDate.format(DATE_FORMATTER), loanId);
    }

    public void refreshLoanStatuses() {
        List<Map<String, Object>> rows = database.query("SELECT * FROM loans");
        LocalDate today = LocalDate.now();
        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            if ("RETURNED".equalsIgnoreCase(status)) {
                continue;
            }
            LocalDate dueDate = LocalDate.parse((String) row.get("due_date"), DATE_FORMATTER);
            double dailyFee = getDouble(row.get("daily_fee"));
            double currentFee = getDouble(row.get("accrued_fee"));
            String newStatus = status;
            double newFee = currentFee;
            if (today.isAfter(dueDate)) {
                long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
                newFee = daysOverdue * dailyFee;
                newStatus = "OVERDUE";
            } else {
                newFee = 0.0;
                newStatus = "BORROWED";
            }
            if (!newStatus.equals(status) || Double.compare(newFee, currentFee) != 0) {
                database.update("UPDATE loans SET status = ?, accrued_fee = ? WHERE id = ?",
                        newStatus,
                        newFee,
                        ((Number) row.get("id")).intValue());
            }
        }
    }

    public List<Reservation> findAllReservations() {
        List<Map<String, Object>> rows = database.query("SELECT * FROM reservations");
        List<Reservation> reservations = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            reservations.add(toReservation(row));
        }
        return reservations;
    }

    public List<Reservation> findReservationsByUser(int userId) {
        List<Map<String, Object>> rows = database.query("SELECT * FROM reservations WHERE user_id = ?", userId);
        List<Reservation> reservations = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            reservations.add(toReservation(row));
        }
        return reservations;
    }

    public Reservation createReservation(int bookId, int userId, LocalDate date) {
        database.update("INSERT INTO reservations (book_id, user_id, reservation_date, status) VALUES (?, ?, ?, ?)",
                bookId,
                userId,
                date.format(DATE_FORMATTER),
                "ACTIVE");
        List<Map<String, Object>> rows = database.query(
                "SELECT * FROM reservations WHERE book_id = ? AND user_id = ?", bookId, userId);
        return toReservation(rows.get(rows.size() - 1));
    }

    public void cancelReservation(int reservationId) {
        database.update("UPDATE reservations SET status = ? WHERE id = ?", "CANCELLED", reservationId);
    }

    public void fulfilReservation(int reservationId) {
        database.update("UPDATE reservations SET status = ? WHERE id = ?", "FULFILLED", reservationId);
    }

    private User toUser(Map<String, Object> row) {
        return new User(((Number) row.get("id")).intValue(),
                (String) row.get("username"),
                (String) row.get("password"),
                (String) row.get("role"),
                (String) row.get("full_name"),
                (String) row.get("email"));
    }

    private Book toBook(Map<String, Object> row) {
        return new Book(((Number) row.get("id")).intValue(),
                (String) row.get("title"),
                (String) row.get("author"),
                (String) row.get("category"),
                getBoolean(row.get("available")));
    }

    private Loan toLoan(Map<String, Object> row) {
        LocalDate loanDate = LocalDate.parse((String) row.get("loan_date"), DATE_FORMATTER);
        String returnDateRaw = (String) row.get("return_date");
        LocalDate returnDate = returnDateRaw == null ? null : LocalDate.parse(returnDateRaw, DATE_FORMATTER);
        LocalDate dueDate = LocalDate.parse((String) row.get("due_date"), DATE_FORMATTER);
        double dailyFee = getDouble(row.get("daily_fee"));
        double accruedFee = getDouble(row.get("accrued_fee"));
        return new Loan(((Number) row.get("id")).intValue(),
                ((Number) row.get("book_id")).intValue(),
                ((Number) row.get("user_id")).intValue(),
                loanDate,
                dueDate,
                returnDate,
                (String) row.get("status"),
                dailyFee,
                accruedFee);
    }

    private Reservation toReservation(Map<String, Object> row) {
        LocalDate reservationDate = LocalDate.parse((String) row.get("reservation_date"), DATE_FORMATTER);
        return new Reservation(((Number) row.get("id")).intValue(),
                ((Number) row.get("book_id")).intValue(),
                ((Number) row.get("user_id")).intValue(),
                reservationDate,
                (String) row.get("status"));
    }

    private boolean getBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return false;
    }

    private double getDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            return Double.parseDouble(text);
        }
        return 0.0;
    }
}
