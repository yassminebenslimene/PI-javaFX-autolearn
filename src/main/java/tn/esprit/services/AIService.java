package tn.esprit.services;

import com.google.gson.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class AIService {

    // Utilisation d'une API gratuite (ex: Hugging Face, ou Ollama en local)
    // Pour cet exemple, nous allons simuler avec une logique de génération
    // mais vous pouvez remplacer par votre propre API

    public List<AIExercise> generateExercises(String topic, int count, String difficulty) {
        List<AIExercise> exercises = new ArrayList<>();

        // Générer les exercices basés sur le thème et la difficulté
        for (int i = 1; i <= count; i++) {
            String question = generateQuestion(topic, difficulty, i);
            String answer = generateAnswer(topic, question, difficulty);
            int points = calculatePoints(difficulty);

            exercises.add(new AIExercise(question, answer, points));
        }

        return exercises;
    }

    private String generateQuestion(String topic, String difficulty, int index) {
        String[] questionTemplates = {
                "Qu'est-ce que %s et comment l'utiliser dans un contexte professionnel ?",
                "Expliquez les concepts avancés de %s pour un développeur senior.",
                "Quelles sont les meilleures pratiques pour implémenter %s dans une architecture d'entreprise ?",
                "Comparez %s avec ses alternatives sur le marché actuel.",
                "Décrivez une architecture scalable utilisant %s pour une application à fort trafic.",
                "Quels sont les défis courants lors de l'intégration de %s et comment les résoudre ?",
                "Comment optimiser les performances d'une application utilisant %s ?",
                "Quelles sont les bonnes pratiques de sécurité pour %s ?",
                "Expliquez le processus de déploiement continu pour une application %s.",
                "Comment gérer les erreurs et les exceptions dans %s ?"
        };

        String selectedTemplate = questionTemplates[(index - 1) % questionTemplates.length];

        String fullQuestion = String.format(selectedTemplate, topic);

        // Ajouter des variations selon la difficulté
        switch (difficulty.toLowerCase()) {
            case "expert":
                fullQuestion = "[EXPERT] " + fullQuestion + " (avec exemples de code concrets)";
                break;
            case "avancé":
                fullQuestion = "[AVANCÉ] " + fullQuestion;
                break;
            case "débutant":
                fullQuestion = "[DÉBUTANT] " + fullQuestion.replace("contextes professionnels", "situations simples");
                break;
            default:
                break;
        }

        return fullQuestion;
    }

    private String generateAnswer(String topic, String question, String difficulty) {
        StringBuilder answer = new StringBuilder();

        switch (difficulty.toLowerCase()) {
            case "débutant":
                answer.append("Réponse simple : ").append(topic).append(" est une technologie fondamentale. ");
                answer.append("Dans un contexte professionnel, on l'utilise pour résoudre des problèmes courants. ");
                answer.append("Exemple de base : implémentation simple avec les bonnes pratiques standards.");
                break;

            case "intermédiaire":
                answer.append("Réponse détaillée : ").append(topic).append(" offre plusieurs approches. ");
                answer.append("En entreprise, les avantages incluent : la scalabilité, la maintenabilité et la performance. ");
                answer.append("Voici un exemple d'implémentation professionnelle :\n");
                answer.append("```\n");
                answer.append("// Code exemple pour ").append(topic).append("\n");
                answer.append("public class Example {\n");
                answer.append("    // Implémentation standard\n");
                answer.append("    public void execute() {\n");
                answer.append("        // Logique métier\n");
                answer.append("    }\n");
                answer.append("}\n");
                answer.append("```\n");
                answer.append("Points clés à retenir : robustesse, tests unitaires et documentation.");
                break;

            case "avancé":
                answer.append("Analyse approfondie : ").append(topic).append(" dans une architecture d'entreprise. ");
                answer.append("Patterns recommandés : Microservices, Event-Driven, CQRS. ");
                answer.append("Considérations techniques :\n");
                answer.append("• Performance et optimisation\n");
                answer.append("• Sécurité et authentification\n");
                answer.append("• Monitoring et logging\n");
                answer.append("• Gestion des erreurs avancée\n");
                answer.append("• Tests d'intégration et E2E\n");
                answer.append("Exemple d'architecture :");
                break;

            case "expert":
                answer.append("Solution architecturale : Optimisation de ").append(topic).append(" en environnement de production. ");
                answer.append("Stratégies avancées :\n");
                answer.append("1. Architecture Hexagonale / Clean Architecture\n");
                answer.append("2. Event Sourcing et CQRS\n");
                answer.append("3. Circuit Breaker et Resilience4J\n");
                answer.append("4. Distributed Tracing avec Jaeger/Zipkin\n");
                answer.append("5. Chaos Engineering\n");
                answer.append("KPI à surveiller :\n");
                answer.append("- Latence p99\n");
                answer.append("- Taux d'erreur\n");
                answer.append("- Throughput\n");
                answer.append("- Saturation des ressources");
                break;

            default:
                answer.append("Réponse professionnelle concernant ").append(topic);
        }

        return answer.toString();
    }

    private int calculatePoints(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "débutant": return 10;
            case "intermédiaire": return 20;
            case "avancé": return 30;
            case "expert": return 50;
            default: return 15;
        }
    }

    public static class AIExercise {
        private String question;
        private String answer;
        private int points;

        public AIExercise(String question, String answer, int points) {
            this.question = question;
            this.answer = answer;
            this.points = points;
        }

        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public int getPoints() { return points; }
    }
}