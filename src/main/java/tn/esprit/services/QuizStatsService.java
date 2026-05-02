package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizStatsService — Statistiques avancées du module Quiz.
 *
 * Requêtes SQL complexes :
 *  - 5 tables jointes (cours, chapitre, quiz, question, option)
 *  - Sous-requêtes corrélées et IN (SELECT ...)
 *  - GROUP BY + HAVING + CASE WHEN + COALESCE
 *  - Recommandations intelligentes basées sur l'analyse des données
 */
public class QuizStatsService {

    private final Connection connection = MyConnection.getInstance().getConnection();

    // ── Records de résultat ───────────────────────────────────────────────────

    public record QuizStatRow(
        String coursTitre,
        String coursNiveau,
        String chapitreTitre,
        int    chapitreOrdre,
        String quizTitre,
        String quizEtat,
        int    nbQuestions,
        int    totalPoints,
        int    nbOptions,
        int    nbBonnesReponses,
        double tauxReussiteOptions   // % d'options qui sont correctes (renommé)
    ) {}

    public record CoursStatRow(
        String coursTitre,
        String coursNiveau,
        int    nbChapitres,
        int    nbQuizTotal,
        int    nbQuizActifs,
        int    nbQuestionsTotal,
        int    totalPointsMax
    ) {}

    public record RecommandationRow(
        String type,        // "MANQUE_QUIZ", "QUIZ_VIDE", "QUIZ_RICHE"
        String coursTitre,
        String chapitreTitre,
        String quizTitre,
        String message,
        String priorite     // "HAUTE", "MOYENNE", "BASSE"
    ) {}

    // ── Requête 1 : détail par quiz (5 jointures + sous-requêtes) ─────────────

    /**
     * Requête principale avec 5 tables jointes.
     * Utilise des sous-requêtes pour éviter les doublons de comptage.
     *
     * SQL complexe :
     *   - JOIN cours → chapitre → quiz
     *   - Sous-requête pour nb_questions (évite duplication par option)
     *   - Sous-requête pour total_points
     *   - Sous-requête pour nb_options et nb_bonnes_reponses
     *   - CASE WHEN pour taux_reussite_options
     */
    public List<QuizStatRow> getDetailedStats() {
        List<QuizStatRow> result = new ArrayList<>();

        String sql = """
            SELECT
                c.titre                                              AS cours_titre,
                c.niveau                                             AS cours_niveau,
                ch.titre                                             AS chapitre_titre,
                ch.ordre                                             AS chapitre_ordre,
                q.titre                                              AS quiz_titre,
                q.etat                                               AS quiz_etat,
                -- Sous-requête pour nb_questions (évite duplication par jointure option)
                (SELECT COUNT(*) FROM question qu2
                 WHERE qu2.quiz_id = q.id)                           AS nb_questions,
                -- Sous-requête pour total_points
                COALESCE(
                    (SELECT SUM(qu3.point) FROM question qu3
                     WHERE qu3.quiz_id = q.id), 0)                   AS total_points,
                -- Sous-requête pour nb_options total
                (SELECT COUNT(*) FROM `option` o2
                 JOIN question qu4 ON o2.question_id = qu4.id
                 WHERE qu4.quiz_id = q.id)                           AS nb_options,
                -- Sous-requête pour nb_bonnes_reponses
                (SELECT COUNT(*) FROM `option` o3
                 JOIN question qu5 ON o3.question_id = qu5.id
                 WHERE qu5.quiz_id = q.id AND o3.est_correcte = 1)   AS nb_bonnes_reponses,
                -- Taux de réussite des options (renommé depuis taux_couverture)
                CASE
                    WHEN (SELECT COUNT(*) FROM `option` o4
                          JOIN question qu6 ON o4.question_id = qu6.id
                          WHERE qu6.quiz_id = q.id) = 0 THEN 0.0
                    ELSE ROUND(
                        (SELECT COUNT(*) FROM `option` o5
                         JOIN question qu7 ON o5.question_id = qu7.id
                         WHERE qu7.quiz_id = q.id AND o5.est_correcte = 1) * 100.0
                        /
                        (SELECT COUNT(*) FROM `option` o6
                         JOIN question qu8 ON o6.question_id = qu8.id
                         WHERE qu8.quiz_id = q.id), 1)
                END                                                  AS taux_reussite_options
            FROM cours c
            JOIN chapitre ch ON ch.cours_id = c.id
            JOIN quiz q      ON q.chapitre_id = ch.id
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
                    rs.getDouble("taux_reussite_options")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[QuizStats] Erreur requête détaillée : " + e.getMessage());
        }
        return result;
    }

    // ── Requête 2 : résumé par cours (GROUP BY + HAVING) ─────────────────────

    public List<CoursStatRow> getCoursSummary() {
        List<CoursStatRow> result = new ArrayList<>();

        String sql = """
            SELECT
                c.titre                                              AS cours_titre,
                c.niveau                                             AS cours_niveau,
                COUNT(DISTINCT ch.id)                                AS nb_chapitres,
                COUNT(DISTINCT q.id)                                 AS nb_quiz_total,
                SUM(CASE WHEN q.etat = 'actif' THEN 1 ELSE 0 END)   AS nb_quiz_actifs,
                -- Sous-requête pour éviter duplication des questions
                (SELECT COUNT(*) FROM question qu
                 WHERE qu.quiz_id IN (
                     SELECT q2.id FROM quiz q2
                     JOIN chapitre ch2 ON q2.chapitre_id = ch2.id
                     WHERE ch2.cours_id = c.id
                 ))                                                  AS nb_questions_total,
                COALESCE(
                    (SELECT SUM(qu2.point) FROM question qu2
                     WHERE qu2.quiz_id IN (
                         SELECT q3.id FROM quiz q3
                         JOIN chapitre ch3 ON q3.chapitre_id = ch3.id
                         WHERE ch3.cours_id = c.id
                     )), 0)                                          AS total_points_max
            FROM cours c
            JOIN chapitre ch ON ch.cours_id = c.id
            LEFT JOIN quiz q ON q.chapitre_id = ch.id
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
            System.err.println("[QuizStats] Erreur résumé cours : " + e.getMessage());
        }
        return result;
    }

    // ── Requête 3 : chapitres sans quiz actif (LEFT JOIN + HAVING) ────────────

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

    // ── Requête 5 : Audit intelligent des quiz (Smart Quiz Analyzer) ──────────

    /**
     * Requête SQL niveau pro MAX :
     * - 5 jointures (cours, chapitre, quiz, question, option)
     * - AVG + CASE WHEN pour calculer le taux de réussite des options
     * - CASE imbriqué pour générer le diagnostic automatique
     * - GROUP BY multi-niveaux
     *
     * Diagnostics générés :
     *   QUIZ_VIDE      → 0 question
     *   TROP_FACILE    → > 80% options correctes
     *   TROP_DIFFICILE → < 30% options correctes
     *   NORMAL         → entre 30% et 80%
     */
    public record AuditRow(
        String coursTitre,
        String chapitreTitre,
        String quizTitre,
        String quizEtat,
        int    nbQuestions,
        double tauxReussite,   // % options correctes
        String diagnostic,     // QUIZ_VIDE / TROP_FACILE / TROP_DIFFICILE / NORMAL
        String action          // Action recommandée
    ) {}

    public List<AuditRow> getAuditIntelligent() {
        List<AuditRow> result = new ArrayList<>();

        // Requête simplifiée : fonctionne même si chapitre_id ou cours_id est null
        String sql = """
            SELECT
                COALESCE(c.titre, '—')                           AS cours,
                COALESCE(ch.titre, '—')                          AS chapitre,
                q.titre                                          AS quiz,
                q.etat                                           AS etat,
                COUNT(DISTINCT qu.id)                            AS nb_questions,
                COALESCE(
                    AVG(CASE WHEN o.est_correcte = 1 THEN 1.0 ELSE 0.0 END) * 100,
                    0)                                           AS taux_reussite,
                CASE
                    WHEN COUNT(DISTINCT qu.id) = 0
                        THEN 'QUIZ_VIDE'
                    WHEN COUNT(DISTINCT o.id) = 0
                        THEN 'SANS_OPTIONS'
                    WHEN AVG(CASE WHEN o.est_correcte = 1 THEN 1.0 ELSE 0.0 END) > 0.8
                        THEN 'TROP_FACILE'
                    WHEN AVG(CASE WHEN o.est_correcte = 1 THEN 1.0 ELSE 0.0 END) < 0.15
                        THEN 'TROP_DIFFICILE'
                    ELSE 'NORMAL'
                END                                              AS diagnostic
            FROM quiz q
            LEFT JOIN chapitre ch ON ch.id = q.chapitre_id
            LEFT JOIN cours c     ON c.id  = ch.cours_id
            LEFT JOIN question qu ON qu.quiz_id = q.id
            LEFT JOIN `option` o  ON o.question_id = qu.id
            GROUP BY q.id, q.titre, q.etat, c.titre, ch.titre
            ORDER BY
                FIELD(
                    CASE
                        WHEN COUNT(DISTINCT qu.id) = 0 THEN 'QUIZ_VIDE'
                        WHEN COUNT(DISTINCT o.id) = 0 THEN 'SANS_OPTIONS'
                        WHEN AVG(CASE WHEN o.est_correcte = 1 THEN 1.0 ELSE 0.0 END) > 0.8 THEN 'TROP_FACILE'
                        WHEN AVG(CASE WHEN o.est_correcte = 1 THEN 1.0 ELSE 0.0 END) < 0.15 THEN 'TROP_DIFFICILE'
                        ELSE 'NORMAL'
                    END,
                    'QUIZ_VIDE', 'SANS_OPTIONS', 'TROP_DIFFICILE', 'TROP_FACILE', 'NORMAL'
                ),
                q.titre
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String diagnostic = rs.getString("diagnostic");
                String action = switch (diagnostic) {
                    case "QUIZ_VIDE"       -> "Ajouter des questions à ce quiz";
                    case "SANS_OPTIONS"    -> "Ajouter des options de réponse aux questions";
                    case "TROP_FACILE"     -> "Augmenter la difficulté — réduire les bonnes réponses";
                    case "TROP_DIFFICILE"  -> "Simplifier le quiz — revoir les options correctes";
                    default                -> "Quiz bien équilibré ✅";
                };
                result.add(new AuditRow(
                    rs.getString("cours"),
                    rs.getString("chapitre"),
                    rs.getString("quiz"),
                    rs.getString("etat"),
                    rs.getInt("nb_questions"),
                    rs.getDouble("taux_reussite"),
                    diagnostic,
                    action
                ));
            }
        } catch (SQLException e) {
            System.err.println("[QuizStats] Erreur audit : " + e.getMessage());
        }
        return result;
    }

    /**
     * Génère des recommandations intelligentes basées sur l'analyse des données.
     *
     * Utilise :
     *  - Sous-requête IN (SELECT quiz_id FROM question GROUP BY ... HAVING COUNT > 5)
     *  - LEFT JOIN pour détecter les chapitres sans quiz
     *  - CASE WHEN pour classifier la priorité
     */
    public List<RecommandationRow> getRecommandations() {
        List<RecommandationRow> result = new ArrayList<>();

        // Recommandation 1 : Quiz avec trop peu de questions (< 3)
        String sqlPeuQuestions = """
            SELECT
                c.titre  AS cours_titre,
                ch.titre AS chapitre_titre,
                q.titre  AS quiz_titre,
                (SELECT COUNT(*) FROM question qu WHERE qu.quiz_id = q.id) AS nb_q
            FROM quiz q
            JOIN chapitre ch ON q.chapitre_id = ch.id
            JOIN cours c     ON ch.cours_id = c.id
            WHERE q.etat = 'actif'
              AND q.id IN (
                  SELECT quiz_id FROM question
                  GROUP BY quiz_id
                  HAVING COUNT(*) < 3
              )
            ORDER BY nb_q ASC
            """;

        try (PreparedStatement ps = connection.prepareStatement(sqlPeuQuestions);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int nb = rs.getInt("nb_q");
                result.add(new RecommandationRow(
                    "QUIZ_INCOMPLET",
                    rs.getString("cours_titre"),
                    rs.getString("chapitre_titre"),
                    rs.getString("quiz_titre"),
                    "Quiz actif avec seulement " + nb + " question(s) — recommandé : min 3",
                    nb == 0 ? "HAUTE" : "MOYENNE"
                ));
            }
        } catch (SQLException e) {
            System.err.println("[QuizStats] Erreur reco questions : " + e.getMessage());
        }

        // Recommandation 2 : Chapitres sans aucun quiz
        String sqlSansQuiz = """
            SELECT c.titre AS cours_titre, ch.titre AS chapitre_titre, ch.ordre
            FROM chapitre ch
            JOIN cours c ON ch.cours_id = c.id
            WHERE ch.id NOT IN (SELECT DISTINCT chapitre_id FROM quiz WHERE chapitre_id IS NOT NULL)
            ORDER BY c.titre, ch.ordre
            """;

        try (PreparedStatement ps = connection.prepareStatement(sqlSansQuiz);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new RecommandationRow(
                    "MANQUE_QUIZ",
                    rs.getString("cours_titre"),
                    rs.getString("chapitre_titre"),
                    "—",
                    "Chapitre " + rs.getInt("ordre") + " sans aucun quiz — créer un quiz pour ce chapitre",
                    "HAUTE"
                ));
            }
        } catch (SQLException e) {
            System.err.println("[QuizStats] Erreur reco sans quiz : " + e.getMessage());
        }

        // Recommandation 3 : Quiz riches (> 5 questions) — à mettre en avant
        String sqlRiches = """
            SELECT c.titre AS cours_titre, ch.titre AS chapitre_titre, q.titre AS quiz_titre,
                   COUNT(*) AS nb_q
            FROM question qu
            JOIN quiz q      ON qu.quiz_id = q.id
            JOIN chapitre ch ON q.chapitre_id = ch.id
            JOIN cours c     ON ch.cours_id = c.id
            WHERE q.etat = 'actif'
            GROUP BY q.id, c.titre, ch.titre, q.titre
            HAVING COUNT(*) > 5
            ORDER BY nb_q DESC
            LIMIT 5
            """;

        try (PreparedStatement ps = connection.prepareStatement(sqlRiches);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new RecommandationRow(
                    "QUIZ_RICHE",
                    rs.getString("cours_titre"),
                    rs.getString("chapitre_titre"),
                    rs.getString("quiz_titre"),
                    "Quiz complet avec " + rs.getInt("nb_q") + " questions — excellent contenu ✅",
                    "BASSE"
                ));
            }
        } catch (SQLException e) {
            System.err.println("[QuizStats] Erreur reco riches : " + e.getMessage());
        }

        return result;
    }
}
