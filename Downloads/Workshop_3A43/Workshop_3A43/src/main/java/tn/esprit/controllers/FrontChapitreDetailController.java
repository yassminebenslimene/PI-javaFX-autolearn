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
 * FrontChapitreDetailController ÔÇö affiche le contenu complet d'un chapitre.
 * Vue : frontoffice/chapitre/detail.fxml
 *
 * Cette vue est ouverte quand l'├®tudiant clique sur "Lire le chapitre".
 * Elle affiche :
 * - le badge "Chapitre N" et le nom du cours
 * - le titre complet du chapitre
 * - le contenu int├®gral (pas tronqu├®)
 * - le lien ressource si disponible
 * - un bouton "Chapitre suivant" pour naviguer sans revenir ├á la grille
 * - un bouton "ÔåÉ Retour" pour revenir ├á la grille des chapitres
 *
 * La liste compl├¿te des chapitres est recharg├®e depuis la BDD pour permettre
 * la navigation "suivant" sans d├®pendre de la grille parente.
 */
public class FrontChapitreDetailController {

    // ÔöÇÔöÇ Composants FXML ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML private Label  labelBadge;    // "­ƒôî Chapitre N"
    @FXML private Label  labelCours;    // "­ƒÄô [titre du cours]"
    @FXML private Label  labelTitre;    // titre complet du chapitre
    @FXML private Label  labelContenu;  // contenu int├®gral
    @FXML private Label  labelRessource; // lien URL de la ressource
    @FXML private VBox   boxRessource;  // section ressource (cach├®e si pas de ressource)
    @FXML private Button btnSuivant;    // bouton "Chapitre suivant ÔåÆ" (cach├® sur le dernier)

    // ÔöÇÔöÇ ├ëtat interne ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    private Cours          cours;         // cours parent du chapitre affich├®
    private List<Chapitre> chapitres;     // liste compl├¿te des chapitres du cours (pour navigation)
    private int            currentIndex;  // index du chapitre actuellement affich├® dans la liste

    // Callback pour revenir ├á la grille des chapitres (inject├® par FrontofficeController)
    private Runnable onRetourCallback;

    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    // ÔöÇÔöÇ INITIALISATION ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /**
     * Appel├® par FrontofficeController pour injecter le contexte complet.
     *
     * @param cours     le cours parent (pour afficher son titre et charger ses chapitres)
     * @param chapitre  le chapitre ├á afficher en premier
     * @param onRetour  callback ex├®cut├® quand l'├®tudiant clique sur "ÔåÉ Retour"
     */
    public void setChapitre(Cours cours, Chapitre chapitre, Runnable onRetour) {
        this.cours             = cours;
        this.onRetourCallback  = onRetour;
        // Charger tous les chapitres du cours pour permettre la navigation "suivant"
        this.chapitres         = serviceChapitre.consulterParCoursId(cours.getId());

        // Trouver l'index du chapitre dans la liste (par r├®f├®rence d'abord)
        this.currentIndex = chapitres.indexOf(chapitre);
        // Fallback : si indexOf retourne -1 (objet diff├®rent mais m├¬me id), chercher par id
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

    // ÔöÇÔöÇ AFFICHAGE D'UN CHAPITRE ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /**
     * Met ├á jour tous les composants de la vue avec les donn├®es du chapitre.
     * G├¿re aussi la visibilit├® du bouton "suivant" et de la section ressource.
     */
    private void afficher(Chapitre chapitre) {
        labelBadge.setText("­ƒôî  Chapitre " + chapitre.getOrdre());
        labelCours.setText("­ƒÄô  " + cours.getTitre());
        labelTitre.setText(chapitre.getTitre());
        labelContenu.setText(chapitre.getContenu() == null ? "" : chapitre.getContenu());

        // Afficher la section ressource seulement si un lien est renseign├®
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

    // ÔöÇÔöÇ ACTIONS ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ

    /** Retourne ├á la grille des chapitres en ex├®cutant le callback inject├®. */
    @FXML
    private void onRetour() {
        if (onRetourCallback != null) onRetourCallback.run();
    }

    /**
     * Passe au chapitre suivant dans la liste sans revenir ├á la grille.
     * Le bouton est cach├® automatiquement sur le dernier chapitre.
     */
    @FXML
    private void onSuivant() {
        if (currentIndex < chapitres.size() - 1) {
            currentIndex++;
            afficher(chapitres.get(currentIndex)); // afficher le chapitre suivant
        }
    }
}