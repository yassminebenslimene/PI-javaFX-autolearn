package tn.esprit.services;

import java.util.List;

/**
 * Interface générique CRUD partagée par tous les services.
 *
 * ServiceCours / ServiceChapitre → implémentent consulter() et consulterParId()
 * EvenementService / EquipeService / ParticipationService → implémentent getAll() et getById()
 */
public interface IService<T> {

    void ajouter(T t);
    void modifier(T t);
    void supprimer(int id);

    // ── Méthodes READ ─────────────────────────────────────────────────────────
    // Chaque service implémente l'une OU l'autre paire.
    // Les default assurent la compatibilité croisée sans récursion.

    default List<T> consulter()      { return getAll(); }
    default T consulterParId(int id) { return getById(id); }

    default List<T> getAll()         { return consulter(); }
    default T getById(int id)        { return consulterParId(id); }
}
