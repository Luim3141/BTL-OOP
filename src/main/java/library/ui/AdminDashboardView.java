package library.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import library.model.BorrowedBookSnapshot;
import library.model.Loan;
import library.model.User;
import library.service.LibraryService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AdminDashboardView {
    private final User currentUser;
    private final LibraryService libraryService;
    private final Runnable onLogout;

    private final TableView<Book> bookTable = new TableView<>();
    private final TableView<User> userTable = new TableView<>();
    private final TableView<Loan> loanTable = new TableView<>();
    private final TableView<BorrowedBookSnapshot> borrowSummaryTable = new TableView<>();
    private ReservationsPanel reservationsPanel;

    private final ObservableList<Book> masterBooks = FXCollections.observableArrayList();
    private final FilteredList<Book> filteredBooks = new FilteredList<>(masterBooks);
    private final ObservableList<User> masterUsers = FXCollections.observableArrayList();
    private final ObservableList<Loan> masterLoans = FXCollections.observableArrayList();
    private final FilteredList<Loan> filteredLoans = new FilteredList<>(masterLoans);
    private final ObservableList<BorrowedBookSnapshot> borrowSummaries = FXCollections.observableArrayList();
    private TextField titleFilterField;
    private TextField authorFilterField;
    private ComboBox<String> categoryFilterBox;
    private CheckBox availableFilterCheck;
    private ComboBox<String> loanStatusFilter;
    private AutoCloseable changeSubscription;
    private Map<Integer, Long> activeLoanCountByUser = Map.of();
    private Label borrowSummaryTotalLabel;

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
        subscribeToChanges();
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
        logoutButton.setOnAction(event -> {
            cleanup();
            onLogout.run();
        });

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
                createBorrowedSummaryTab(),
                createReservationsTab(),
                createReportsTab());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabPane;
    }

    private Tab createBooksTab() {
        TableColumn<Book, String> idColumn = new TableColumn<>("Mã");
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        TableColumn<Book, String> titleColumn = new TableColumn<>("Tiêu đề");
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        TableColumn<Book, String> authorColumn = new TableColumn<>("Tác giả");
        authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        TableColumn<Book, String> categoryColumn = new TableColumn<>("Thể loại");
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        TableColumn<Book, String> quantityColumn = new TableColumn<>("Tồn kho");
        quantityColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAvailableCopies() + "/" + data.getValue().getTotalCopies()));
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

        bookTable.getColumns().setAll(idColumn, titleColumn, authorColumn, categoryColumn, quantityColumn, availabilityColumn);
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bookTable.setItems(filteredBooks);

        titleFilterField = new TextField();
        titleFilterField.setPromptText("Tiêu đề");
        titleFilterField.setPrefWidth(160);
        titleFilterField.textProperty().addListener((obs, old, value) -> applyBookFilters());

        authorFilterField = new TextField();
        authorFilterField.setPromptText("Tác giả");
        authorFilterField.setPrefWidth(160);
        authorFilterField.textProperty().addListener((obs, old, value) -> applyBookFilters());

        categoryFilterBox = new ComboBox<>();
        categoryFilterBox.setPromptText("Thể loại");
        categoryFilterBox.setMinWidth(140);
        categoryFilterBox.valueProperty().addListener((obs, old, value) -> applyBookFilters());

        availableFilterCheck = new CheckBox("Còn sách");
        availableFilterCheck.selectedProperty().addListener((obs, old, value) -> applyBookFilters());

        HBox filterBar = new HBox(10,
                new Label("Lọc:"),
                titleFilterField,
                authorFilterField,
                categoryFilterBox,
                availableFilterCheck);
        filterBar.setPadding(new Insets(0, 0, 8, 0));
        updateCategoryFilterOptions();
        applyBookFilters();

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

        VBox container = new VBox(10, filterBar, bookTable, controls);
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

        int initialTotal = book == null ? 1 : book.getTotalCopies();
        if (initialTotal < 0) {
            initialTotal = 0;
        }
        Spinner<Integer> totalSpinner = new Spinner<>();
        SpinnerValueFactory.IntegerSpinnerValueFactory totalFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, initialTotal);
        totalSpinner.setValueFactory(totalFactory);
        totalSpinner.setEditable(true);

        int initialAvailable = book == null ? initialTotal : book.getAvailableCopies();
        initialAvailable = Math.max(0, Math.min(initialAvailable, initialTotal));
        Spinner<Integer> availableSpinner = new Spinner<>();
        SpinnerValueFactory.IntegerSpinnerValueFactory availableFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, initialTotal, initialAvailable);
        availableSpinner.setValueFactory(availableFactory);
        availableSpinner.setEditable(true);

        totalFactory.valueProperty().addListener((obs, oldValue, newValue) -> {
            int newTotal = newValue == null ? 0 : newValue;
            availableFactory.setMax(newTotal);
            if (availableSpinner.getValue() > newTotal) {
                availableSpinner.getValueFactory().setValue(newTotal);
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Tiêu đề"), titleField);
        grid.addRow(1, new Label("Tác giả"), authorField);
        grid.addRow(2, new Label("Thể loại"), categoryField);
        grid.addRow(3, new Label("Tổng số bản"), totalSpinner);
        grid.addRow(4, new Label("Đang có"), availableSpinner);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                String category = categoryField.getText().trim();
                int total = totalSpinner.getValue();
                int available = Math.min(availableSpinner.getValue(), total);
                if (title.isEmpty() || author.isEmpty()) {
                    return null;
                }
                if (book == null) {
                    libraryService.addBook(title, author, category, total, available);
                } else {
                    libraryService.updateBook(book.withDetails(title, author, category, total, available));
                }
                return book;
            }
            return null;
        });

        dialog.showAndWait();
        refreshAllTables();
    }

    private Tab createUsersTab() {
        TableColumn<User, String> idColumn = new TableColumn<>("Mã");
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        TableColumn<User, String> usernameColumn = new TableColumn<>("Tên đăng nhập");
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        TableColumn<User, String> passwordColumn = new TableColumn<>("Mật khẩu");
        passwordColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPassword()));
        TableColumn<User, String> roleColumn = new TableColumn<>("Vai trò");
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        TableColumn<User, String> activeLoansColumn = new TableColumn<>("Đang mượn");
        activeLoansColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(
                activeLoanCountByUser.getOrDefault(data.getValue().getId(), 0L))));
        TableColumn<User, String> fullNameColumn = new TableColumn<>("Họ tên");
        fullNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        TableColumn<User, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));

        userTable.getColumns().setAll(idColumn, usernameColumn, passwordColumn, roleColumn, activeLoansColumn,
                fullNameColumn, emailColumn);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        userTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        userTable.setItems(masterUsers);

        Button deleteButton = new Button("Xóa tài khoản");
        Button refreshButton = new Button("Làm mới");
        applyIcon(deleteButton, "remove");
        applyIcon(refreshButton, "refresh");
        deleteButton.setOnAction(event -> deleteSelectedUsers());
        refreshButton.setOnAction(event -> refreshUsers());

        HBox controls = new HBox(10, deleteButton, refreshButton);
        VBox container = new VBox(10, userTable, controls);
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
        loanTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        loanTable.setItems(filteredLoans);

        loanStatusFilter = new ComboBox<>();
        loanStatusFilter.getItems().setAll(
                "Chờ duyệt",
                "Đang mượn",
                "Quá hạn",
                "Đã trả",
                "Đã từ chối",
                "Tất cả");
        loanStatusFilter.setValue("Chờ duyệt");
        loanStatusFilter.valueProperty().addListener((obs, old, value) -> applyLoanFilters());

        HBox filterBar = new HBox(10, new Label("Hiển thị:"), loanStatusFilter);
        filterBar.setPadding(new Insets(0, 0, 8, 0));

        Button approveButton = new Button("Duyệt đã chọn");
        Button rejectButton = new Button("Từ chối đã chọn");
        Button markReturnedButton = new Button("Đã trả");
        Button refreshButton = new Button("Làm mới");

        applyIcon(approveButton, "add");
        applyIcon(rejectButton, "remove");
        applyIcon(markReturnedButton, "loan");
        applyIcon(refreshButton, "refresh");

        approveButton.setOnAction(event -> approveSelectedLoans());
        rejectButton.setOnAction(event -> rejectSelectedLoans());
        markReturnedButton.setOnAction(event -> markSelectedReturned());
        refreshButton.setOnAction(event -> refreshLoans());

        HBox controls = new HBox(10, approveButton, rejectButton, markReturnedButton, refreshButton);
        VBox container = new VBox(10, filterBar, loanTable, controls);
        container.setPadding(new Insets(12));
        VBox.setVgrow(loanTable, Priority.ALWAYS);
        Tab tab = new Tab("Phiếu mượn", container);
        tab.setGraphic(IconProvider.icon("loan", 18));
        return tab;
    }

    private Tab createBorrowedSummaryTab() {
        TableColumn<BorrowedBookSnapshot, String> loanIdColumn = new TableColumn<>("Phiếu");
        loanIdColumn.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getLoanId()));
        TableColumn<BorrowedBookSnapshot, String> userColumn = new TableColumn<>("Người dùng");
        userColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        TableColumn<BorrowedBookSnapshot, String> bookColumn = new TableColumn<>("Sách");
        bookColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBookTitle()));
        TableColumn<BorrowedBookSnapshot, String> loanDateColumn = new TableColumn<>("Ngày mượn");
        loanDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLoanDate().toString()));
        TableColumn<BorrowedBookSnapshot, String> dueDateColumn = new TableColumn<>("Hạn trả");
        dueDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDueDate().toString()));
        TableColumn<BorrowedBookSnapshot, String> remainingColumn = new TableColumn<>("Ngày còn lại");
        remainingColumn.setCellValueFactory(data -> new SimpleStringProperty(formatRemainingDays(data.getValue())));
        TableColumn<BorrowedBookSnapshot, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        TableColumn<BorrowedBookSnapshot, String> stockColumn = new TableColumn<>("Bản còn lại");
        stockColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getRemainingCopies())));

        borrowSummaryTable.getColumns().setAll(loanIdColumn, userColumn, bookColumn, loanDateColumn, dueDateColumn,
                remainingColumn, statusColumn, stockColumn);
        borrowSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        borrowSummaryTable.setItems(borrowSummaries);

        Label infoLabel = new Label("Theo dõi các phiếu mượn đang hoạt động kèm tồn kho thực tế.");
        infoLabel.setGraphic(IconProvider.icon("analytics", 16));
        infoLabel.setContentDisplay(ContentDisplay.LEFT);

        borrowSummaryTotalLabel = new Label();
        borrowSummaryTotalLabel.setGraphic(IconProvider.icon("loan", 16));
        borrowSummaryTotalLabel.setContentDisplay(ContentDisplay.LEFT);

        VBox container = new VBox(10, infoLabel, borrowSummaryTable, borrowSummaryTotalLabel);
        container.setPadding(new Insets(12));
        VBox.setVgrow(borrowSummaryTable, Priority.ALWAYS);
        Tab tab = new Tab("Đang mượn", container);
        tab.setGraphic(IconProvider.icon("analytics", 18));
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

    private void updateCategoryFilterOptions() {
        if (categoryFilterBox == null) {
            return;
        }
        String previous = categoryFilterBox.getValue();
        Set<String> categories = masterBooks.stream()
                .map(Book::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        categoryFilterBox.getItems().setAll("Tất cả");
        categoryFilterBox.getItems().addAll(categories);
        if (previous != null && categoryFilterBox.getItems().contains(previous)) {
            categoryFilterBox.setValue(previous);
        } else {
            categoryFilterBox.setValue("Tất cả");
        }
    }

    private void applyBookFilters() {
        if (titleFilterField == null) {
            return;
        }
        String titleKeyword = normalise(titleFilterField.getText());
        String authorKeyword = normalise(authorFilterField.getText());
        String selectedCategory = categoryFilterBox == null ? null : categoryFilterBox.getValue();
        boolean availableOnly = availableFilterCheck != null && availableFilterCheck.isSelected();

        filteredBooks.setPredicate(book -> {
            if (!titleKeyword.isEmpty() && !containsIgnoreCase(book.getTitle(), titleKeyword)) {
                return false;
            }
            if (!authorKeyword.isEmpty() && !containsIgnoreCase(book.getAuthor(), authorKeyword)) {
                return false;
            }
            if (selectedCategory != null && !selectedCategory.equals("Tất cả")) {
                if (book.getCategory() == null || !book.getCategory().equalsIgnoreCase(selectedCategory)) {
                    return false;
                }
            }
            if (availableOnly && !book.isAvailable()) {
                return false;
            }
            return true;
        });
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String formatRemainingDays(BorrowedBookSnapshot snapshot) {
        long days = snapshot.getDaysRemaining();
        if (days > 0) {
            return days + " ngày";
        }
        if (days == 0) {
            return "Hạn hôm nay";
        }
        return "Trễ " + Math.abs(days) + " ngày";
    }

    private void refreshAllTables() {
        refreshBooks();
        refreshUsers();
        refreshLoans();
        if (reservationsPanel != null) {
            reservationsPanel.refresh();
        }
    }

    private void subscribeToChanges() {
        cleanup();
        changeSubscription = libraryService.onDataChanged(change -> Platform.runLater(this::refreshAllTables));
    }

    private void cleanup() {
        if (changeSubscription != null) {
            try {
                changeSubscription.close();
            } catch (Exception ignored) {
            }
            changeSubscription = null;
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
        masterBooks.setAll(books);
        updateCategoryFilterOptions();
        applyBookFilters();
    }

    private void refreshUsers() {
        refreshBorrowSummaries();
        List<User> users = libraryService.getAllUsers();
        masterUsers.setAll(users);
        userTable.refresh();
    }

    private void refreshLoans() {
        List<Loan> loans = libraryService.getAllLoans();
        masterLoans.setAll(loans);
        applyLoanFilters();
    }

    private void refreshBorrowSummaries() {
        List<BorrowedBookSnapshot> snapshots = libraryService.getAllBorrowedBooks();
        borrowSummaries.setAll(snapshots);
        activeLoanCountByUser = snapshots.stream()
                .collect(Collectors.groupingBy(BorrowedBookSnapshot::getUserId, Collectors.counting()));
        if (borrowSummaryTotalLabel != null) {
            borrowSummaryTotalLabel.setText("Tổng phiếu đang hoạt động: " + snapshots.size());
        }
        userTable.refresh();
    }

    private void approveSelectedLoans() {
        List<Loan> selected = new ArrayList<>(loanTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        int approved = 0;
        List<String> errors = new ArrayList<>();
        for (Loan loan : selected) {
            if (!loan.isPending()) {
                continue;
            }
            try {
                libraryService.approveLoan(loan.getId());
                approved++;
            } catch (RuntimeException exception) {
                errors.add("#" + loan.getId() + ": " + exception.getMessage());
            }
        }
        loanTable.getSelectionModel().clearSelection();
        refreshAllTables();
        if (approved > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Đã duyệt " + approved + " yêu cầu mượn.");
            alert.setHeaderText("Thành công");
            alert.showAndWait();
        }
        if (!errors.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    String.join("\n", errors));
            alert.setHeaderText("Một số yêu cầu không thể duyệt");
            alert.showAndWait();
        }
    }

    private void rejectSelectedLoans() {
        List<Loan> selected = new ArrayList<>(loanTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        int rejected = 0;
        for (Loan loan : selected) {
            if (loan.isPending()) {
                libraryService.rejectLoan(loan.getId());
                rejected++;
            }
        }
        loanTable.getSelectionModel().clearSelection();
        refreshAllTables();
        if (rejected > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Đã từ chối " + rejected + " yêu cầu mượn.");
            alert.setHeaderText("Đã cập nhật");
            alert.showAndWait();
        }
    }

    private void markSelectedReturned() {
        List<Loan> selected = new ArrayList<>(loanTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        int returned = 0;
        for (Loan loan : selected) {
            if (!loan.isPending() && !loan.isRejected() && !loan.isReturned()) {
                libraryService.returnBook(loan);
                returned++;
            }
        }
        loanTable.getSelectionModel().clearSelection();
        refreshAllTables();
        if (returned > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Đã ghi nhận " + returned + " phiếu mượn đã trả.");
            alert.setHeaderText("Hoàn tất");
            alert.showAndWait();
        }
    }

    private void deleteSelectedUsers() {
        List<User> selected = new ArrayList<>(userTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        selected.removeIf(user -> user.getId() == currentUser.getId());
        if (selected.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Không thể tự xóa tài khoản quản trị đang đăng nhập.");
            alert.setHeaderText("Thao tác bị chặn");
            alert.showAndWait();
            return;
        }
        String joined = selected.stream()
                .map(User::getUsername)
                .collect(Collectors.joining(", "));
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn xóa các tài khoản: " + joined + "?");
        confirm.setHeaderText("Xác nhận xóa tài khoản");
        confirm.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> {
                    for (User user : selected) {
                        libraryService.deleteUser(user.getId());
                    }
                    refreshAllTables();
                });
    }

    private void applyLoanFilters() {
        if (loanStatusFilter == null) {
            return;
        }
        String selected = loanStatusFilter.getValue();
        filteredLoans.setPredicate(loan -> {
            if (selected == null || selected.equals("Tất cả")) {
                return true;
            }
            return switch (selected) {
                case "Chờ duyệt" -> loan.isPending();
                case "Đang mượn" -> "BORROWED".equalsIgnoreCase(loan.getStatus());
                case "Quá hạn" -> "OVERDUE".equalsIgnoreCase(loan.getStatus());
                case "Đã trả" -> loan.isReturned();
                case "Đã từ chối" -> loan.isRejected();
                default -> true;
            };
        });
    }

    private String resolveBookTitle(int bookId) {
        return masterBooks.stream()
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
