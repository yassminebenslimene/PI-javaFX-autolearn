package tn.esprit.services;

import tn.esprit.entities.Cours;
import tn.esprit.entities.Post;
import tn.esprit.entities.Quiz;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resource Recommendation Service
 * ─────────────────────────────────────────────────────────────────────────────
 * Algorithm: Keyword Overlap Score (Token-based similarity)
 *
 *   score(post, resource) = |keywords(post) ∩ keywords(resource)| / |keywords(post)|
 *
 * - keywords(post)     = tags + words from titre/contenu (≥4 chars, no stop-words)
 * - keywords(resource) = words from titre + description + matiere (Cours)
 *                        or titre + description (Quiz)
 * - Threshold: score ≥ 0.15 to appear in recommendations
 * - Returns top-N sorted by score DESC
 */
public class ResourceRecommendationService {

    private final ServiceCours serviceCours = new ServiceCours();
    private final ServiceQuiz  serviceQuiz  = new ServiceQuiz();

    private static final Set<String> STOP_WORDS = Set.of(
        "pour", "dans", "avec", "cette", "votre", "notre", "vous", "nous",
        "les", "des", "une", "est", "que", "qui", "par", "sur", "mais",
        "tout", "plus", "bien", "aussi", "comme", "when", "what", "this",
        "that", "with", "from", "have", "will", "been", "they", "their"
    );

    // ── Public API ────────────────────────────────────────────────────────────

    public record ResourceResult(String type, int id, String titre,
                                  String subtitle, double score) {}

    /**
     * Returns top-N recommended courses + quizzes for a given post.
     */
    public List<ResourceResult> recommend(Post post, int topN) {
        Set<String> postKeywords = extractKeywords(
            post.getTags(),
            post.getTitre(),
            post.getContenu()
        );

        if (postKeywords.isEmpty()) return Collections.emptyList();

        System.out.println("[Recommend] post#" + post.getId() + " keywords=" + postKeywords);
        List<ResourceResult> results = new ArrayList<>();

        // ── Score Courses ──────────────────────────────────────────────────
        for (Cours c : serviceCours.getAll()) {
            Set<String> resKw = extractKeywords(null, c.getTitre(),
                    c.getDescription() + " " + c.getMatiere());
            double score = overlapScore(postKeywords, resKw);
            System.out.printf("[Recommend] cours#%d '%s' kw=%s score=%.3f%n",
                    c.getId(), c.getTitre(), resKw, score);
            if (score >= 0.1) {
                String subtitle = (c.getMatiere() != null ? c.getMatiere() : "")
                        + (c.getNiveau() != null ? "  ·  " + c.getNiveau() : "");
                results.add(new ResourceResult("cours", c.getId(), c.getTitre(), subtitle, score));
            }
        }

        // ── Score Quizzes ──────────────────────────────────────────────────
        for (Quiz q : serviceQuiz.afficher()) {
            Set<String> resKw = extractKeywords(null, q.getTitre(), q.getDescription());
            double score = overlapScore(postKeywords, resKw);
            if (score >= 0.1) {
                String subtitle = q.getEtat() != null ? q.getEtat() : "Quiz";
                results.add(new ResourceResult("quiz", q.getId(), q.getTitre(), subtitle, score));
            }
        }

        // Sort by score DESC, limit to topN
        return results.stream()
            .sorted(Comparator.comparingDouble(ResourceResult::score).reversed())
            .limit(topN)
            .collect(Collectors.toList());
    }

    // ── Algorithm ─────────────────────────────────────────────────────────────

    /**
     * Keyword Overlap Score:
     *   score = |A ∩ B| / |A|
     * Measures how much of the post's keywords are covered by the resource.
     */
    private double overlapScore(Set<String> postKw, Set<String> resourceKw) {
        if (postKw.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(postKw);
        intersection.retainAll(resourceKw);
        return (double) intersection.size() / postKw.size();
    }

    /**
     * Extracts a normalized keyword set from multiple text sources.
     * - Splits on whitespace/punctuation
     * - Lowercases
     * - Removes stop-words and short words (< 3 chars)
     * - Tags (comma-separated) are added directly
     */
    private Set<String> extractKeywords(String tags, String... texts) {
        Set<String> keywords = new HashSet<>();

        // Add tags directly (already meaningful)
        if (tags != null && !tags.isBlank()) {
            for (String t : tags.split(",")) {
                String kw = t.trim().toLowerCase();
                if (!kw.isEmpty()) keywords.add(kw);
            }
        }

        // Extract from free text
        for (String text : texts) {
            if (text == null || text.isBlank()) continue;
            String normalized = text.toLowerCase()
                    .replaceAll("[^a-zàâäéèêëîïôùûüç0-9\\s]", " ");
            for (String word : normalized.split("\\s+")) {
                if (word.length() >= 3 && !STOP_WORDS.contains(word)) {
                    keywords.add(word);
                }
            }
        }
        return keywords;
    }
}
