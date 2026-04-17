package tn.esprit.session;

import tn.esprit.entities.User;


public class SessionManager {

    private static User currentUser;

    public static void login(User user) { currentUser = user; }
    public static void logout()         { currentUser = null; }
    public static User getCurrentUser() { return currentUser; }
    public static boolean isAdmin()     { return currentUser != null && "ADMIN".equals(currentUser.getRole()); }
    public static boolean isLoggedIn()  { return currentUser != null; }
}
