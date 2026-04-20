package tn.esprit.services;

import tn.esprit.entities.Quiz;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Service Quiz — gère toutes les opérations SQL sur la table "quiz".
 * Implémente IService<Quiz> pour les 4 opérations CRUD de base.
 */
public class ServiceQuiz {

    // Connexion à la base de données (singleton partagé dans toute l'application)
    private final Connection connection = MyConnection.getInstance().getConnection();

    // ── CREATE : Insérer un nouveau quiz en BDD ───────────────────────────────
    public boolean ajouter(Quiz quiz) {
        String req = "INSERT INTO quiz (titre, description, etat, duree_max_minutes, seuil_reussite, max_tentatives, chapitre_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getEtat());
            statement.setObject(4, quiz.getDureeMaxMinutes());
            statement.setObject(5, quiz.getSeuilReussite());
            statement.setObject(6, quiz.getMaxTentatives());
            statement.setObject(7, quiz.getChapitreId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ajout quiz : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ── DELETE : Supprimer un quiz et toutes ses questions/options en cascade ──
    public boolean supprimer(Quiz quiz) {
        try {
            // Étape 1 : supprimer toutes les options des questions de ce quiz
            String delOptions = "DELETE FROM `option` WHERE question_id IN (SELECT id FROM question WHERE quiz_id = ?)";
            try (PreparedStatement st = connection.prepareStatement(delOptions)) {
                st.setInt(1, quiz.getId());
                st.executeUpdate();
            }
            // Étape 2 : supprimer toutes les questions du quiz
            String delQuestions = "DELETE FROM question WHERE quiz_id = ?";
            try (PreparedStatement st = connection.prepareStatement(delQuestions)) {
                st.setInt(1, quiz.getId());
                st.executeUpdate();
            }
            // Étape 3 : supprimer le quiz lui-même
            String delQuiz = "DELETE FROM quiz WHERE id = ?";
            try (PreparedStatement st = connection.prepareStatement(delQuiz)) {
                st.setInt(1, quiz.getId());
                return st.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur suppression quiz : " + e.getMessage());
            return false;
        }
    }

    // ── UPDATE : Modifier un quiz existant ────────────────────────────────────
    public boolean modifier(Quiz quiz) {
        String req = "UPDATE quiz SET titre = ?, description = ?, etat = ?, duree_max_minutes = ?, seuil_reussite = ?, max_tentatives = ?, chapitre_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getEtat());
            statement.setObject(4, quiz.getDureeMaxMinutes());
            statement.setObject(5, quiz.getSeuilReussite());
            statement.setObject(6, quiz.getMaxTentatives());
            statement.setObject(7, quiz.getChapitreId());
            statement.setInt(8, quiz.getId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modification quiz : " + e.getMessage());
            return false;
        }
    }

    // ── READ ALL (console) : Affiche tous les quiz dans la console ────────────
    public void getAll() {
        String req = "SELECT * FROM quiz";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                System.out.println(mapQuiz(rs)); // affiche chaque quiz
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage quiz : " + e.getMessage());
        }
    }

    // ── READ ONE (console) : Affiche un quiz par son id dans la console ───────
    public void getOneById(int id) {
        String req = "SELECT * FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapQuiz(rs));
                } else {
                    System.out.println("Aucun quiz trouvé avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche quiz : " + e.getMessage());
        }
    }

    // ── READ ALL (liste) : Retourne tous les quiz sous forme de liste ─────────
    // Utilisé par les controllers JavaFX pour afficher la liste dans l'interface
    public java.util.List<Quiz> afficher() {
        java.util.List<Quiz> quizzes = new java.util.ArrayList<>();
        String req = "SELECT * FROM quiz";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) quizzes.add(mapQuiz(rs));
        } catch (SQLException e) {
            System.err.println("Erreur affichage quiz : " + e.getMessage());
        }
        return quizzes;
    }

    // ── READ ONE (objet) : Retourne un quiz par son id ────────────────────────
    // Utilisé après une modification pour rafraîchir l'affichage
    public Quiz findById(int id) {
        String req = "SELECT * FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return mapQuiz(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById quiz : " + e.getMessage());
        }
        return null; // null si aucun quiz trouvé
    }

    // ── Méthode privée : convertit une ligne SQL en objet Quiz ────────────────
    private Quiz mapQuiz(ResultSet rs) throws SQLException {
        return new Quiz(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("etat"),
                (Integer) rs.getObject("duree_max_minutes"),
                (Integer) rs.getObject("seuil_reussite"),
                (Integer) rs.getObject("max_tentatives"),
                null, null, null,
                (Integer) rs.getObject("chapitre_id")
        );
    }

    // ── READ BY CHAPITRE : Retourne les quiz d'un chapitre spécifique ─────────
    public java.util.List<Quiz> findByChapitreId(int chapitreId) {
        java.util.List<Quiz> quizzes = new java.util.ArrayList<>();
        String req = "SELECT * FROM quiz WHERE chapitre_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, chapitreId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) quizzes.add(mapQuiz(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByChapitreId : " + e.getMessage());
        }
        return quizzes;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SYSTÈME DE TENTATIVES — Stockage en mémoire (Map statique)
    // Équivalent de la session PHP dans Symfony
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Stockage des tentatives terminées en mémoire.
     * Clé : "etudiantId_quizId"
     * Valeur : nombre de tentatives terminées
     */
    private static final java.util.Map<String, Integer> tentatives = new java.util.HashMap<>();

    /**
     * Stockage des résultats de la dernière tentative.
     * Clé : "etudiantId_quizId"
     * Valeur : Map contenant score, totalPoints, percentage, date
     */
    private static final java.util.Map<String, java.util.Map<String, Object>> derniersResultats = new java.util.HashMap<>();

    /**
     * Génère une clé unique pour un étudiant et un quiz.
     * Format : "etudiantId_quizId"
     */
    private String getKey(int etudiantId, int quizId) {
        return etudiantId + "_" + quizId;
    }

    /**
     * Obtient le nombre de tentatives terminées pour un étudiant et un quiz.
     * Équivalent de getNombreTentatives() dans Symfony.
     *
     * @param etudiantId ID de l'étudiant
     * @param quizId ID du quiz
     * @return nombre de tentatives (0 si aucune)
     */
    public int getNombreTentatives(int etudiantId, int quizId) {
        String key = getKey(etudiantId, quizId);
        return tentatives.getOrDefault(key, 0);
    }

    /**
     * Enregistre une tentative terminée et incrémente le compteur.
     * Équivalent de enregistrerTentative() dans Symfony.
     *
     * @param etudiantId ID de l'étudiant
     * @param quizId ID du quiz
     * @param score points obtenus
     * @param totalPoints total des points possibles
     * @param percentage pourcentage de réussite
     */
    public void enregistrerTentative(int etudiantId, int quizId, int score, int totalPoints, double percentage) {
        String key = getKey(etudiantId, quizId);
        
        // Incrémenter le compteur
        tentatives.put(key, tentatives.getOrDefault(key, 0) + 1);
        
        // Sauvegarder les résultats de cette tentative
        java.util.Map<String, Object> resultats = new java.util.HashMap<>();
        resultats.put("score", score);
        resultats.put("totalPoints", totalPoints);
        resultats.put("percentage", percentage);
        resultats.put("date", java.time.LocalDateTime.now().toString());
        resultats.put("tentative", tentatives.get(key));
        
        derniersResultats.put(key, resultats);
        
        System.out.println("✅ Tentative enregistrée : " + key + " → " + tentatives.get(key) + " tentative(s)");
    }

    /**
     * Récupère les résultats de la dernière tentative.
     * Équivalent de getDerniersResultats() dans Symfony.
     *
     * @param etudiantId ID de l'étudiant
     * @param quizId ID du quiz
     * @return Map contenant les résultats ou null si aucune tentative
     */
    public java.util.Map<String, Object> getDerniersResultats(int etudiantId, int quizId) {
        String key = getKey(etudiantId, quizId);
        return derniersResultats.get(key);
    }

    /**
     * Vérifie si l'étudiant a réussi le quiz (basé sur la dernière tentative).
     * Équivalent de aReussiQuiz() dans Symfony.
     *
     * @param etudiantId ID de l'étudiant
     * @param quiz le quiz
     * @return true si réussi, false sinon
     */
    public boolean aReussiQuiz(int etudiantId, Quiz quiz) {
        java.util.Map<String, Object> resultats = getDerniersResultats(etudiantId, quiz.getId());
        
        if (resultats == null) {
            return false;
        }
        
        double percentage = (double) resultats.get("percentage");
        int seuilReussite = quiz.getSeuilReussite() != null ? quiz.getSeuilReussite() : 50;
        
        return percentage >= seuilReussite;
    }

    /**
     * Vérifie si l'étudiant peut passer le quiz.
     * Équivalent de canStudentTakeQuiz() dans Symfony.
     *
     * @param etudiantId ID de l'étudiant
     * @param quiz le quiz
     * @return Map avec "canTake" (boolean) et "errors" (List<String>)
     */
    public java.util.Map<String, Object> canStudentTakeQuiz(int etudiantId, Quiz quiz) {
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        // VÉRIFICATION 1 : Le quiz doit être actif
        if (!"actif".equals(quiz.getEtat())) {
            errors.add("Ce quiz n'est pas actif.");
        }
        
        // VÉRIFICATION 2 : Nombre max de tentatives non dépassé
        if (quiz.getMaxTentatives() != null) {
            int nbTentatives = getNombreTentatives(etudiantId, quiz.getId());
            if (nbTentatives >= quiz.getMaxTentatives()) {
                errors.add("Vous avez atteint le nombre maximum de tentatives (" 
                    + quiz.getMaxTentatives() + ") pour ce quiz.");
            }
        }
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("canTake", errors.isEmpty());
        result.put("errors", errors);
        
        return result;
    }

    /**
     * Obtient toutes les statistiques d'un étudiant pour un quiz.
     * Équivalent de getStatistiquesEtudiant() dans Symfony.
     *
     * @param etudiantId ID de l'étudiant
     * @param quiz le quiz
     * @return Map contenant toutes les statistiques
     */
    public java.util.Map<String, Object> getStatistiquesEtudiant(int etudiantId, Quiz quiz) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        int nombreTentatives = getNombreTentatives(etudiantId, quiz.getId());
        java.util.Map<String, Object> check = canStudentTakeQuiz(etudiantId, quiz);
        
        stats.put("nombreTentatives", nombreTentatives);
        stats.put("maxTentatives", quiz.getMaxTentatives());
        stats.put("derniersResultats", getDerniersResultats(etudiantId, quiz.getId()));
        stats.put("aReussi", aReussiQuiz(etudiantId, quiz));
        stats.put("peutRecommencer", check.get("canTake"));
        stats.put("seuilReussite", quiz.getSeuilReussite() != null ? quiz.getSeuilReussite() : 50);
        
        return stats;
    }
}

