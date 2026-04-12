package tn.esprit.services;

import java.util.List;

/**
 * Interface générique CRUD — utilisée par ServiceCours et ServiceChapitre.
 * ServiceQuiz, ServiceQuestion, ServiceOption n'implémentent pas cette interface
 * car ils ont des signatures différentes (boolean, supprimer(T t)).
 */
public interface IService<T> {

    // Ajouter un enregistrement
    void ajouter(T t);

    // Modifier un enregistrement
    void modifier(T t);

    // Supprimer par id
    void supprimer(int id);

    // Retourner tous les enregistrements
    List<T> consulter();

    // Retourner un enregistrement par id
    T consulterParId(int id);
}
