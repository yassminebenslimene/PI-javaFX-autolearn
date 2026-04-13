package tn.esprit.entities;

/**
 * Entit├® Option ÔÇö repr├®sente une option de r├®ponse pour une question.
 * Correspond ├á la table "`option`" en SQL (backticks car "option" est un mot r├®serv├® SQL).
 * Une option est li├®e ├á une question via questionId (relation ManyToOne).
 */
public class Option {

    // Identifiant unique de l'option (auto-incr├®ment├® par la BDD)
    private int id;

    // Texte de l'option de r├®ponse (max 255 caract├¿res)
    private String texteOption;

    // true = cette option est la bonne r├®ponse, false = mauvaise r├®ponse
    private boolean estCorrecte;

    // Identifiant de la question ├á laquelle appartient cette option (cl├® ├®trang├¿re)
    private int questionId;

    // Constructeur vide (n├®cessaire pour JavaFX et JDBC)
    public Option() {
    }

    // Constructeur sans id (utilis├® pour cr├®er une nouvelle option avant insertion en BDD)
    public Option(String texteOption, boolean estCorrecte, int questionId) {
        this.texteOption = texteOption;
        this.estCorrecte = estCorrecte;
        this.questionId = questionId;
    }

    // Constructeur complet avec id (utilis├® quand on lit une option depuis la BDD)
    public Option(int id, String texteOption, boolean estCorrecte, int questionId) {
        this.id = id;
        this.texteOption = texteOption;
        this.estCorrecte = estCorrecte;
        this.questionId = questionId;
    }

    // ÔöÇÔöÇ Getters et Setters ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTexteOption() { return texteOption; }
    public void setTexteOption(String texteOption) { this.texteOption = texteOption; }

    // Retourne true si c'est la bonne r├®ponse
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