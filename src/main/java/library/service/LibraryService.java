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

    public Book addBook(String title, String author, String category) {
        return databaseManager.createBook(title, author, category, true);
    }

    public void updateBook(Book book) {
        databaseManager.updateBook(book);
    }

    public void deleteBook(int bookId) {
        databaseManager.deleteBook(bookId);
    }

    public void setBookAvailability(int bookId, boolean available) {
        databaseManager.updateBookAvailability(bookId, available);
    }

    public List<Loan> getAllLoans() {
        return databaseManager.findAllLoans();
    }

    public List<Loan> getLoansForUser(int userId) {
        return databaseManager.findLoansByUser(userId);
    }

    public Loan borrowBook(int bookId, User user) {
        databaseManager.updateBookAvailability(bookId, false);
        LocalDate loanDate = LocalDate.now();
        LocalDate dueDate = loanDate.plusDays(DEFAULT_LOAN_PERIOD_DAYS);
        Loan loan = databaseManager.createLoan(bookId, user.getId(), loanDate, dueDate, DEFAULT_DAILY_FEE);
        for (Reservation reservation : databaseManager.findReservationsByUser(user.getId())) {
            if (reservation.getBookId() == bookId && reservation.isActive()) {
                databaseManager.fulfilReservation(reservation.getId());
            }
        }
        return loan;
    }

    public void returnBook(Loan loan) {
        databaseManager.markLoanReturned(loan.getId(), LocalDate.now());
        databaseManager.updateBookAvailability(loan.getBookId(), true);
    }

    public List<User> getAllUsers() {
        return databaseManager.findAllUsers();
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

    public void fulfilReservation(int reservationId) {
        databaseManager.fulfilReservation(reservationId);
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
