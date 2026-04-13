package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.ServiceChapitre;

import java.util.List;

/**
 * FrontChapitreDetailController — affiche le contenu complet d'un chapitre.
 * Vue : frontoffice/chapitre/detail.fxml
 *
 * Cette vue est ouverte quand l'étudiant clique sur "Lire le chapitre".
 * Elle affiche :
 * - le badge "Chapitre N" et le nom du cours
 * - le titre complet du chapitre
 * - le contenu intégral (pas tronqué)
 * - le lien ressource si disponible
 * - un bouton "Chapitre suivant" pour naviguer sans revenir à la grille
 * - un bouton "← Retour" pour revenir à la grille des chapitres
 *
 * La liste complète des chapitres est rechargée depuis la BDD pour permettre
 * la navigation "suivant" sans dépendre de la grille parente.
 */
public class FrontChapitreDetailController {

    // ── Composants FXML ───────────────────────────────────────────────────────
    @FXML private Label  labelBadge;    // "📌 Chapitre N"
    @FXML private Label  labelCours;    // "🎓 [titre du cours]"
    @FXML private Label  labelTitre;    // titre complet du chapitre
    @FXML private Label  labelContenu;  // contenu intégral
    @FXML private Label  labelRessource; // lien URL de la ressource
    @FXML private VBox   boxRessource;  // section ressource (cachée si pas de ressource)
    @FXML private Button btnSuivant;    // bouton "Chapitre suivant →" (caché sur le dernier)

    // ── État interne ──────────────────────────────────────────────────────────
    private Cours          cours;         // cours parent du chapitre affiché
    private List<Chapitre> chapitres;     // liste complète des chapitres du cours (pour navigation)
    private int            currentIndex;  // index du chapitre actuellement affiché dans la liste

    // Callback pour revenir à la grille des chapitres (injecté par FrontofficeController)
    private Runnable onRetourCallback;

    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    // ── INITIALISATION ────────────────────────────────────────────────────────
    /**
     * Appelé par FrontofficeController pour injecter le contexte complet.
     *
     * @param cours     le cours parent (pour afficher son titre et charger ses chapitres)
     * @param chapitre  le chapitre à afficher en premier
     * @param onRetour  callback exécuté quand l'étudiant clique sur "← Retour"
     */
    public void setChapitre(Cours cours, Chapitre chapitre, Runnable onRetour) {
        this.cours             = cours;
        this.onRetourCallback  = onRetour;
        // Charger tous les chapitres du cours pour permettre la navigation "suivant"
        this.chapitres         = serviceChapitre.consulterParCoursId(cours.getId());

        // Trouver l'index du chapitre dans la liste (par référence d'abord)
        this.currentIndex = chapitres.indexOf(chapitre);
        // Fallback : si indexOf retourne -1 (objet différent mais même id), chercher par id
        if (currentIndex < 0) {
            for (int i = 0; i < chapitres.size(); i++) {
                if (chapitres.get(i).getId() == chapitre.getId()) {
                    currentIndex = i;
                    break;
                }
            }
        }

        afficher(chapitre); // afficher le chapitre
    }

    // ── AFFICHAGE D'UN CHAPITRE ───────────────────────────────────────────────
    /**
     * Met à jour tous les composants de la vue avec les données du chapitre.
     * Gère aussi la visibilité du bouton "suivant" et de la section ressource.
     */
    private void afficher(Chapitre chapitre) {
        labelBadge.setText("📌  Chapitre " + chapitre.getOrdre());
        labelCours.setText("🎓  " + cours.getTitre());
        labelTitre.setText(chapitre.getTitre());
        labelContenu.setText(chapitre.getContenu() == null ? "" : chapitre.getContenu());

        // Afficher la section ressource seulement si un lien est renseigné
        String res = chapitre.getRessources();
        if (res != null && !res.isBlank()) {
            labelRessource.setText(res);
            boxRessource.setVisible(true);
            boxRessource.setManaged(true);
        } else {
            boxRessource.setVisible(false);
            boxRessource.setManaged(false);
        }

        // Cacher le bouton "suivant" si on est sur le dernier chapitre
        boolean hasSuivant = currentIndex < chapitres.size() - 1;
        btnSuivant.setVisible(hasSuivant);
        btnSuivant.setManaged(hasSuivant);
    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────

    /** Retourne à la grille des chapitres en exécutant le callback injecté. */
    @FXML
    private void onRetour() {
        if (onRetourCallback != null) onRetourCallback.run();
    }

    /**
     * Passe au chapitre suivant dans la liste sans revenir à la grille.
     * Le bouton est caché automatiquement sur le dernier chapitre.
     */
    @FXML
    private void onSuivant() {
        if (currentIndex < chapitres.size() - 1) {
            currentIndex++;
            afficher(chapitres.get(currentIndex)); // afficher le chapitre suivant
        }
    }
}
