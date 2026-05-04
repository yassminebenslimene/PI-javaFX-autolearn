package tn.esprit.controllers.evenement.front;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.geometry.Pos;

public class ImageUtil {
    
    // Emojis pour chaque fonctionnalité (fallback si images ne chargent pas)
    public static final String VENDING_EMOJI = "\uD83C\uDFB0";
    public static final String COFFEE_EMOJI = "\u2615";
    public static final String CANDY_EMOJI = "\uD83C\uDFAC";
    public static final String MEMORY_EMOJI = "\uD83C\uDFB4";
    public static final String GAMES_EMOJI = "\uD83C\uDFAE";
    public static final String MENU_EMOJI = "\uD83C\uDF7D\uFE0F";
    public static final String EQUIPMENT_EMOJI = "\uD83D\uDD0C";
    
    // Couleurs pour chaque fonctionnalité
    public static final String VENDING_COLOR = "#ff6b9d";
    public static final String COFFEE_COLOR = "#c0392b";
    public static final String CANDY_COLOR = "#e74c3c";
    public static final String MEMORY_COLOR = "#9b59b6";
    public static final String GAMES_COLOR = "#7c3aed";
    public static final String MENU_COLOR = "#667eea";
    public static final String EQUIPMENT_COLOR = "#10b981";
    
    // Créer un Label emoji avec style
    public static Label createEmojiLabel(String emoji, int fontSize) {
        Label label = new Label(emoji);
        label.setStyle("-fx-font-size:" + fontSize + ";");
        label.setAlignment(Pos.CENTER);
        return label;
    }
    
    // Créer un Label emoji avec background coloré
    public static Label createEmojiLabelWithBg(String emoji, int fontSize, String bgColor) {
        Label label = new Label(emoji);
        label.setStyle("-fx-font-size:" + fontSize + ";"
                + "-fx-background-color:" + bgColor + ";"
                + "-fx-background-radius:50%;"
                + "-fx-padding:12 14 12 14;"
                + "-fx-min-width:" + (fontSize + 28) + ";"
                + "-fx-min-height:" + (fontSize + 28) + ";"
                + "-fx-alignment:CENTER;");
        return label;
    }
    
    // Charger une image depuis une URL avec fallback
    public static ImageView loadImageFromUrl(String url, double width, double height, String fallbackEmoji) {
        try {
            Image image = new Image(url, width, height, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(true);
            return imageView;
        } catch (Exception e) {
            // Fallback sur emoji
            Label fallback = createEmojiLabel(fallbackEmoji, (int)(width / 2));
            fallback.setPrefWidth(width);
            fallback.setPrefHeight(height);
            return null; // Retourner null pour utiliser le fallback emoji
        }
    }
    
    // Charger une image depuis les ressources avec fallback
    public static ImageView loadImageFromResources(String resourcePath, double width, double height, String fallbackEmoji) {
        try {
            String url = ImageUtil.class.getResource(resourcePath).toExternalForm();
            Image image = new Image(url, width, height, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(true);
            return imageView;
        } catch (Exception e) {
            // Fallback sur emoji
            return null;
        }
    }
}
