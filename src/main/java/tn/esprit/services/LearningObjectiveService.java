package tn.esprit.services;

import tn.esprit.entities.Cours;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * SERVICE : OBJECTIF D'APPRENTISSAGE PERSONNALISÉ
 * ═══════════════════════════════════════════════════════════════
 * Filtre les cours selon l'objectif et le niveau de l'étudiant.
 * Ne modifie aucune table existante en base de données.
 *
 * FONCTIONNEMENT :
 *   1. Chaque cours est associé à des tags via son titre/matière
 *   2. L'objectif saisi est transformé en mots-clés
 *   3. Les cours dont les tags correspondent aux mots-clés sont retournés
 *   4. Filtrage optionnel par niveau
 * ═══════════════════════════════════════════════════════════════
 */
public class LearningObjectiveService {

    // ── Objectifs prédéfinis avec leurs mots-clés associés ───────────────────
    public static final Map<String, List<String>> PREDEFINED_OBJECTIVES = new LinkedHashMap<>();
    static {
        // Web Developer → technologies web uniquement, PAS java générique
        PREDEFINED_OBJECTIVES.put("Web Developer",    List.of("web", "html", "css", "javascript", "react", "angular", "vue", "frontend", "backend", "node", "php", "laravel", "bootstrap", "typescript", "developpement web"));
        // Java Developer → java uniquement
        PREDEFINED_OBJECTIVES.put("Java Developer",   List.of("java", "spring", "maven", "poo", "objet", "jee", "hibernate", "java programming"));
        // Python Developer → python uniquement
        PREDEFINED_OBJECTIVES.put("Python Developer", List.of("python", "django", "flask", "pip", "jupyter", "python programming"));
        // Data Science → data + python + sql, PAS java
        PREDEFINED_OBJECTIVES.put("Data Science",     List.of("data", "python", "sql", "mysql", "statistique", "analyse", "pandas", "numpy", "base de donnee", "donnee", "sgbd"));
        // AI / ML → IA + python, PAS java
        PREDEFINED_OBJECTIVES.put("AI / Machine Learning", List.of("python", "tensorflow", "keras", "pytorch", "neural", "deep learning", "machine learning", "intelligence artificielle", "ia", "ml"));
        // Cybersecurity
        PREDEFINED_OBJECTIVES.put("Cybersecurity",    List.of("securite", "security", "cyber", "reseau", "cryptographie", "hacking", "linux", "pentest", "firewall"));
        // Mobile
        PREDEFINED_OBJECTIVES.put("Mobile Developer", List.of("mobile", "android", "ios", "kotlin", "swift", "flutter", "react native", "application mobile"));
        // DevOps
        PREDEFINED_OBJECTIVES.put("DevOps",           List.of("devops", "docker", "kubernetes", "linux", "cloud", "aws", "git", "jenkins", "ci cd", "pipeline"));
    }

    /**
     * Transforme un objectif libre en mots-clés simples.
     * Ex: "Data Science pour débutants" → ["data", "science", "debutants"]
     */
    public List<String> extractKeywords(String objective) {
        if (objective == null || objective.isBlank()) return List.of();

        // Normaliser : minuscules, supprimer accents, découper par espaces/tirets
        String normalized = normalize(objective);
        String[] words = normalized.split("[\\s\\-_/,;.]+");

        // Filtrer les mots trop courts (articles, prépositions)
        List<String> stopWords = List.of("le", "la", "les", "de", "du", "des", "un", "une",
                "pour", "avec", "et", "ou", "en", "au", "aux", "je", "mon", "ma", "mes",
                "the", "a", "an", "for", "and", "or", "in", "of", "to", "my");

        return Arrays.stream(words)
                .filter(w -> w.length() >= 3)
                .filter(w -> !stopWords.contains(w))
                .collect(Collectors.toList());
    }

    // ── Synonymes et technologies par domaine ────────────────────────────────
    private static final Map<String, List<String>> DOMAIN_SYNONYMS = new HashMap<>();
    static {
        // Web - technologies web uniquement
        DOMAIN_SYNONYMS.put("web",         List.of("html", "css", "javascript", "react", "angular", "vue", "frontend", "backend", "node", "php", "laravel", "bootstrap", "jquery", "typescript", "developpement web", "web development"));
        DOMAIN_SYNONYMS.put("developer",   List.of("programmation", "development", "developpement", "code", "coding"));
        DOMAIN_SYNONYMS.put("frontend",    List.of("html", "css", "javascript", "react", "angular", "vue", "bootstrap", "ui", "ux", "interface"));
        DOMAIN_SYNONYMS.put("backend",     List.of("spring", "php", "node", "api", "rest", "serveur", "server"));

        // Data Science - PAS java
        DOMAIN_SYNONYMS.put("data",        List.of("python", "sql", "mysql", "postgresql", "pandas", "numpy", "donnee", "analyse", "statistique", "base de donnee", "database"));
        DOMAIN_SYNONYMS.put("science",     List.of("python", "statistique", "analyse", "recherche", "scientifique"));

        // Java - uniquement Java
        DOMAIN_SYNONYMS.put("java",        List.of("java", "spring", "maven", "poo", "objet", "jee", "hibernate", "java programming", "java developer"));

        // Python - uniquement Python
        DOMAIN_SYNONYMS.put("python",      List.of("python", "django", "flask", "pip", "jupyter", "pandas", "numpy", "python programming"));

        // AI / ML - PAS java, uniquement IA/ML
        DOMAIN_SYNONYMS.put("ai",          List.of("python", "tensorflow", "keras", "pytorch", "neural", "deep learning", "intelligence artificielle", "machine learning", "ia"));
        DOMAIN_SYNONYMS.put("machine",     List.of("python", "tensorflow", "keras", "pytorch", "neural", "deep", "intelligence artificielle", "ia", "ml"));
        // "learning" seul est trop générique - ne pas l'utiliser comme synonyme

        // Database - base de données uniquement
        DOMAIN_SYNONYMS.put("database",    List.of("sql", "mysql", "postgresql", "mongodb", "oracle", "nosql", "base de donnee", "sgbd", "merise"));
        DOMAIN_SYNONYMS.put("base",        List.of("sql", "mysql", "postgresql", "mongodb", "oracle", "nosql", "sgbd", "merise", "base de donnee"));

        // Security
        DOMAIN_SYNONYMS.put("securite",    List.of("reseau", "cryptographie", "hacking", "cyber", "linux", "firewall", "pentest"));
        DOMAIN_SYNONYMS.put("security",    List.of("reseau", "cryptographie", "hacking", "cyber", "linux", "firewall", "pentest"));
        DOMAIN_SYNONYMS.put("cyber",       List.of("securite", "security", "reseau", "cryptographie", "hacking", "linux", "pentest"));

        // Mobile
        DOMAIN_SYNONYMS.put("mobile",      List.of("android", "ios", "kotlin", "swift", "flutter", "react native", "application mobile"));

        // DevOps
        DOMAIN_SYNONYMS.put("devops",      List.of("docker", "kubernetes", "linux", "cloud", "aws", "git", "jenkins", "ci cd", "pipeline"));
    }

    /**
     * Filtre les cours selon les mots-clés de l'objectif et le niveau choisi.
     */
    public List<Cours> filterCoursByObjective(List<Cours> allCours, String objective, String niveau) {
        // Si l'objectif est un prédéfini, utiliser ses mots-clés directs
        List<String> keywords;
        if (PREDEFINED_OBJECTIVES.containsKey(objective)) {
            keywords = PREDEFINED_OBJECTIVES.get(objective);
        } else {
            // Sinon extraire les mots-clés + leurs synonymes
            List<String> baseKeywords = extractKeywords(objective);
            keywords = new ArrayList<>(baseKeywords);
            // Ajouter les synonymes pour chaque mot-clé
            for (String kw : baseKeywords) {
                List<String> synonyms = DOMAIN_SYNONYMS.getOrDefault(kw, List.of());
                keywords.addAll(synonyms);
            }
        }

        if (keywords.isEmpty()) return allCours;

        final List<String> finalKeywords = keywords;
        List<Cours> filtered = allCours.stream()
                .filter(cours -> matchesCours(cours, finalKeywords))
                .collect(Collectors.toList());

        // Filtrer par niveau si spécifié
        if (niveau != null && !niveau.equals("Tous")) {
            String niveauNorm = normalize(niveau);
            filtered = filtered.stream()
                    .filter(c -> c.getNiveau() != null && normalize(c.getNiveau()).contains(niveauNorm))
                    .collect(Collectors.toList());
        }

        return filtered;
    }

    /**
     * Vérifie si un cours correspond aux mots-clés.
     * Utilise une correspondance par mot entier pour éviter les faux positifs.
     * Ex: "java" ne doit pas matcher "javascript"
     */
    private boolean matchesCours(Cours cours, List<String> keywords) {
        String coursText = normalize(
            (cours.getTitre()       != null ? cours.getTitre()       : "") + " " +
            (cours.getMatiere()     != null ? cours.getMatiere()     : "") + " " +
            (cours.getDescription() != null ? cours.getDescription() : "")
        );

        for (String kw : keywords) {
            String kwNorm = normalize(kw);
            // Correspondance exacte pour les mots courts (évite java → javascript)
            if (kwNorm.length() <= 5) {
                // Chercher le mot entier avec délimiteurs
                if (coursText.matches(".*\\b" + java.util.regex.Pattern.quote(kwNorm) + "\\b.*")) {
                    return true;
                }
            } else {
                // Pour les mots longs (ex: "machine learning"), simple contains suffit
                if (coursText.contains(kwNorm)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Normalise une chaîne : minuscules + suppression des accents.
     */
    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
                .replace("à", "a").replace("â", "a").replace("ä", "a")
                .replace("ù", "u").replace("û", "u").replace("ü", "u")
                .replace("î", "i").replace("ï", "i")
                .replace("ô", "o").replace("ö", "o")
                .replace("ç", "c").replace("ñ", "n");
    }

    /**
     * Retourne les mots-clés d'un objectif prédéfini.
     */
    public List<String> getKeywordsForPredefined(String objectiveName) {
        return PREDEFINED_OBJECTIVES.getOrDefault(objectiveName, List.of());
    }
}
