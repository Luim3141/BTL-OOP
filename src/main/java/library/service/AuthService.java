package library.service;

import library.model.User;

public class AuthService {
    private final DatabaseManager databaseManager;

    public AuthService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public User authenticate(String username, String password) {
        User user = databaseManager.findUserByUsername(username);
        if (user == null) {
            return null;
        }
        if (!user.getPassword().equals(password)) {
            return null;
        }
        return user;
    }

    public User register(String username, String password, String fullName, String email) {
        return databaseManager.createUser(username, password, "USER", fullName, email);
    }
}
