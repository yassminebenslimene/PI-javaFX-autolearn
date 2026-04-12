package tn.esprit.services;

public interface IService<T> {
    public boolean ajouter(T t);
    public boolean supprimer(T t);
    public boolean modifier(T t);
    public void getAll();
    public void getOneById(int id);
}
