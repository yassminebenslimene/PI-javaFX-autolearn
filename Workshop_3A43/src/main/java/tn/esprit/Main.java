package tn.esprit;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import tn.esprit.entities.Option;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;
import tn.esprit.services.ServiceQuiz;
import tn.esprit.tools.MyConnection;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            Connection connection = MyConnection.getInstance().getConnection();
            System.out.println("Etat de la connexion : " + !connection.isClosed());

            ServiceQuiz serviceQuiz = new ServiceQuiz();
            ServiceQuestion serviceQuestion = new ServiceQuestion();
            ServiceOption serviceOption = new ServiceOption();

            Quiz quiz = new Quiz("Java Basics", "Quiz d'introduction Java", "actif", 30, 60, 3, null, null, LocalDateTime.now());
            Question question = new Question("Quelle est la JVM ?", 10, LocalDateTime.now(), 1);
            Option option = new Option("Java Virtual Machine", true, 1);

            // serviceQuiz.ajouter(quiz);
            // serviceQuestion.ajouter(question);
            // serviceOption.ajouter(option);
            // serviceQuiz.modifier(new Quiz(1, "Java Basics MAJ", "Quiz modifie", "actif", 45, 70, 2, null, null, LocalDateTime.now()));
            // serviceQuestion.modifier(new Question(1, "Question modifiee", 15, LocalDateTime.now(), 1));
            // serviceOption.modifier(new Option(1, "Reponse modifiee", false, 1));
            // serviceOption.supprimer(new Option(1, null, false, 0));
            // serviceQuestion.supprimer(new Question(1, null, 0, null, 0));
            // serviceQuiz.supprimer(new Quiz(1, null, null, null, null, null, null, null, null, null));
            // serviceQuiz.getAll();
            // serviceQuestion.getAll();
            // serviceOption.getAll();
            // serviceQuiz.getOneById(1);
        } catch (SQLException e) {
            System.err.println("Erreur lors du test de la connexion : " + e.getMessage());
        }
    }
}
