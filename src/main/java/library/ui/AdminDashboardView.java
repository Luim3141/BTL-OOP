package library.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import library.model.Book;
import library.model.Loan;
import library.model.User;
import library.service.LibraryService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class AdminDashboardView {
    private final User currentUser;
    private final LibraryService libraryService;
    private final Runnable onLogout;

    private final TableView<Book> bookTable = new TableView<>();
    private final TableView<User> userTable = new TableView<>();
    private final TableView<Loan> loanTable = new TableView<>();
    private ReservationsPanel reservationsPanel;

    public AdminDashboardView(User currentUser, LibraryService libraryService, Runnable onLogout) {
        this.currentUser = Objects.requireNonNull(currentUser);
        this.libraryService = Objects.requireNonNull(libraryService);
        this.onLogout = Objects.requireNonNull(onLogout);
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(createHeader());
        root.setCenter(createContent());
        refreshAllTables();
        return new Scene(root, 980, 640);
    }

    private BorderPane createHeader() {
        String displayName = currentUser.getFullName() == null || currentUser.getFullName().isBlank()
                ? currentUser.getUsername()
                : currentUser.getFullName();
        Label welcomeLabel = new Label("Xin chào, " + displayName + " (Quản trị)");
        welcomeLabel.setGraphic(IconProvider.icon("dashboard", 18));
        welcomeLabel.setContentDisplay(ContentDisplay.LEFT);

        Button logoutButton = new Button("Đăng xuất");
        applyIcon(logoutButton, "action-button");
        logoutButton.setOnAction(event -> onLogout.run());

        BorderPane borderPane = new BorderPane();
        borderPane.setLeft(welcomeLabel);
        borderPane.setRight(logoutButton);
        borderPane.setPadding(new Insets(0, 0, 12, 0));
        return borderPane;
    }

    private TabPane createContent() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                createBooksTab(),
                createUsersTab(),
                createLoansTab(),
                createReservationsTab(),
                createReportsTab());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabPane;
    }

    private Tab createBooksTab() {
        TableColumn<Book, String> titleColumn = new TableColumn<>("Tiêu đề");
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        TableColumn<Book, String> authorColumn = new TableColumn<>("Tác giả");
        authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        TableColumn<Book, String> categoryColumn = new TableColumn<>("Thể loại");
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        TableColumn<Book, Boolean> availabilityColumn = new TableColumn<>("Có sẵn");
        availabilityColumn.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isAvailable()).asObject());
        availabilityColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Có" : "Không");
                }
            }
        });

        bookTable.getColumns().setAll(titleColumn, authorColumn, categoryColumn, availabilityColumn);
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        Button addButton = new Button("Thêm sách");
        Button editButton = new Button("Sửa");
        Button deleteButton = new Button("Xóa");
        Button refreshButton = new Button("Làm mới");

        applyIcon(addButton, "add");
        applyIcon(editButton, "pencil");
        applyIcon(deleteButton, "remove");
        applyIcon(refreshButton, "refresh");

        addButton.setOnAction(event -> showBookDialog(null));
        editButton.setOnAction(event -> {
            Book selected = bookTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showBookDialog(selected);
            }
        });
        deleteButton.setOnAction(event -> {
            Book selected = bookTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Bạn chắc chắn muốn xóa sách '" + selected.getTitle() + "'?");
            confirm.setHeaderText("Xác nhận");
            confirm.showAndWait().filter(button -> button == ButtonType.OK)
                    .ifPresent(button -> {
                        libraryService.deleteBook(selected.getId());
                        refreshAllTables();
                    });
        });
        refreshButton.setOnAction(event -> refreshAllTables());

        HBox controls = new HBox(8, addButton, editButton, deleteButton, refreshButton);
        VBox.setMargin(controls, new Insets(12, 0, 0, 0));

        VBox container = new VBox(10, bookTable, controls);
        container.setPadding(new Insets(12));
        VBox.setVgrow(bookTable, Priority.ALWAYS);
        Tab tab = new Tab("Sách", container);
        tab.setGraphic(IconProvider.icon("book", 18));
        return tab;
    }

    private void showBookDialog(Book book) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(book == null ? "Thêm sách" : "Cập nhật sách");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField(book == null ? "" : book.getTitle());
        TextField authorField = new TextField(book == null ? "" : book.getAuthor());
        TextField categoryField = new TextField(book == null ? "" : book.getCategory());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Tiêu đề"), titleField);
        grid.addRow(1, new Label("Tác giả"), authorField);
        grid.addRow(2, new Label("Thể loại"), categoryField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                String category = categoryField.getText().trim();
                if (title.isEmpty() || author.isEmpty()) {
                    return null;
                }
                if (book == null) {
                    libraryService.addBook(title, author, category);
                } else {
                    libraryService.updateBook(book.withDetails(title, author, category));
                }
                return book;
            }
            return null;
        });

        dialog.showAndWait();
        refreshAllTables();
    }

    private Tab createUsersTab() {
        TableColumn<User, String> usernameColumn = new TableColumn<>("Tên đăng nhập");
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        TableColumn<User, String> roleColumn = new TableColumn<>("Vai trò");
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        TableColumn<User, String> fullNameColumn = new TableColumn<>("Họ tên");
        fullNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        TableColumn<User, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));

        userTable.getColumns().setAll(usernameColumn, roleColumn, fullNameColumn, emailColumn);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        Button refreshButton = new Button("Làm mới");
        applyIcon(refreshButton, "refresh");
        refreshButton.setOnAction(event -> refreshUsers());

        VBox container = new VBox(10, userTable, refreshButton);
        container.setPadding(new Insets(12));
        VBox.setVgrow(userTable, Priority.ALWAYS);
        Tab tab = new Tab("Người dùng", container);
        tab.setGraphic(IconProvider.icon("users", 18));
        return tab;
    }

    private Tab createLoansTab() {
        TableColumn<Loan, String> idColumn = new TableColumn<>("Mã");
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        TableColumn<Loan, String> bookColumn = new TableColumn<>("Sách");
        bookColumn.setCellValueFactory(data -> new SimpleStringProperty(resolveBookTitle(data.getValue().getBookId())));
        TableColumn<Loan, String> userColumn = new TableColumn<>("Người mượn");
        userColumn.setCellValueFactory(data -> new SimpleStringProperty(resolveUsername(data.getValue().getUserId())));
        TableColumn<Loan, String> loanDateColumn = new TableColumn<>("Ngày mượn");
        loanDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLoanDate().toString()));
        TableColumn<Loan, String> dueDateColumn = new TableColumn<>("Hạn trả");
        dueDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDueDate().toString()));
        TableColumn<Loan, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        TableColumn<Loan, String> feeColumn = new TableColumn<>("Phí");
        feeColumn.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.0f", data.getValue().getAccruedFee())));

        loanDateColumn.setGraphic(IconProvider.icon("calendar", 16));
        dueDateColumn.setGraphic(IconProvider.icon("clock", 16));
        statusColumn.setGraphic(IconProvider.icon("overdue", 16));
        feeColumn.setGraphic(IconProvider.icon("report", 16));

        loanTable.getColumns().setAll(idColumn, bookColumn, userColumn, loanDateColumn, dueDateColumn, statusColumn, feeColumn);
        loanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        Button markReturnedButton = new Button("Đánh dấu đã trả");
        Button refreshButton = new Button("Làm mới");

        applyIcon(markReturnedButton, "loan");
        applyIcon(refreshButton, "refresh");

        markReturnedButton.setOnAction(event -> {
            Loan selected = loanTable.getSelectionModel().getSelectedItem();
            if (selected == null || "RETURNED".equalsIgnoreCase(selected.getStatus())) {
                return;
            }
            libraryService.returnBook(selected);
            refreshAllTables();
        });
        refreshButton.setOnAction(event -> refreshLoans());

        HBox controls = new HBox(10, markReturnedButton, refreshButton);
        VBox container = new VBox(10, loanTable, controls);
        container.setPadding(new Insets(12));
        VBox.setVgrow(loanTable, Priority.ALWAYS);
        Tab tab = new Tab("Phiếu mượn", container);
        tab.setGraphic(IconProvider.icon("loan", 18));
        return tab;
    }

    private Tab createReservationsTab() {
        reservationsPanel = new ReservationsPanel(libraryService, this::refreshAllTables, true);
        Tab tab = new Tab("Đặt trước", reservationsPanel);
        tab.setGraphic(IconProvider.icon("reservation", 18));
        return tab;
    }

    private Tab createReportsTab() {
        Button exportButton = new Button("Xuất báo cáo CSV");
        applyIcon(exportButton, "export");
        Label infoLabel = new Label("Báo cáo tổng hợp gồm số liệu sách, người dùng, phiếu mượn và đặt trước.");
        infoLabel.setGraphic(IconProvider.icon("analytics", 18));
        infoLabel.setContentDisplay(ContentDisplay.LEFT);
        exportButton.setOnAction(event -> {
            FileChooser chooser = createCsvFileChooser("library-report-" + LocalDate.now() + ".csv");
            File destination = chooser.showSaveDialog(resolveWindow(exportButton));
            if (destination == null) {
                return;
            }
            try {
                Path reportPath = libraryService.exportReportToFile(destination.toPath());
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Đã xuất báo cáo: " + reportPath.toAbsolutePath());
                alert.setHeaderText("Xuất báo cáo thành công");
                alert.showAndWait();
            } catch (RuntimeException exception) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Không thể xuất báo cáo: " + exception.getMessage());
                alert.setHeaderText("Lỗi");
                alert.showAndWait();
            }
        });
        VBox box = new VBox(12, infoLabel, exportButton);
        box.setPadding(new Insets(16));
        Tab tab = new Tab("Báo cáo", box);
        tab.setGraphic(IconProvider.icon("report", 18));
        return tab;
    }

    private void applyIcon(Button button, String iconName) {
        button.setGraphic(IconProvider.icon(iconName, 18));
        button.setContentDisplay(ContentDisplay.LEFT);
    }

    private void refreshAllTables() {
        refreshBooks();
        refreshUsers();
        refreshLoans();
        if (reservationsPanel != null) {
            reservationsPanel.refresh();
        }
    }

    private FileChooser createCsvFileChooser(String defaultFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu file CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        chooser.setInitialFileName(defaultFileName);
        File home = new File(System.getProperty("user.home", "."));
        if (home.isDirectory()) {
            chooser.setInitialDirectory(home);
        }
        return chooser;
    }

    private Window resolveWindow(Control control) {
        return control.getScene() == null ? null : control.getScene().getWindow();
    }

    private void refreshBooks() {
        List<Book> books = libraryService.getAllBooks();
        bookTable.setItems(FXCollections.observableArrayList(books));
    }

    private void refreshUsers() {
        List<User> users = libraryService.getAllUsers();
        userTable.setItems(FXCollections.observableArrayList(users));
    }

    private void refreshLoans() {
        List<Loan> loans = libraryService.getAllLoans();
        loanTable.setItems(FXCollections.observableArrayList(loans));
    }

    private String resolveBookTitle(int bookId) {
        return libraryService.getAllBooks().stream()
                .filter(book -> book.getId() == bookId)
                .map(Book::getTitle)
                .findFirst()
                .orElse("#" + bookId);
    }

    private String resolveUsername(int userId) {
        return libraryService.getAllUsers().stream()
                .filter(user -> user.getId() == userId)
                .map(User::getUsername)
                .findFirst()
                .orElse("#" + userId);
    }
}
