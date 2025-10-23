package library.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
import java.util.List;
import java.util.Objects;

public class UserDashboardView {
    private final User currentUser;
    private final LibraryService libraryService;
    private final Runnable onLogout;

    private final TableView<Book> bookTable = new TableView<>();
    private final TableView<Loan> loanTable = new TableView<>();
    private ReservationsPanel reservationsPanel;

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
        logout.setOnAction(event -> onLogout.run());

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
        TableColumn<Book, String> titleColumn = new TableColumn<>("Tiêu đề");
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        TableColumn<Book, String> authorColumn = new TableColumn<>("Tác giả");
        authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        TableColumn<Book, String> categoryColumn = new TableColumn<>("Thể loại");
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
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

        bookTable.getColumns().setAll(titleColumn, authorColumn, categoryColumn, availableColumn);
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

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
        VBox container = new VBox(10, bookTable, controls);
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

        loanDateColumn.setGraphic(IconProvider.icon("calendar", 16));
        dueDateColumn.setGraphic(IconProvider.icon("clock", 16));
        statusColumn.setGraphic(IconProvider.icon("overdue", 16));
        feeColumn.setGraphic(IconProvider.icon("report", 16));

        loanTable.getColumns().setAll(bookColumn, loanDateColumn, dueDateColumn, statusColumn, feeColumn);
        loanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        Button refreshButton = new Button("Làm mới");
        applyButtonIcon(refreshButton, "refresh");
        refreshButton.setOnAction(event -> refreshLoans());

        Button downloadButton = new Button("Tải xuống");
        applyButtonIcon(downloadButton, "export");
        downloadButton.setOnAction(event -> exportUserSnapshot());

        HBox controls = new HBox(10, refreshButton, downloadButton);

        VBox container = new VBox(10, loanTable, controls);
        container.setPadding(new Insets(12));
        VBox.setVgrow(loanTable, Priority.ALWAYS);
        Tab tab = new Tab("Phiếu mượn", container);
        tab.setGraphic(IconProvider.icon("clock", 18));
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
        if (!selected.isAvailable()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Sách hiện không có sẵn. Vui lòng thử đặt trước.");
            alert.setHeaderText("Không thể mượn");
            alert.showAndWait();
            return;
        }
        libraryService.borrowBook(selected.getId(), currentUser);
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Đã tạo phiếu mượn. Hạn trả sau 14 ngày.");
        alert.setHeaderText("Mượn sách thành công");
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
                "Đã tạo yêu cầu đặt trước cho sách '" + selected.getTitle() + "'.");
        alert.setHeaderText("Đặt trước thành công");
        alert.showAndWait();
        refreshAll();
    }

    private void refreshAll() {
        refreshBooks();
        refreshLoans();
        if (reservationsPanel != null) {
            reservationsPanel.refresh();
        }
    }

    private void refreshBooks() {
        List<Book> books = libraryService.getAllBooks();
        bookTable.setItems(FXCollections.observableArrayList(books));
    }

    private void refreshLoans() {
        List<Loan> loans = libraryService.getLoansForUser(currentUser.getId());
        loanTable.setItems(FXCollections.observableArrayList(loans));
    }

    private String resolveBookTitle(int bookId) {
        return libraryService.getAllBooks().stream()
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
