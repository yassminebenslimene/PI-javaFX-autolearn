package tn.esprit.session;

import tn.esprit.entities.User;

/**
 * Compatibility wrapper around JwtManager.
 * Delegates all calls to JwtManager so existing code continues to work unchanged.
 */
public class SessionManager {

    public static User getCurrentUser() {
        return JwtManager.getCurrentUser();
    }

    public static boolean isLoggedIn() {
        return JwtManager.isLoggedIn();
    }

    public static void logout() {
        JwtManager.logout();
    }

    public static void setCurrentUser(User user) {
        JwtManager.login(user);
    }
}
