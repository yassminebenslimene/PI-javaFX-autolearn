package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Communaute;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.ServiceCommunaute;
import tn.esprit.session.SessionManager;

public class CommunauteFormController {

    @FXML private Label labelTitle;
    @FXML private TextField fieldNom;
    @FXML private TextArea fieldDescription;
    @FXML private TextField fieldOwnerId;
    @FXML private Label labelError;
    @FXML private Button btnSave;

    private final ServiceCommunaute service = new ServiceCommunaute();
    private Communaute communaute;
    private CommunauteController parentController;

    public void setCommunaute(Communaute c, CommunauteController parent) {
        this.parentController = parent;
        this.communaute = c;
        if (c != null) {
            labelTitle.setText("Modifier la Communauté");
            fieldNom.setText(c.getNom());
            fieldDescription.setText(c.getDescription());
            fieldOwnerId.setText(String.valueOf(c.getOwnerId()));
        }
    }

    @FXML
    public void onSave() {
        String nom      = fieldNom.getText().trim();
        String desc     = fieldDescription.getText().trim();
        String ownerStr = fieldOwnerId.getText().trim();

        labelError.setText("");

        if (nom.length() < 3 || nom.length() > 80) {
            labelError.setText("Le nom doit contenir entre 3 et 80 caractères.");
            return;
        }
        if (desc.length() < 15) {
            labelError.setText("La description doit contenir au moins 15 caractères.");
            return;
        }
        if (desc.length() > 500) {
            labelError.setText("La description ne peut pas dépasser 500 caractères.");
            return;
        }
        if (ownerStr.isEmpty()) {
            labelError.setText("L'owner ID est obligatoire.");
            return;
        }
        int ownerId;
        try { ownerId = Integer.parseInt(ownerStr); }
        catch (NumberFormatException e) { labelError.setText("Owner ID doit être un nombre entier."); return; }
        if (ownerId <= 0) {
            labelError.setText("Owner ID doit être un nombre positif.");
            return;
        }

        if (communaute == null) {
            service.ajouter(new Communaute(nom, desc, ownerId));
            var admin = SessionManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.created_communaute",
                java.util.Map.of("nom", nom));
        } else {
            communaute.setNom(nom);
            communaute.setDescription(desc);
            communaute.setOwnerId(ownerId);
            service.modifier(communaute);
            var admin = SessionManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.updated_communaute",
                java.util.Map.of("nom", nom));
        }
        retourListe();
    }

    @FXML
    public void onAnnuler() { retourListe(); }

    private void retourListe() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/views/backoffice/communaute/index.fxml"));
            javafx.scene.Parent view = loader.load();
            StackPane contentArea = (StackPane) fieldNom.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
