package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Communaute;
import tn.esprit.services.ServiceCommunaute;

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
        String nom = fieldNom.getText().trim();
        String desc = fieldDescription.getText().trim();
        String ownerStr = fieldOwnerId.getText().trim();

        if (nom.isEmpty() || ownerStr.isEmpty()) {
            labelError.setText("Le nom et l'owner ID sont obligatoires.");
            return;
        }
        int ownerId;
        try { ownerId = Integer.parseInt(ownerStr); }
        catch (NumberFormatException e) { labelError.setText("Owner ID doit être un nombre."); return; }

        if (communaute == null) {
            service.ajouter(new Communaute(nom, desc, ownerId));
        } else {
            communaute.setNom(nom);
            communaute.setDescription(desc);
            communaute.setOwnerId(ownerId);
            service.modifier(communaute);
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
