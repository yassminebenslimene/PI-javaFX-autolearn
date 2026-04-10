package tn.esprit.entities;

public class Option {
    private int id;
    private String texteOption;
    private boolean estCorrecte;
    private int questionId;

    public Option() {
    }

    public Option(String texteOption, boolean estCorrecte, int questionId) {
        this.texteOption = texteOption;
        this.estCorrecte = estCorrecte;
        this.questionId = questionId;
    }

    public Option(int id, String texteOption, boolean estCorrecte, int questionId) {
        this.id = id;
        this.texteOption = texteOption;
        this.estCorrecte = estCorrecte;
        this.questionId = questionId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTexteOption() {
        return texteOption;
    }

    public void setTexteOption(String texteOption) {
        this.texteOption = texteOption;
    }

    public boolean isEstCorrecte() {
        return estCorrecte;
    }

    public void setEstCorrecte(boolean estCorrecte) {
        this.estCorrecte = estCorrecte;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    @Override
    public String toString() {
        return "Option{" +
                "id=" + id +
                ", texteOption='" + texteOption + '\'' +
                ", estCorrecte=" + estCorrecte +
                ", questionId=" + questionId +
                '}';
    }
}
