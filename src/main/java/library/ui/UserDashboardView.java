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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class UserDashboardView {
    private final User currentUser;
    private final LibraryService libraryService;
    private final Runnable onLogout;

    private final TableView<Book> bookTable = new TableView<>();
    private final TableView<Loan> loanTable = new TableView<>();
    private ReservationsPanel reservationsPanel;

    private final ObservableList<Book> masterBooks = FXCollections.observableArrayList();
    private final FilteredList<Book> filteredBooks = new FilteredList<>(masterBooks);
    private final ObservableList<Loan> masterLoans = FXCollections.observableArrayList();
    private TextField titleFilterField;
    private TextField authorFilterField;
    private ComboBox<String> categoryFilterBox;
    private CheckBox availableFilterCheck;
    private AutoCloseable changeSubscription;
    private Label activeLoanCountLabel;
    private Label pendingLoanCountLabel;

    public UserDashboardView(User currentUser, LibraryService libraryService, Runnable onLogout) {
        this.currentUser = Objects.requireNonNull(currentUser);
        this.libraryService = Objects.requireNonNull(libraryService);
        this.onLogout = Objects.requireNonNull(onLogout);
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(createHeader());
        root.setCenter(createContent());
        refreshAll();
        subscribeToChanges();
        return new Scene(root, 900, 620);
    }

    private BorderPane createHeader() {
        String displayName = currentUser.getFullName() == null || currentUser.getFullName().isBlank()
                ? currentUser.getUsername()
                : currentUser.getFullName();
        Label welcome = new Label("Xin chào, " + displayName);
        welcome.setGraphic(IconProvider.icon("reader", 18));
        welcome.setContentDisplay(ContentDisplay.LEFT);

        Button logout = new Button("Đăng xuất");
        applyButtonIcon(logout, "action-button");
        logout.setOnAction(event -> {
            cleanup();
            onLogout.run();
        });

        BorderPane header = new BorderPane();
        header.setLeft(welcome);
        header.setRight(logout);
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    private TabPane createContent() {
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(createBooksTab(), createLoansTab(), createReservationsTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabs;
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
        TableColumn<Book, Boolean> availableColumn = new TableColumn<>("Có sẵn");
        availableColumn.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isAvailable()).asObject());
        availableColumn.setCellFactory(column -> new TableCell<>() {
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

        bookTable.getColumns().setAll(idColumn, titleColumn, authorColumn, categoryColumn, quantityColumn, availableColumn);
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

        Button borrowButton = new Button("Mượn sách");
        Button reserveButton = new Button("Đặt trước");
        Button refreshButton = new Button("Làm mới");

        applyButtonIcon(borrowButton, "loan");
        applyButtonIcon(reserveButton, "reservation");
        applyButtonIcon(refreshButton, "refresh");

        borrowButton.setOnAction(event -> borrowSelectedBook());
        reserveButton.setOnAction(event -> reserveSelectedBook());
        refreshButton.setOnAction(event -> refreshBooks());

        HBox controls = new HBox(10, borrowButton, reserveButton, refreshButton);
        VBox container = new VBox(10, filterBar, bookTable, controls);
        container.setPadding(new Insets(12));
        VBox.setVgrow(bookTable, Priority.ALWAYS);
        Tab tab = new Tab("Danh mục", container);
        tab.setGraphic(IconProvider.icon("book", 18));
        return tab;
    }

    private Tab createLoansTab() {
        TableColumn<Loan, String> bookColumn = new TableColumn<>("Sách");
        bookColumn.setCellValueFactory(data -> new SimpleStringProperty(resolveBookTitle(data.getValue().getBookId())));
        TableColumn<Loan, String> loanDateColumn = new TableColumn<>("Ngày mượn");
        loanDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLoanDate().toString()));
        TableColumn<Loan, String> dueDateColumn = new TableColumn<>("Hạn trả");
        dueDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDueDate().toString()));
        TableColumn<Loan, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        TableColumn<Loan, String> feeColumn = new TableColumn<>("Phí");
        feeColumn.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.0f", data.getValue().getAccruedFee())));
        TableColumn<Loan, String> remainingColumn = new TableColumn<>("Ngày còn lại");
        remainingColumn.setCellValueFactory(data -> new SimpleStringProperty(formatRemainingDays(data.getValue())));

        loanDateColumn.setGraphic(IconProvider.icon("calendar", 16));
        dueDateColumn.setGraphic(IconProvider.icon("clock", 16));
        statusColumn.setGraphic(IconProvider.icon("overdue", 16));
        feeColumn.setGraphic(IconProvider.icon("report", 16));
        remainingColumn.setGraphic(IconProvider.icon("clock", 16));

        loanTable.getColumns().setAll(bookColumn, loanDateColumn, dueDateColumn, remainingColumn, statusColumn, feeColumn);
        loanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        loanTable.setItems(masterLoans);

        Button refreshButton = new Button("Làm mới");
        applyButtonIcon(refreshButton, "refresh");
        refreshButton.setOnAction(event -> refreshLoans());

        Button downloadButton = new Button("Tải xuống");
        applyButtonIcon(downloadButton, "export");
        downloadButton.setOnAction(event -> exportUserSnapshot());

        activeLoanCountLabel = new Label();
        activeLoanCountLabel.setGraphic(IconProvider.icon("loan", 16));
        activeLoanCountLabel.setContentDisplay(ContentDisplay.LEFT);

        pendingLoanCountLabel = new Label();
        pendingLoanCountLabel.setGraphic(IconProvider.icon("reservation", 16));
        pendingLoanCountLabel.setContentDisplay(ContentDisplay.LEFT);

        HBox controls = new HBox(10, refreshButton, downloadButton);

        VBox container = new VBox(10, loanTable, activeLoanCountLabel, pendingLoanCountLabel, controls);
        container.setPadding(new Insets(12));
        VBox.setVgrow(loanTable, Priority.ALWAYS);
        Tab tab = new Tab("Phiếu mượn", container);
        tab.setGraphic(IconProvider.icon("loan", 18));
        return tab;
    }

    private Tab createReservationsTab() {
        reservationsPanel = new ReservationsPanel(libraryService, this::refreshAll, false, currentUser.getId());
        Tab tab = new Tab("Đặt trước", reservationsPanel);
        tab.setGraphic(IconProvider.icon("reservation", 18));
        return tab;
    }

    private void borrowSelectedBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        libraryService.borrowBook(selected.getId(), currentUser);
        String message = selected.isAvailable()
                ? "Đã gửi yêu cầu mượn. Vui lòng chờ quản trị viên phê duyệt trước khi nhận sách."
                : "Sách hiện đã hết bản sao. Yêu cầu của bạn sẽ nằm trong hàng chờ và được xử lý khi có sách.";
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText("Đã gửi yêu cầu mượn");
        alert.showAndWait();
        refreshAll();
    }

    private void reserveSelectedBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        libraryService.reserveBook(selected.getId(), currentUser);
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Yêu cầu đặt trước đã được gửi và sẽ chờ quản trị viên xác nhận.");
        alert.setHeaderText("Đặt trước đang chờ duyệt");
        alert.showAndWait();
        refreshAll();
    }

    private String formatRemainingDays(Loan loan) {
        if (loan.isPending()) {
            return "Chờ duyệt";
        }
        if (loan.isRejected()) {
            return "Đã từ chối";
        }
        if (loan.isReturned()) {
            return "Đã trả";
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), loan.getDueDate());
        if (days > 0) {
            return days + " ngày";
        }
        if (days == 0) {
            return "Hạn hôm nay";
        }
        return "Trễ " + Math.abs(days) + " ngày";
    }

    private void refreshAll() {
        refreshBooks();
        refreshLoans();
        if (reservationsPanel != null) {
            reservationsPanel.refresh();
        }
    }

    private void subscribeToChanges() {
        cleanup();
        changeSubscription = libraryService.onDataChanged(change -> Platform.runLater(this::refreshAll));
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

    private void refreshBooks() {
        List<Book> books = libraryService.getAllBooks();
        masterBooks.setAll(books);
        updateCategoryFilterOptions();
        applyBookFilters();
    }

    private void refreshLoans() {
        List<Loan> loans = libraryService.getLoansForUser(currentUser.getId());
        List<Loan> visibleLoans = loans.stream()
                .filter(loan -> !loan.isPending())
                .collect(Collectors.toList());
        masterLoans.setAll(visibleLoans);
        long activeCount = visibleLoans.stream().filter(Loan::isActive).count();
        long pendingCount = loans.stream().filter(Loan::isPending).count();
        if (activeLoanCountLabel != null) {
            activeLoanCountLabel.setText("Đang mượn: " + activeCount);
        }
        if (pendingLoanCountLabel != null) {
            pendingLoanCountLabel.setText("Chờ duyệt: " + pendingCount);
        }
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

    private String resolveBookTitle(int bookId) {
        return masterBooks.stream()
                .filter(book -> book.getId() == bookId)
                .map(Book::getTitle)
                .findFirst()
                .orElse("#" + bookId);
    }

    private void applyButtonIcon(Button button, String iconName) {
        button.setGraphic(IconProvider.icon(iconName, 18));
        button.setContentDisplay(ContentDisplay.LEFT);
    }

    private void exportUserSnapshot() {
        FileChooser chooser = createCsvFileChooser(currentUser.getUsername() + "-lich-su.csv");
        File destination = chooser.showSaveDialog(resolveWindow(loanTable));
        if (destination == null) {
            return;
        }
        try {
            Path exported = libraryService.exportUserSnapshot(currentUser.getId(), destination.toPath());
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Đã lưu dữ liệu tại: " + exported.toAbsolutePath());
            alert.setHeaderText("Tải xuống thành công");
            alert.showAndWait();
        } catch (RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Không thể tải xuống: " + exception.getMessage());
            alert.setHeaderText("Lỗi");
            alert.showAndWait();
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
}
