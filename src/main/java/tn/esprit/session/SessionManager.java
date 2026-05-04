package tn.esprit.session;

import tn.esprit.entities.User;

/**
 * SessionManager — délègue tout à JwtManager.
 * Garde la compatibilité avec le code GestionCours/Gestionquiz
 * tout en utilisant JwtManager comme source unique de vérité.
 */
public class SessionManager {

    public static void login(User user)     { JwtManager.login(user); }
    public static void logout()             { JwtManager.logout(); }
    public static User getCurrentUser()     { return JwtManager.getCurrentUser(); }
    public static boolean isAdmin()         { return JwtManager.isAdmin(); }
    public static boolean isLoggedIn()      { return JwtManager.isLoggedIn(); }
}
