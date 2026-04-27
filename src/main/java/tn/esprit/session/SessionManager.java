package tn.esprit.session;

import tn.esprit.entities.User;


public class SessionManager {

    private static User currentUser;

    public static void login(User user) {
        currentUser = user;
        // Sync with JwtManager so Face ID and JWT features work correctly
        if (user != null) JwtManager.setCurrentUser(user);
    }
    public static void logout()         { currentUser = null; JwtManager.setCurrentUser(null); }
    public static User getCurrentUser() { return currentUser; }
    public static boolean isAdmin()     { return currentUser != null && "ADMIN".equals(currentUser.getRole()); }
    public static boolean isLoggedIn()  { return currentUser != null; }
}
