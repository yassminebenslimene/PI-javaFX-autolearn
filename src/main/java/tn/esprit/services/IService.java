package tn.esprit.services;

import java.util.List;

/**
 * Interface générique CRUD partagée par tous les services.
 *
 * Conventions :
 *   - ServiceCours / ServiceChapitre → implémentent consulter() et consulterParId()
 *     (getAll/getById délèguent vers eux via default)
 *   - EvenementService / EquipeService / ParticipationService → implémentent getAll() et getById()
 *     (consulter/consulterParId délèguent vers eux via default)
 */
public interface IService<T> {

    void ajouter(T t);
    void modifier(T t);
    void supprimer(int id);

    // Implémentée par ServiceCours / ServiceChapitre
    default List<T> consulter()        { throw new UnsupportedOperationException("Implémenter consulter() ou getAll()"); }
    default T consulterParId(int id)   { throw new UnsupportedOperationException("Implémenter consulterParId() ou getById()"); }

    // Implémentée par EvenementService / EquipeService / ParticipationService
    default List<T> getAll()           { return consulter(); }
    default T getById(int id)          { return consulterParId(id); }
}
