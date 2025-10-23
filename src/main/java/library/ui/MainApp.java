package library.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import library.model.User;
import library.service.AuthService;
import library.service.DatabaseManager;
import library.service.LibraryService;

import java.nio.file.Path;

public class MainApp extends Application {
    private DatabaseManager databaseManager;
    private AuthService authService;
    private LibraryService libraryService;
    @Override
    public void start(Stage primaryStage) {
        databaseManager = new DatabaseManager(Path.of("data/library.db"));
        authService = new AuthService(databaseManager);
        libraryService = new LibraryService(databaseManager);

        primaryStage.getIcons().setAll(IconProvider.image("book"), IconProvider.image("reader"));
        showLogin(primaryStage);
    }

    @Override
    public void stop() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private void showLogin(Stage stage) {
        LoginView loginView = new LoginView(authService, user -> openDashboard(stage, user));
        Scene scene = loginView.createScene();
        stage.setTitle("Thư viện - Đăng nhập");
        stage.setScene(scene);
        stage.show();
    }

    private void openDashboard(Stage stage, User user) {
        if (user.isAdmin()) {
            AdminDashboardView adminView = new AdminDashboardView(user, libraryService, () -> showLogin(stage));
            stage.setTitle("Thư viện - Quản trị");
            stage.setScene(adminView.createScene());
        } else {
            UserDashboardView userView = new UserDashboardView(user, libraryService, () -> showLogin(stage));
            stage.setTitle("Thư viện - Người dùng");
            stage.setScene(userView.createScene());
        }
        stage.show();
    }
}
