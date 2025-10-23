package library.service;

import library.model.Book;
import library.model.Loan;
import library.model.Reservation;
import library.model.User;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

public class LibraryService {
    private static final int DEFAULT_LOAN_PERIOD_DAYS = 14;
    private static final double DEFAULT_DAILY_FEE = 5000.0;

    private final DatabaseManager databaseManager;

    public LibraryService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Book> getAllBooks() {
        return databaseManager.findAllBooks();
    }

    public Book addBook(String title, String author, String category, int totalCopies, int availableCopies) {
        if (totalCopies < 0) {
            throw new IllegalArgumentException("Tổng số bản phải không âm");
        }
        if (availableCopies < 0 || availableCopies > totalCopies) {
            throw new IllegalArgumentException("Số bản còn lại không hợp lệ");
        }
        return databaseManager.createBook(title, author, category, totalCopies, availableCopies);
    }

    public void updateBook(Book book) {
        if (book.getTotalCopies() < 0) {
            throw new IllegalArgumentException("Tổng số bản phải không âm");
        }
        if (book.getAvailableCopies() < 0 || book.getAvailableCopies() > book.getTotalCopies()) {
            throw new IllegalArgumentException("Số bản còn lại không hợp lệ");
        }
        databaseManager.updateBook(book);
    }

    public void deleteBook(int bookId) {
        databaseManager.deleteBook(bookId);
    }

    public List<Loan> getAllLoans() {
        return databaseManager.findAllLoans();
    }

    public List<Loan> getLoansForUser(int userId) {
        return databaseManager.findLoansByUser(userId);
    }

    public Loan borrowBook(int bookId, User user) {
        Book book = databaseManager.findBookById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Không tìm thấy sách với id=" + bookId);
        }
        return databaseManager.createBorrowRequest(bookId, user.getId(), LocalDate.now(), DEFAULT_LOAN_PERIOD_DAYS, DEFAULT_DAILY_FEE);
    }

    public void returnBook(Loan loan) {
        databaseManager.markLoanReturned(loan.getId(), LocalDate.now());
    }

    public Loan approveLoan(int loanId) {
        Loan loan = databaseManager.approveLoan(loanId, DEFAULT_LOAN_PERIOD_DAYS);
        if (loan != null && loan.isActive()) {
            for (Reservation reservation : databaseManager.findReservationsByUser(loan.getUserId())) {
                if (reservation.getBookId() == loan.getBookId() && reservation.isApproved()) {
                    databaseManager.fulfilReservation(reservation.getId());
                }
            }
        }
        return loan;
    }

    public Loan rejectLoan(int loanId) {
        return databaseManager.rejectLoan(loanId);
    }

    public List<User> getAllUsers() {
        return databaseManager.findAllUsers();
    }

    public void deleteUser(int userId) {
        databaseManager.deleteUser(userId);
    }

    public List<Reservation> getAllReservations() {
        return databaseManager.findAllReservations();
    }

    public List<Reservation> getReservationsForUser(int userId) {
        return databaseManager.findReservationsByUser(userId);
    }

    public Reservation reserveBook(int bookId, User user) {
        return databaseManager.createReservation(bookId, user.getId(), LocalDate.now());
    }

    public void cancelReservation(int reservationId) {
        databaseManager.cancelReservation(reservationId);
    }

    public void approveReservation(int reservationId) {
        databaseManager.approveReservation(reservationId);
    }

    public void rejectReservation(int reservationId) {
        databaseManager.rejectReservation(reservationId);
    }

    public void fulfilReservation(int reservationId) {
        databaseManager.fulfilReservation(reservationId);
    }

    public AutoCloseable onDataChanged(Consumer<DatabaseManager.DataChange> listener) {
        return databaseManager.addChangeListener(listener);
    }

    public Path exportReport(Path directory) {
        try {
            Files.createDirectories(directory);
            String fileName = "library-report-" + LocalDate.now() + ".csv";
            Path reportFile = directory.resolve(fileName);
            return exportReportToFile(reportFile);
        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất báo cáo", e);
        }
    }

    public Path exportReportToFile(Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            List<Book> books = getAllBooks();
            List<User> users = getAllUsers();
            List<Loan> loans = getAllLoans();
            List<Reservation> reservations = getAllReservations();
            long availableBooks = books.stream().filter(Book::isAvailable).count();
            long activeLoans = loans.stream().filter(loan -> !"RETURNED".equalsIgnoreCase(loan.getStatus())).count();
            long overdueLoans = loans.stream().filter(loan -> "OVERDUE".equalsIgnoreCase(loan.getStatus())).count();
            double totalFees = loans.stream().mapToDouble(Loan::getAccruedFee).sum();
            long activeReservations = reservations.stream().filter(Reservation::isActive).count();

            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                writer.write("Section,Metric,Value\n");
                writer.write("Books,Total," + books.size() + "\n");
                writer.write("Books,Available," + availableBooks + "\n");
                writer.write("Users,Total," + users.size() + "\n");
                writer.write("Loans,Active," + activeLoans + "\n");
                writer.write("Loans,Overdue," + overdueLoans + "\n");
                writer.write("Loans,TotalFees," + totalFees + "\n");
                writer.write("Reservations,Active," + activeReservations + "\n");
                writer.write("\n");
                writer.write("Loan ID,Book ID,User ID,Loan Date,Due Date,Status,Fee\n");
                for (Loan loan : loans) {
                    writer.write(String.join(",",
                            String.valueOf(loan.getId()),
                            String.valueOf(loan.getBookId()),
                            String.valueOf(loan.getUserId()),
                            loan.getLoanDate().toString(),
                            loan.getDueDate().toString(),
                            loan.getStatus(),
                            String.valueOf(loan.getAccruedFee())));
                    writer.write("\n");
                }
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất báo cáo", e);
        }
    }

    public Path exportUserSnapshot(int userId, Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            User user = databaseManager.findUserById(userId);
            if (user == null) {
                throw new IllegalArgumentException("Không tìm thấy người dùng với id=" + userId);
            }
            List<Loan> loans = databaseManager.findLoansByUser(userId);
            List<Reservation> reservations = databaseManager.findReservationsByUser(userId);
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                writer.write("Thông tin người dùng\n");
                writer.write("Tài khoản," + user.getUsername() + "\n");
                writer.write("Họ tên," + safe(user.getFullName()) + "\n");
                writer.write("Email," + safe(user.getEmail()) + "\n\n");

                writer.write("Phiếu mượn\n");
                writer.write("Loan ID,Book ID,Loan Date,Due Date,Status,Fee\n");
                for (Loan loan : loans) {
                    writer.write(String.join(",",
                            String.valueOf(loan.getId()),
                            String.valueOf(loan.getBookId()),
                            loan.getLoanDate().toString(),
                            loan.getDueDate().toString(),
                            loan.getStatus(),
                            String.valueOf(loan.getAccruedFee())));
                    writer.write("\n");
                }

                writer.write("\nĐặt trước\n");
                writer.write("Reservation ID,Book ID,Reservation Date,Status\n");
                for (Reservation reservation : reservations) {
                    writer.write(String.join(",",
                            String.valueOf(reservation.getId()),
                            String.valueOf(reservation.getBookId()),
                            reservation.getReservationDate().toString(),
                            reservation.getStatus()));
                    writer.write("\n");
                }
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất dữ liệu người dùng", e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
