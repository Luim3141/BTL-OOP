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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DatabaseManager {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SimpleSqlDatabase database;
    private final CopyOnWriteArrayList<Consumer<DataChange>> changeListeners = new CopyOnWriteArrayList<>();

    public DatabaseManager(Path storagePath) {
        this.database = new SimpleSqlDatabase(storagePath.toString());
        initialiseSchema();
        seedInitialData();
    }

    public SimpleSqlDatabase getDatabase() {
        return database;
    }

    public void close() {
        database.close();
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
                "available BOOLEAN NOT NULL," +
                "total_copies INTEGER NOT NULL," +
                "available_copies INTEGER NOT NULL" +
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
        migrateBookInventory();
    }

    private void migrateBookInventory() {
        List<Map<String, Object>> rows = database.query("SELECT * FROM books");
        for (Map<String, Object> row : rows) {
            int id = ((Number) row.get("id")).intValue();
            int total = row.get("total_copies") == null ? 1 : getInt(row.get("total_copies"));
            if (total <= 0) {
                total = 1;
            }
            int available = row.get("available_copies") == null
                    ? (getBoolean(row.get("available")) ? total : 0)
                    : getInt(row.get("available_copies"));
            available = Math.max(0, Math.min(available, total));
            boolean availableFlag = available > 0;
            database.update("UPDATE books SET total_copies = ?, available_copies = ?, available = ? WHERE id = ?",
                    total,
                    available,
                    availableFlag,
                    id);
        }
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
            database.update("INSERT INTO books (title, author, category, available, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?)",
                    "Lập trình Java", "Nguyễn Văn A", "Công nghệ", true, 3, 3);
            database.update("INSERT INTO books (title, author, category, available, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?)",
                    "Cấu trúc dữ liệu", "Trần Thị B", "Khoa học", true, 2, 2);
            database.update("INSERT INTO books (title, author, category, available, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?)",
                    "Thuật toán nâng cao", "Phạm Văn C", "Công nghệ", true, 4, 4);
        }
        notifyChange("users", "books");
    }

    public AutoCloseable addChangeListener(Consumer<DataChange> listener) {
        changeListeners.add(listener);
        return () -> removeChangeListener(listener);
    }

    public void removeChangeListener(Consumer<DataChange> listener) {
        changeListeners.remove(listener);
    }

    private void notifyChange(String... tables) {
        if (changeListeners.isEmpty()) {
            return;
        }
        Set<String> affected = new LinkedHashSet<>();
        if (tables != null) {
            for (String table : tables) {
                if (table != null && !table.isBlank()) {
                    affected.add(table.toLowerCase(Locale.ROOT));
                }
            }
        }
        if (affected.isEmpty()) {
            affected.add("*");
        }
        DataChange change = new DataChange(Collections.unmodifiableSet(affected), System.currentTimeMillis());
        for (Consumer<DataChange> listener : changeListeners) {
            listener.accept(change);
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
        User created = findUserByUsername(username);
        notifyChange("users");
        return created;
    }

    public void deleteUser(int userId) {
        List<Loan> loans = findLoansByUser(userId);
        for (Loan loan : loans) {
            if (!loan.isReturned() && !loan.isRejected() && !loan.isPending()) {
                Book book = findBookById(loan.getBookId());
                if (book != null) {
                    int newAvailable = Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1);
                    database.update("UPDATE books SET available_copies = ?, available = ? WHERE id = ?",
                            newAvailable,
                            newAvailable > 0,
                            book.getId());
                }
            }
        }
        database.update("DELETE FROM loans WHERE user_id = ?", userId);
        database.update("DELETE FROM reservations WHERE user_id = ?", userId);
        database.update("DELETE FROM users WHERE id = ?", userId);
        notifyChange("users", "loans", "reservations", "books");
    }

    public List<Book> findAllBooks() {
        List<Map<String, Object>> results = database.query("SELECT * FROM books");
        List<Book> books = new ArrayList<>();
        for (Map<String, Object> row : results) {
            books.add(toBook(row));
        }
        return books;
    }

    public Book findBookById(int id) {
        List<Map<String, Object>> result = database.query("SELECT * FROM books WHERE id = ?", id);
        if (result.isEmpty()) {
            return null;
        }
        return toBook(result.get(0));
    }

    public Book createBook(String title,
                           String author,
                           String category,
                           int totalCopies,
                           int availableCopies) {
        int safeTotal = Math.max(totalCopies, 0);
        int safeAvailable = Math.max(0, Math.min(availableCopies, safeTotal));
        database.update("INSERT INTO books (title, author, category, available, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?)",
                title,
                author,
                category,
                safeAvailable > 0,
                safeTotal,
                safeAvailable);
        List<Map<String, Object>> result = database.query(
                "SELECT * FROM books WHERE title = ? AND author = ?", title, author);
        if (result.isEmpty()) {
            return null;
        }
        Book created = toBook(result.get(result.size() - 1));
        notifyChange("books");
        return created;
    }

    public void updateBook(Book book) {
        int safeTotal = Math.max(book.getTotalCopies(), 0);
        int safeAvailable = Math.max(0, Math.min(book.getAvailableCopies(), safeTotal));
        database.update("UPDATE books SET title = ?, author = ?, category = ?, available = ?, total_copies = ?, available_copies = ? WHERE id = ?",
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                safeAvailable > 0,
                safeTotal,
                safeAvailable,
                book.getId());
        notifyChange("books");
    }

    public void deleteBook(int bookId) {
        database.update("DELETE FROM books WHERE id = ?", bookId);
        notifyChange("books");
    }

    public void decrementAvailableCopies(int bookId) {
        Book book = findBookById(bookId);
        if (book == null) {
            return;
        }
        int newAvailable = Math.max(0, book.getAvailableCopies() - 1);
        database.update("UPDATE books SET available_copies = ?, available = ? WHERE id = ?",
                newAvailable,
                newAvailable > 0,
                bookId);
        notifyChange("books");
    }

    public void incrementAvailableCopies(int bookId) {
        Book book = findBookById(bookId);
        if (book == null) {
            return;
        }
        int newAvailable = Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1);
        database.update("UPDATE books SET available_copies = ?, available = ? WHERE id = ?",
                newAvailable,
                newAvailable > 0,
                bookId);
        notifyChange("books");
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

    public Loan findLoanById(int loanId) {
        List<Map<String, Object>> rows = database.query("SELECT * FROM loans WHERE id = ?", loanId);
        if (rows.isEmpty()) {
            return null;
        }
        return toLoan(rows.get(0));
    }

    public Loan createBorrowRequest(int bookId,
                                    int userId,
                                    LocalDate requestDate,
                                    int loanPeriodDays,
                                    double dailyFee) {
        LocalDate dueDate = requestDate.plusDays(loanPeriodDays);
        database.update("INSERT INTO loans (book_id, user_id, loan_date, due_date, return_date, daily_fee, accrued_fee, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                bookId,
                userId,
                requestDate.format(DATE_FORMATTER),
                dueDate.format(DATE_FORMATTER),
                null,
                dailyFee,
                0.0,
                "PENDING");
        List<Map<String, Object>> rows = database.query(
                "SELECT * FROM loans WHERE book_id = ? AND user_id = ?", bookId, userId);
        Loan created = toLoan(rows.get(rows.size() - 1));
        notifyChange("loans");
        return created;
    }

    public Loan approveLoan(int loanId, int loanPeriodDays) {
        Loan loan = findLoanById(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu mượn với id=" + loanId);
        }
        if (!loan.isPending()) {
            return loan;
        }
        Book book = findBookById(loan.getBookId());
        if (book == null) {
            throw new IllegalStateException("Không tìm thấy sách tương ứng");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("Không còn bản sao khả dụng để cho mượn");
        }
        LocalDate loanDate = LocalDate.now();
        LocalDate dueDate = loanDate.plusDays(loanPeriodDays);
        database.update("UPDATE loans SET status = ?, loan_date = ?, due_date = ?, return_date = ?, accrued_fee = ? WHERE id = ?",
                "BORROWED",
                loanDate.format(DATE_FORMATTER),
                dueDate.format(DATE_FORMATTER),
                null,
                0.0,
                loanId);
        decrementAvailableCopies(book.getId());
        Loan updated = findLoanById(loanId);
        notifyChange("loans", "books");
        return updated;
    }

    public Loan rejectLoan(int loanId) {
        Loan loan = findLoanById(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu mượn với id=" + loanId);
        }
        if (loan.isPending()) {
            database.update("UPDATE loans SET status = ? WHERE id = ?", "REJECTED", loanId);
        }
        Loan updated = findLoanById(loanId);
        notifyChange("loans");
        return updated;
    }

    public void markLoanReturned(int loanId, LocalDate returnDate) {
        Loan loan = findLoanById(loanId);
        if (loan == null) {
            return;
        }
        database.update("UPDATE loans SET status = ?, return_date = ? WHERE id = ?",
                "RETURNED", returnDate.format(DATE_FORMATTER), loanId);
        incrementAvailableCopies(loan.getBookId());
        notifyChange("loans");
    }

    public void refreshLoanStatuses() {
        List<Map<String, Object>> rows = database.query("SELECT * FROM loans");
        LocalDate today = LocalDate.now();
        boolean changed = false;
        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            if ("RETURNED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
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
                changed = true;
            }
        }
        if (changed) {
            notifyChange("loans");
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
                "PENDING");
        List<Map<String, Object>> rows = database.query(
                "SELECT * FROM reservations WHERE book_id = ? AND user_id = ?", bookId, userId);
        Reservation reservation = toReservation(rows.get(rows.size() - 1));
        notifyChange("reservations");
        return reservation;
    }

    public void cancelReservation(int reservationId) {
        database.update("UPDATE reservations SET status = ? WHERE id = ?", "CANCELLED", reservationId);
        notifyChange("reservations");
    }

    public void approveReservation(int reservationId) {
        database.update("UPDATE reservations SET status = ? WHERE id = ?", "APPROVED", reservationId);
        notifyChange("reservations");
    }

    public void rejectReservation(int reservationId) {
        database.update("UPDATE reservations SET status = ? WHERE id = ?", "REJECTED", reservationId);
        notifyChange("reservations");
    }

    public void fulfilReservation(int reservationId) {
        database.update("UPDATE reservations SET status = ? WHERE id = ?", "FULFILLED", reservationId);
        notifyChange("reservations");
    }

    public record DataChange(Set<String> tables, long timestamp) {
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
        int totalCopies = row.get("total_copies") == null
                ? (getBoolean(row.get("available")) ? 1 : 0)
                : getInt(row.get("total_copies"));
        if (totalCopies < 0) {
            totalCopies = 0;
        }
        int availableCopies = row.get("available_copies") == null
                ? (getBoolean(row.get("available")) ? Math.max(1, totalCopies) : 0)
                : getInt(row.get("available_copies"));
        availableCopies = Math.max(0, Math.min(availableCopies, totalCopies));
        return new Book(((Number) row.get("id")).intValue(),
                (String) row.get("title"),
                (String) row.get("author"),
                (String) row.get("category"),
                totalCopies,
                availableCopies);
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

    private int getInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            return Integer.parseInt(text);
        }
        return 0;
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
