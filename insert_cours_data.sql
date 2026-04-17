-- ============================================================
-- INSERT COURS + CHAPITRES depuis les DataFixtures Symfony
-- Exécuter dans MySQL sur la base du projet Java
-- ============================================================

-- COURS 1: Java Programming
INSERT INTO cours (titre, description, matiere, niveau, duree, created_at)
VALUES ('Java Programming for Beginners',
        'Complete Java programming course covering fundamentals to advanced concepts. Learn object-oriented programming, data structures, and build real-world applications.',
        'Informatique', 'Debutant', 50, NOW());

SET @java_cours_id = LAST_INSERT_ID();

INSERT INTO chapitre (titre, contenu, ordre, cours_id) VALUES
('Introduction to Java',
 'Java est un langage de programmation puissant créé par James Gosling en 1995. Il est indépendant de la plateforme (Write Once Run Anywhere), orienté objet, robuste et sécurisé. Java est utilisé dans les applications d''entreprise, Android, le web et le Big Data. Pour commencer : installez le JDK et un IDE comme IntelliJ IDEA. Structure de base : public class HelloWorld { public static void main(String[] args) { System.out.println("Hello, World!"); } }',
 1, @java_cours_id),

('Variables and Data Types',
 'Java possède 8 types primitifs : byte, short, int, long, float, double, char, boolean. Exemples : int age = 25; double pi = 3.14; char grade = ''A''; boolean isStudent = true; String name = "Alice"; Les constantes utilisent le mot-clé final : final double PI = 3.14159; Le casting permet de convertir entre types : int x = (int) 3.14; // x = 3',
 2, @java_cours_id),

('Operators and Expressions',
 'Les opérateurs arithmétiques : + - * / % ++ --. Les opérateurs de comparaison : == != > < >= <=. Les opérateurs logiques : && || !. Les opérateurs d''affectation : += -= *= /= %=. L''opérateur ternaire : String status = (age >= 18) ? "Adult" : "Minor";',
 3, @java_cours_id),

('Control Flow Statements',
 'Les instructions de contrôle permettent de prendre des décisions. if/else : if (score >= 90) { grade = "A"; } else if (score >= 80) { grade = "B"; } else { grade = "C"; }. switch : switch(day) { case 1: System.out.println("Monday"); break; default: System.out.println("Other"); }',
 4, @java_cours_id),

('Loops and Iterations',
 'Les boucles répètent du code. for : for (int i = 0; i < 5; i++) { System.out.println(i); }. while : while (count < 5) { count++; }. do-while : do { input = scanner.nextInt(); } while (input <= 0);. for-each : for (int num : numbers) { System.out.println(num); }. break et continue contrôlent le flux.',
 5, @java_cours_id),

('Methods and Functions',
 'Les méthodes sont des blocs de code réutilisables. Déclaration : public static int add(int a, int b) { return a + b; }. La surcharge (overloading) permet plusieurs méthodes avec le même nom mais des paramètres différents. La récursion : public static int factorial(int n) { if (n <= 1) return 1; return n * factorial(n-1); }',
 6, @java_cours_id),

('Object-Oriented Programming',
 'La POO repose sur les classes et objets. Encapsulation : variables privées avec getters/setters. Héritage : class Dog extends Animal { @Override public void makeSound() { System.out.println("Woof!"); } }. Polymorphisme : un objet peut prendre plusieurs formes. Classes abstraites : abstract class Shape { abstract double calculateArea(); }',
 7, @java_cours_id),

('Arrays and Collections',
 'Les tableaux stockent plusieurs valeurs : int[] numbers = {1, 2, 3, 4, 5};. ArrayList : ArrayList<String> list = new ArrayList<>(); list.add("Alice");. HashMap : HashMap<String, Integer> map = new HashMap<>(); map.put("Alice", 25);. HashSet : pas de doublons. Collections.sort(), Collections.reverse() pour manipuler les collections.',
 8, @java_cours_id);

-- COURS 2: Web Development
INSERT INTO cours (titre, description, matiere, niveau, duree, created_at)
VALUES ('Web Development - HTML CSS JavaScript',
        'Complete web development course covering HTML, CSS, and JavaScript. Learn to build modern, responsive websites from scratch.',
        'Developpement Web', 'Debutant', 60, NOW());

SET @web_cours_id = LAST_INSERT_ID();

INSERT INTO chapitre (titre, contenu, ordre, cours_id) VALUES
('Introduction to HTML',
 'HTML (HyperText Markup Language) est le langage de base du web. Il structure le contenu avec des balises. Chaque page commence par <!DOCTYPE html> suivi de <html>, <head> et <body>. Les balises principales : <h1>-<h6> pour les titres, <p> pour les paragraphes, <a href=""> pour les liens, <img src=""> pour les images, <ul><li> pour les listes.',
 1, @web_cours_id),

('HTML Structure and Semantics',
 'HTML5 introduit des balises sémantiques : <header>, <nav>, <main>, <section>, <article>, <aside>, <footer>. Ces balises améliorent l''accessibilité et le SEO. Les formulaires utilisent <form>, <input type="text">, <input type="email">, <button>. Les tableaux : <table>, <tr>, <td>, <th>.',
 2, @web_cours_id),

('Introduction to CSS',
 'CSS (Cascading Style Sheets) contrôle l''apparence visuelle. Les sélecteurs : par balise (p), par classe (.classe), par id (#id). Propriétés principales : color, background-color, font-size, margin, padding, border. Exemple : .btn { background-color: #7a6ad8; color: white; padding: 10px 20px; border-radius: 5px; }',
 3, @web_cours_id),

('CSS Layout and Positioning',
 'Le box model comprend content, padding, border et margin. Flexbox : display:flex, justify-content:center, align-items:center. CSS Grid : display:grid, grid-template-columns: repeat(3, 1fr). Position : static, relative, absolute, fixed, sticky. Ces outils permettent de créer des mises en page complexes et responsives.',
 4, @web_cours_id),

('Responsive Web Design',
 'Le responsive design adapte les pages à toutes les tailles d''écran. Media queries : @media (max-width: 768px) { .container { flex-direction: column; } }. Unités relatives : %, vw, vh, em, rem. Images responsives : img { max-width: 100%; height: auto; }. Le meta viewport est essentiel : <meta name="viewport" content="width=device-width, initial-scale=1">.',
 5, @web_cours_id),

('Introduction to JavaScript',
 'JavaScript ajoute de l''interactivité aux pages web. Variables : let x = 10; const PI = 3.14; var old = "legacy". Types : string, number, boolean, array, object, null, undefined. Fonctions : function greet(name) { return "Hello " + name; } ou arrow : const greet = (name) => "Hello " + name. Conditions : if/else, switch. Boucles : for, while, forEach.',
 6, @web_cours_id),

('JavaScript DOM Manipulation',
 'Le DOM représente la page HTML comme un arbre d''objets. Sélection : document.getElementById("id"), document.querySelector(".classe"). Modification : element.innerHTML = "<b>Nouveau</b>"; element.style.color = "red"; element.classList.add("active"). Création : const div = document.createElement("div"); document.body.appendChild(div). Suppression : element.remove().',
 7, @web_cours_id),

('JavaScript Events and Interactivity',
 'Les événements répondent aux actions utilisateur. addEventListener : btn.addEventListener("click", function() { alert("Cliqué!"); }). Événements courants : click, mouseover, mouseout, keydown, keyup, submit, change. event.preventDefault() empêche le comportement par défaut (ex: soumission de formulaire). La délégation d''événements gère les éléments dynamiques.',
 8, @web_cours_id),

('JavaScript Async and Fetch API',
 'JavaScript asynchrone gère les opérations longues. Promises : fetch(url).then(response => response.json()).then(data => console.log(data)).catch(error => console.error(error)). async/await : async function getData() { try { const response = await fetch(url); const data = await response.json(); return data; } catch(e) { console.error(e); } }',
 9, @web_cours_id),

('Building a Complete Web Project',
 'Pour construire un projet web complet : structure du projet avec index.html, styles.css, script.js. Bonnes pratiques : code propre et commenté, séparation des responsabilités HTML/CSS/JS, validation des formulaires côté client, gestion des erreurs. Utiliser Git pour le versioning. Déploiement sur GitHub Pages, Netlify ou Vercel gratuitement.',
 10, @web_cours_id);

-- ============================================================
-- Vérification
-- ============================================================
SELECT c.id, c.titre, c.matiere, c.niveau, COUNT(ch.id) as nb_chapitres
FROM cours c
LEFT JOIN chapitre ch ON ch.cours_id = c.id
GROUP BY c.id, c.titre, c.matiere, c.niveau;
