package tn.esprit.services;

/**
 * Interface générique pour les opérations CRUD.
 * Chaque service (ServiceQuiz, ServiceQuestion, ServiceOption) doit implémenter ces méthodes.
 * Le type T représente l'entité concernée (Quiz, Question, Option).
 */
public interface IService<T> {

    // Ajoute un nouvel enregistrement en base de données → retourne true si succès
    public boolean ajouter(T t);

    // Supprime un enregistrement de la base de données → retourne true si succès
    public boolean supprimer(T t);

    // Modifie un enregistrement existant → retourne true si succès
    public boolean modifier(T t);

    // Affiche tous les enregistrements dans la console (pour debug)
    public void getAll();

    // Affiche un enregistrement par son identifiant (pour debug)
    public void getOneById(int id);
}
