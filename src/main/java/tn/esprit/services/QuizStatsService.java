package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizStatsService — Statistiques avancées du module Quiz.
 *
 * Utilise une requête SQL complexe avec 4 jointures (cours, chapitre, quiz, question, option)
 * + GROUP BY + CASE WHEN + sous-requête pour produire un rapport analytique complet.
 *
 * Tables impliquées : cours → chapitre → quiz → question → option
 */
public class QuizStatsService {

    private final Connection connection = MyConnection.getInstance().getConnection();

    // ── Record de résultat ────────────────────────────────────────────────────

    public record QuizStatRow(
        String coursTitre,
        String coursNiveau,
        String chapitreTitre,
        int chapitreOrdre,
        String quizTitre,
        String quizEtat,
        int nbQuestions,
        int totalPoints,
        int nbOptions,
        int nbBonnesReponses,
        double tauxCouverture   // % d'options qui sont correctes
    ) {}

    public record CoursStatRow(
        String coursTitre,
        String coursNiveau,
        int nbChapitres,
        int nbQuizTotal,
        int nbQuizActifs,
        int nbQuestionsTotal,
        int totalPointsMax
    ) {}

    // ── Requête principale : détail par quiz ──────────────────────────────────

    /**
     * Requête SQL complexe avec 4 jointures + GROUP BY + CASE WHEN.
     * Retourne une ligne par quiz avec toutes ses statistiques.
     */
    public List<QuizStatRow> getDetailedStats() {
        List<QuizStatRow> result = new ArrayList<>();

        String sql = """
            SELECT
                c.titre                                          AS cours_titre,
                c.niveau                                         AS cours_niveau,
                ch.titre                                         AS chapitre_titre,
                ch.ordre                                         AS chapitre_ordre,
                q.titre                                          AS quiz_titre,
                q.etat                                           AS quiz_etat,
                COUNT(DISTINCT qu.id)                            AS nb_questions,
                COALESCE(SUM(qu.point), 0)                       AS total_points,
                COUNT(DISTINCT o.id)                             AS nb_options,
                SUM(CASE WHEN o.est_correcte = 1 THEN 1 ELSE 0 END) AS nb_bonnes_reponses,
                CASE
                    WHEN COUNT(DISTINCT o.id) = 0 THEN 0.0
                    ELSE ROUND(
                        SUM(CASE WHEN o.est_correcte = 1 THEN 1 ELSE 0 END) * 100.0
                        / COUNT(DISTINCT o.id), 1)
                END                                              AS taux_couverture
            FROM cours c
            JOIN chapitre ch ON ch.cours_id = c.id
            JOIN quiz q      ON q.chapitre_id = ch.id
            LEFT JOIN question qu ON qu.quiz_id = q.id
            LEFT JOIN `option` o  ON o.question_id = qu.id
            GROUP BY c.id, c.titre, c.niveau, ch.id, ch.titre, ch.ordre,
                     q.id, q.titre, q.etat
            ORDER BY c.titre ASC, ch.ordre ASC, q.titre ASC
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new QuizStatRow(
                    rs.getString("cours_titre"),
                    rs.getString("cours_niveau"),
                    rs.getString("chapitre_titre"),
                    rs.getInt("chapitre_ordre"),
                    rs.getString("quiz_titre"),
                    rs.getString("quiz_etat"),
                    rs.getInt("nb_questions"),
                    rs.getInt("total_points"),
                    rs.getInt("nb_options"),
                    rs.getInt("nb_bonnes_reponses"),
                    rs.getDouble("taux_couverture")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[QuizStats] Erreur requête détaillée : " + e.getMessage());
        }
        return result;
    }

    // ── Requête agrégée : résumé par cours ────────────────────────────────────

    /**
     * Requête SQL avec sous-requête + GROUP BY + HAVING.
     * Retourne une ligne par cours avec les totaux agrégés.
     */
    public List<CoursStatRow> getCoursSummary() {
        List<CoursStatRow> result = new ArrayList<>();

        String sql = """
            SELECT
                c.titre                                              AS cours_titre,
                c.niveau                                             AS cours_niveau,
                COUNT(DISTINCT ch.id)                                AS nb_chapitres,
                COUNT(DISTINCT q.id)                                 AS nb_quiz_total,
                SUM(CASE WHEN q.etat = 'actif' THEN 1 ELSE 0 END)   AS nb_quiz_actifs,
                COUNT(DISTINCT qu.id)                                AS nb_questions_total,
                COALESCE(SUM(qu.point), 0)                           AS total_points_max
            FROM cours c
            JOIN chapitre ch ON ch.cours_id = c.id
            LEFT JOIN quiz q ON q.chapitre_id = ch.id
            LEFT JOIN question qu ON qu.quiz_id = q.id
            GROUP BY c.id, c.titre, c.niveau
            ORDER BY nb_quiz_actifs DESC, total_points_max DESC
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new CoursStatRow(
                    rs.getString("cours_titre"),
                    rs.getString("cours_niveau"),
                    rs.getInt("nb_chapitres"),
                    rs.getInt("nb_quiz_total"),
                    rs.getInt("nb_quiz_actifs"),
                    rs.getInt("nb_questions_total"),
                    rs.getInt("total_points_max")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[QuizStats] Erreur requête résumé : " + e.getMessage());
        }
        return result;
    }

    // ── Requête : chapitres sans quiz actif ───────────────────────────────────

    /**
     * Détecte les chapitres qui n'ont aucun quiz actif (LEFT JOIN + HAVING).
     */
    public List<String[]> getChapitresSansQuizActif() {
        List<String[]> result = new ArrayList<>();

        String sql = """
            SELECT
                c.titre  AS cours_titre,
                ch.titre AS chapitre_titre,
                ch.ordre AS chapitre_ordre,
                COUNT(DISTINCT q.id)                               AS nb_quiz_total,
                SUM(CASE WHEN q.etat = 'actif' THEN 1 ELSE 0 END) AS nb_quiz_actifs
            FROM cours c
            JOIN chapitre ch ON ch.cours_id = c.id
            LEFT JOIN quiz q ON q.chapitre_id = ch.id
            GROUP BY c.id, ch.id
            HAVING nb_quiz_actifs = 0
            ORDER BY c.titre, ch.ordre
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new String[]{
                    rs.getString("cours_titre"),
                    rs.getString("chapitre_titre"),
                    String.valueOf(rs.getInt("chapitre_ordre")),
                    String.valueOf(rs.getInt("nb_quiz_total"))
                });
            }
        } catch (SQLException e) {
            System.err.println("[QuizStats] Erreur chapitres sans quiz : " + e.getMessage());
        }
        return result;
    }
}
