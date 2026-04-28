package tn.esprit.entities;

public class Chapitre {
    private int id;
    private String titre;
    private String contenu;
    private int ordre;
    private String ressources;
    private int coursId;
    private String ressourceType;
    private String ressourceFichier;

    public Chapitre() {
    }

    public Chapitre(String titre, String contenu, int ordre, String ressources, int coursId, String ressourceType, String ressourceFichier) {
        this.titre = titre;
        this.contenu = contenu;
        this.ordre = ordre;
        this.ressources = ressources;
        this.coursId = coursId;
        this.ressourceType = ressourceType;
        this.ressourceFichier = ressourceFichier;
    }

    public Chapitre(int id, String titre, String contenu, int ordre, String ressources, int coursId, String ressourceType, String ressourceFichier) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.ordre = ordre;
        this.ressources = ressources;
        this.coursId = coursId;
        this.ressourceType = ressourceType;
        this.ressourceFichier = ressourceFichier;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public String getRessources() {
        return ressources;
    }

    public void setRessources(String ressources) {
        this.ressources = ressources;
    }

    public int getCoursId() {
        return coursId;
    }

    public void setCoursId(int coursId) {
        this.coursId = coursId;
    }

    public String getRessourceType() {
        return ressourceType;
    }

    public void setRessourceType(String ressourceType) {
        this.ressourceType = ressourceType;
    }

    public String getRessourceFichier() {
        return ressourceFichier;
    }

    public void setRessourceFichier(String ressourceFichier) {
        this.ressourceFichier = ressourceFichier;
    }

    @Override
    public String toString() {
        return "Chapitre{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", contenu='" + contenu + '\'' +
                ", ordre=" + ordre +
                ", ressources='" + ressources + '\'' +
                ", coursId=" + coursId +
                ", ressourceType='" + ressourceType + '\'' +
                ", ressourceFichier='" + ressourceFichier + '\'' +
                '}';
    }
}
