package tn.esprit.services;

import tn.esprit.entities.User;
import java.util.List;

public interface IUserService {
    void ajouter(User user);
    List<User> afficher();
    User trouver(int id);
    void modifier(User user);
    void supprimer(int id);
}
