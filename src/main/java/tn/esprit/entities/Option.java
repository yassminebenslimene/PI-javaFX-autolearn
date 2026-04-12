package tn.esprit.entities;

/**
 * Entité Option — représente une option de réponse pour une question.
 * Correspond à la table "`option`" en SQL (backticks car "option" est un mot réservé SQL).
 * Une option est liée à une question via questionId (relation ManyToOne).
 */
public class Option {

    // Identifiant unique de l'option (auto-incrémenté par la BDD)
    private int id;

    // Texte de l'option de réponse (max 255 caractères)
    private String texteOption;

    // true = cette option est la bonne réponse, false = mauvaise réponse
    private boolean estCorrecte;

    // Identifiant de la question à laquelle appartient cette option (clé étrangère)
    private int questionId;

    // Constructeur vide (nécessaire pour JavaFX et JDBC)
    public Option() {
    }

    // Constructeur sans id (utilisé pour créer une nouvelle option avant insertion en BDD)
    public Option(String texteOption, boolean estCorrecte, int questionId) {
        this.texteOption = texteOption;
        this.estCorrecte = estCorrecte;
        this.questionId = questionId;
    }

    // Constructeur complet avec id (utilisé quand on lit une option depuis la BDD)
    public Option(int id, String texteOption, boolean estCorrecte, int questionId) {
        this.id = id;
        this.texteOption = texteOption;
        this.estCorrecte = estCorrecte;
        this.questionId = questionId;
    }

    // ── Getters et Setters ────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTexteOption() { return texteOption; }
    public void setTexteOption(String texteOption) { this.texteOption = texteOption; }

    // Retourne true si c'est la bonne réponse
    public boolean isEstCorrecte() { return estCorrecte; }
    public void setEstCorrecte(boolean estCorrecte) { this.estCorrecte = estCorrecte; }

    // Retourne l'id de la question parente
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    // Affichage de l'option sous forme de texte (utile pour le debug)
    @Override
    public String toString() {
        return "Option{id=" + id + ", texte='" + texteOption + "', correcte=" + estCorrecte + ", questionId=" + questionId + "}";
    }
}
