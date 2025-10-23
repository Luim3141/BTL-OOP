package library.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import library.model.User;
import library.service.AuthService;

import java.util.Objects;
import java.util.function.Consumer;

public class LoginView {
    private final AuthService authService;
    private final Consumer<User> onLoginSuccess;

    public LoginView(AuthService authService, Consumer<User> onLoginSuccess) {
        this.authService = Objects.requireNonNull(authService);
        this.onLoginSuccess = Objects.requireNonNull(onLoginSuccess);
    }

    public Scene createScene() {
        return new Scene(createContent(), 520, 360);
    }

    private Parent createContent() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Label messageLabel = new Label();

        Tab loginTab = new Tab("Đăng nhập", createLoginForm(messageLabel));
        Tab registerTab = new Tab("Đăng ký", createRegisterForm(messageLabel));
        loginTab.setGraphic(IconProvider.icon("action-button", 18));
        registerTab.setGraphic(IconProvider.icon("reader", 18));
        tabPane.getTabs().addAll(loginTab, registerTab);

        VBox container = new VBox(10, tabPane, messageLabel);
        container.setPadding(new Insets(16));
        messageLabel.getStyleClass().add("message-label");
        return container;
    }

    private Parent createLoginForm(Label messageLabel) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(12);

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Button loginButton = new Button("Đăng nhập");
        applyIcon(loginButton, "action-button");

        grid.add(new Label("Tên đăng nhập"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Mật khẩu"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(loginButton, 1, 2);

        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> {
            messageLabel.setText("");
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Vui lòng nhập đầy đủ thông tin đăng nhập.");
                return;
            }
            User user = authService.authenticate(username, password);
            if (user == null) {
                messageLabel.setText("Sai tên đăng nhập hoặc mật khẩu.");
                return;
            }
            onLoginSuccess.accept(user);
        });

        return wrap(grid);
    }

    private Parent createRegisterForm(Label messageLabel) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(12);

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField fullNameField = new TextField();
        TextField emailField = new TextField();
        Button registerButton = new Button("Đăng ký và đăng nhập");
        applyIcon(registerButton, "add");

        grid.add(new Label("Tên đăng nhập"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Mật khẩu"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Họ và tên"), 0, 2);
        grid.add(fullNameField, 1, 2);
        grid.add(new Label("Email"), 0, 3);
        grid.add(emailField, 1, 3);
        grid.add(registerButton, 1, 4);

        registerButton.setOnAction(event -> {
            messageLabel.setText("");
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String fullName = fullNameField.getText().trim();
            String email = emailField.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Tên đăng nhập và mật khẩu không được để trống.");
                return;
            }
            try {
                User user = authService.register(username, password, fullName, email);
                messageLabel.setText("Đăng ký thành công. Đang đăng nhập...");
                onLoginSuccess.accept(user);
            } catch (Exception ex) {
                messageLabel.setText("Không thể đăng ký: " + ex.getMessage());
            }
        });

        return wrap(grid);
    }

    private Parent wrap(GridPane grid) {
        BorderPane borderPane = new BorderPane(grid);
        BorderPane.setMargin(grid, new Insets(16));
        return borderPane;
    }

    private void applyIcon(Button button, String iconName) {
        button.setGraphic(IconProvider.icon(iconName, 16));
        button.setContentDisplay(ContentDisplay.LEFT);
    }
}
