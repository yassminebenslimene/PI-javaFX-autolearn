package tn.esprit.entities;

import java.time.LocalDateTime;

/**
 * Entit├® Quiz ÔÇö repr├®sente un quiz dans la base de donn├®es.
 * Correspond ├á la table "quiz" en SQL.
 */
public class Quiz {

    // Identifiant unique du quiz (auto-incr├®ment├® par la BDD)
    private int id;

    // Titre du quiz (ex: "Quiz Java", entre 3 et 255 caract├¿res)
    private String titre;

    // Description du contenu du quiz (entre 10 et 2000 caract├¿res)
    private String description;

    // ├ëtat du quiz : "actif", "inactif", "brouillon" ou "archive"
    private String etat;

    // Dur├®e maximale en minutes (optionnel, null = pas de limite)
    private Integer dureeMaxMinutes;

    // Pourcentage minimum pour r├®ussir le quiz (optionnel, ex: 50 = 50%)
    private Integer seuilReussite;

    // Nombre maximum de tentatives autoris├®es (optionnel, null = illimit├®)
    private Integer maxTentatives;

    // Nom du fichier image associ├® au quiz (optionnel)
    private String imageName;

    // Taille du fichier image en octets (optionnel)
    private Integer imageSize;

    // Date et heure de la derni├¿re modification
    private LocalDateTime updatedAt;

    // Cl├® ├®trang├¿re vers le chapitre auquel appartient ce quiz (OBLIGATOIRE)
    private Integer chapitreId;

    // Constructeur vide (n├®cessaire pour JavaFX et JDBC)
    public Quiz() {
    }

    // Constructeur sans id (utilis├® pour cr├®er un nouveau quiz avant insertion en BDD)
    public Quiz(String titre, String description, String etat, Integer dureeMaxMinutes, Integer seuilReussite,
                Integer maxTentatives, String imageName, Integer imageSize, LocalDateTime updatedAt, Integer chapitreId) {
        this.titre = titre;
        this.description = description;
        this.etat = etat;
        this.dureeMaxMinutes = dureeMaxMinutes;
        this.seuilReussite = seuilReussite;
        this.maxTentatives = maxTentatives;
        this.imageName = imageName;
        this.imageSize = imageSize;
        this.updatedAt = updatedAt;
        this.chapitreId = chapitreId;
    }

    // Constructeur complet avec id (utilis├® quand on lit un quiz depuis la BDD)
    public Quiz(int id, String titre, String description, String etat, Integer dureeMaxMinutes, Integer seuilReussite,
                Integer maxTentatives, String imageName, Integer imageSize, LocalDateTime updatedAt, Integer chapitreId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.etat = etat;
        this.dureeMaxMinutes = dureeMaxMinutes;
        this.seuilReussite = seuilReussite;
        this.maxTentatives = maxTentatives;
        this.imageName = imageName;
        this.imageSize = imageSize;
        this.updatedAt = updatedAt;
        this.chapitreId = chapitreId;
    }

    // ÔöÇÔöÇ Getters et Setters ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    // Permettent de lire et modifier les champs depuis l'ext├®rieur de la classe

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public Integer getDureeMaxMinutes() { return dureeMaxMinutes; }
    public void setDureeMaxMinutes(Integer dureeMaxMinutes) { this.dureeMaxMinutes = dureeMaxMinutes; }

    public Integer getSeuilReussite() { return seuilReussite; }
    public void setSeuilReussite(Integer seuilReussite) { this.seuilReussite = seuilReussite; }

    public Integer getMaxTentatives() { return maxTentatives; }
    public void setMaxTentatives(Integer maxTentatives) { this.maxTentatives = maxTentatives; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public Integer getImageSize() { return imageSize; }
    public void setImageSize(Integer imageSize) { this.imageSize = imageSize; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getChapitreId() { return chapitreId; }
    public void setChapitreId(Integer chapitreId) { this.chapitreId = chapitreId; }

    // Affichage du quiz sous forme de texte (utile pour le debug)
    @Override
    public String toString() {
        return "Quiz{id=" + id + ", titre='" + titre + "', etat='" + etat + "'}";
    }
}