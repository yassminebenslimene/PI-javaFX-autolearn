package tn.esprit.session;

import tn.esprit.entities.User;

/**
 * Holds the currently logged-in user for the session.
 * Mirrors Symfony's Security component / getUser().
 */
public class SessionManager {

    private static User currentUser;

    public static void login(User user) { currentUser = user; }
    public static void logout()         { currentUser = null; }
    public static User getCurrentUser() { return currentUser; }
    public static boolean isAdmin()     { return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole()); }
    public static boolean isLoggedIn()  { return currentUser != null; }
}