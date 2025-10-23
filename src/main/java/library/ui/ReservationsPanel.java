package library.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import library.model.Book;
import library.model.Reservation;
import library.model.User;
import library.service.LibraryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReservationsPanel extends VBox {
    private final LibraryService libraryService;
    private final Runnable onChange;
    private final boolean adminMode;
    private final Integer userFilter;

    private final TableView<ReservationRow> tableView = new TableView<>();
    private final ObservableList<ReservationRow> masterReservations = FXCollections.observableArrayList();
    private final FilteredList<ReservationRow> filteredReservations = new FilteredList<>(masterReservations);
    private CheckBox pendingOnlyCheck;

    public ReservationsPanel(LibraryService libraryService, Runnable onChange, boolean adminMode) {
        this(libraryService, onChange, adminMode, null);
    }

    public ReservationsPanel(LibraryService libraryService, Runnable onChange, boolean adminMode, Integer userFilter) {
        super(10);
        this.libraryService = Objects.requireNonNull(libraryService);
        this.onChange = onChange;
        this.adminMode = adminMode;
        this.userFilter = userFilter;
        initialise();
    }

    private void initialise() {
        setPadding(new Insets(12));
        getChildren().addAll(createTable(), createControls());
        VBox.setVgrow(tableView, Priority.ALWAYS);
        refresh();
    }

    private TableView<ReservationRow> createTable() {
        TableColumn<ReservationRow, String> idColumn = new TableColumn<>("Mã");
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().reservation().getId())));
        TableColumn<ReservationRow, String> bookColumn = new TableColumn<>("Sách");
        bookColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bookTitle()));
        TableColumn<ReservationRow, String> userColumn = new TableColumn<>("Người đặt");
        userColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));
        TableColumn<ReservationRow, String> dateColumn = new TableColumn<>("Ngày đặt");
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().reservation().getReservationDate().toString()));
        TableColumn<ReservationRow, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().reservation().getStatus()));

        tableView.getColumns().setAll(idColumn, bookColumn, userColumn, dateColumn, statusColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.setItems(filteredReservations);
        return tableView;
    }

    private HBox createControls() {
        Button approveButton = new Button("Chấp nhận");
        Button rejectButton = new Button("Từ chối");
        Button fulfilButton = new Button("Hoàn tất");
        Button cancelButton = new Button("Hủy");
        Button refreshButton = new Button("Làm mới");

        applyIcon(approveButton, "add");
        applyIcon(rejectButton, "remove");
        applyIcon(fulfilButton, "loan");
        applyIcon(cancelButton, "remove");
        applyIcon(refreshButton, "refresh");

        approveButton.setOnAction(event -> approveSelected());
        rejectButton.setOnAction(event -> rejectSelected());
        fulfilButton.setOnAction(event -> fulfilSelected());
        cancelButton.setOnAction(event -> cancelSelected());

        refreshButton.setOnAction(event -> refresh());

        HBox buttons = new HBox(8);
        if (adminMode) {
            pendingOnlyCheck = new CheckBox("Chỉ hiển thị hàng chờ");
            pendingOnlyCheck.setSelected(true);
            pendingOnlyCheck.selectedProperty().addListener((obs, old, value) -> applyFilters());
            buttons.getChildren().addAll(approveButton, rejectButton, fulfilButton, cancelButton, pendingOnlyCheck, refreshButton);
        } else {
            buttons.getChildren().addAll(cancelButton, refreshButton);
        }
        return buttons;
    }

    private void handleRefresh() {
        refresh();
        if (onChange != null) {
            onChange.run();
        }
    }

    public void refresh() {
        List<Reservation> reservations = userFilter == null
                ? libraryService.getAllReservations()
                : libraryService.getReservationsForUser(userFilter);
        Map<Integer, String> bookTitles = libraryService.getAllBooks().stream()
                .collect(Collectors.toMap(Book::getId, Book::getTitle));
        Map<Integer, String> usernames = libraryService.getAllUsers().stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        List<ReservationRow> rows = new ArrayList<>();
        for (Reservation reservation : reservations) {
            String bookTitle = bookTitles.getOrDefault(reservation.getBookId(), "#" + reservation.getBookId());
            String username = usernames.getOrDefault(reservation.getUserId(), "#" + reservation.getUserId());
            rows.add(new ReservationRow(reservation, bookTitle, username));
        }
        masterReservations.setAll(rows);
        tableView.getSelectionModel().clearSelection();
        applyFilters();
    }

    private record ReservationRow(Reservation reservation, String bookTitle, String username) {
    }

    private void applyIcon(Button button, String iconName) {
        button.setGraphic(IconProvider.icon(iconName, 16));
        button.setContentDisplay(ContentDisplay.LEFT);
    }

    private List<ReservationRow> getSelectedRows() {
        return new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
    }

    private void approveSelected() {
        List<ReservationRow> rows = getSelectedRows();
        if (rows.isEmpty()) {
            return;
        }
        int approved = 0;
        List<String> errors = new ArrayList<>();
        for (ReservationRow row : rows) {
            if (row.reservation().isPending()) {
                try {
                    libraryService.approveReservation(row.reservation().getId());
                    approved++;
                } catch (RuntimeException exception) {
                    errors.add("#" + row.reservation().getId() + ": " + exception.getMessage());
                }
            }
        }
        if (approved > 0) {
            showInfo("Đã duyệt", "Đã chấp nhận " + approved + " yêu cầu đặt trước.");
        }
        if (!errors.isEmpty()) {
            showWarning("Không thể duyệt một số yêu cầu", String.join("\n", errors));
        }
        handleRefresh();
    }

    private void rejectSelected() {
        List<ReservationRow> rows = getSelectedRows();
        if (rows.isEmpty()) {
            return;
        }
        int rejected = 0;
        for (ReservationRow row : rows) {
            if (row.reservation().isPending()) {
                libraryService.rejectReservation(row.reservation().getId());
                rejected++;
            }
        }
        if (rejected > 0) {
            showInfo("Đã từ chối", "Đã từ chối " + rejected + " yêu cầu đặt trước.");
        }
        handleRefresh();
    }

    private void fulfilSelected() {
        List<ReservationRow> rows = getSelectedRows();
        if (rows.isEmpty()) {
            return;
        }
        int fulfilled = 0;
        for (ReservationRow row : rows) {
            if (row.reservation().isApproved()) {
                libraryService.fulfilReservation(row.reservation().getId());
                fulfilled++;
            }
        }
        if (fulfilled > 0) {
            showInfo("Hoàn tất", "Đã hoàn tất " + fulfilled + " yêu cầu đặt trước.");
        }
        handleRefresh();
    }

    private void cancelSelected() {
        List<ReservationRow> rows = getSelectedRows();
        if (rows.isEmpty()) {
            return;
        }
        int cancelled = 0;
        for (ReservationRow row : rows) {
            if (adminMode || row.reservation().isPending() || row.reservation().isApproved()) {
                libraryService.cancelReservation(row.reservation().getId());
                cancelled++;
            }
        }
        if (cancelled > 0) {
            showInfo("Đã hủy", "Đã hủy " + cancelled + " yêu cầu đặt trước.");
        }
        handleRefresh();
    }

    private void applyFilters() {
        if (!adminMode) {
            filteredReservations.setPredicate(row -> row.reservation().isPending());
            return;
        }
        boolean pendingOnly = pendingOnlyCheck != null && pendingOnlyCheck.isSelected();
        filteredReservations.setPredicate(row -> !pendingOnly || row.reservation().isPending());
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    private void showWarning(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
