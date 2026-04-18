package tn.esprit.controllers.evenement.front;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;
import tn.esprit.services.EquipeService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Calendrier mensuel des événements — reproduit le comportement du CalendarBundle Symfony.
 * Couleurs par type : Workshop=violet, Conference=rose, Hackathon=bleu, Annulé=gris, Passé=vert pâle
 */
public class CalendrierEvenementsController {

    @FXML private tn.esprit.controllers.NavbarController navbarController;
    @FXML private Label labelMoisAnnee;
    @FXML private GridPane calendarGrid;

    private YearMonth currentMonth = YearMonth.now();
    private final EvenementService evenementService = new EvenementService();
    private final EquipeService equipeService = new EquipeService();
    private List<Evenement> allEvents = new ArrayList<>();

    private static final DateTimeFormatter FMT_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String[] JOURS = {"Lun.", "Mar.", "Mer.", "Jeu.", "Ven.", "Sam.", "Dim."};

    @FXML
    public void initialize() {
        allEvents = evenementService.getAll();
        renderCalendar();
    }

    private void renderCalendar() {
        labelMoisAnnee.setText(
            currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
            + " " + currentMonth.getYear()
        );
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        // 7 colonnes égales
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(cc);
        }

        // Header jours
        for (int i = 0; i < 7; i++) {
            Label h = new Label(JOURS[i]);
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            h.setStyle("-fx-font-size:12; -fx-font-weight:700; -fx-text-fill:#7a6ad8;"
                    + "-fx-background-color:#f4f3ff; -fx-padding:10 4 10 4;"
                    + "-fx-border-color:#e8e4ff; -fx-border-width:0 0 1 0;");
            calendarGrid.add(h, i, 0);
        }

        // Première case = lundi de la semaine contenant le 1er du mois
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int startDow = firstOfMonth.getDayOfWeek().getValue() - 1; // 0=lundi
        LocalDate cellDate = firstOfMonth.minusDays(startDow);

        int totalCells = 42; // 6 semaines
        for (int cell = 0; cell < totalCells; cell++) {
            int col = cell % 7;
            int row = cell / 7 + 1;
            buildDayCell(cellDate, col, row);
            cellDate = cellDate.plusDays(1);
        }
    }

    private void buildDayCell(LocalDate date, int col, int row) {
        VBox cell = new VBox(3);
        cell.setPadding(new Insets(5));
        cell.setMinHeight(95);
        boolean isCurrentMonth = date.getMonth() == currentMonth.getMonth();
        boolean isToday = date.equals(LocalDate.now());

        String baseBg = isToday
                ? "-fx-background-color:rgba(102,126,234,0.12);"
                : isCurrentMonth
                    ? "-fx-background-color:#fafbff;"
                    : "-fx-background-color:#f3f4f8;";
        cell.setStyle(baseBg + "-fx-border-color:#e8e4ff; -fx-border-width:0.5;");

        // Hover sur la cellule
        cell.setOnMouseEntered(e -> {
            if (!isToday) cell.setStyle("-fx-background-color:rgba(102,126,234,0.06);"
                    + "-fx-border-color:#c5bef5; -fx-border-width:0.5;");
        });
        cell.setOnMouseExited(e -> cell.setStyle(baseBg + "-fx-border-color:#e8e4ff; -fx-border-width:0.5;"));

        // Numéro du jour
        if (isToday) {
            Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
            dayNum.setStyle("-fx-font-size:12; -fx-font-weight:700; -fx-text-fill:white;");
            StackPane circle = new StackPane(dayNum);
            circle.setStyle("-fx-background-color:#667eea; -fx-background-radius:50%;");
            circle.setMinSize(28, 28);
            circle.setMaxSize(28, 28);
            circle.setAlignment(Pos.CENTER);
            cell.getChildren().add(circle);
        } else {
            Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
            dayNum.setStyle("-fx-font-size:12; -fx-font-weight:" + (isCurrentMonth ? "600" : "400") + ";"
                    + "-fx-text-fill:" + (isCurrentMonth ? "#444" : "#bbb") + ";");
            cell.getChildren().add(dayNum);
        }

        // Événements du jour
        List<Evenement> dayEvents = getEventsForDay(date);
        int shown = 0;
        for (Evenement ev : dayEvents) {
            if (shown >= 3) {
                Label more = new Label("+" + (dayEvents.size() - 3) + " autres");
                more.setStyle("-fx-font-size:9; -fx-text-fill:#888; -fx-padding:1 4 1 4;");
                cell.getChildren().add(more);
                break;
            }
            cell.getChildren().add(buildEventChip(ev));
            shown++;
        }

        calendarGrid.add(cell, col, row);
    }

    private Label buildEventChip(Evenement ev) {
        String color = getColorForEvent(ev);
        String time = ev.getDateDebut() != null
                ? ev.getDateDebut().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        Label chip = new Label(time + " " + ev.getTitre());
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white;"
                + "-fx-font-size:10; -fx-font-weight:600; -fx-background-radius:5;"
                + "-fx-padding:3 6 3 6; -fx-cursor:hand;");
        chip.setEllipsisString("…");
        chip.setTextOverrun(OverrunStyle.ELLIPSIS);
        chip.setOnMouseEntered(e -> chip.setStyle(chip.getStyle()
                + "-fx-opacity:0.82; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.2),4,0,0,1);"));
        chip.setOnMouseExited(e -> chip.setStyle(
                "-fx-background-color:" + color + "; -fx-text-fill:white;"
                + "-fx-font-size:10; -fx-font-weight:600; -fx-background-radius:5;"
                + "-fx-padding:3 6 3 6; -fx-cursor:hand;"));
        chip.setOnMouseClicked(e -> showEventDetail(ev));
        return chip;
    }

    private List<Evenement> getEventsForDay(LocalDate date) {
        List<Evenement> result = new ArrayList<>();
        for (Evenement ev : allEvents) {
            if (ev.getDateDebut() == null) continue;
            LocalDate evDate = ev.getDateDebut().toLocalDate();
            if (evDate.equals(date)) result.add(ev);
        }
        return result;
    }

    private String getColorForEvent(Evenement ev) {
        if (ev.isIsCanceled() || "Annulé".equals(ev.getStatus())) return "#95a5a6";
        boolean isPast = ev.getDateFin() != null && LocalDateTime.now().isAfter(ev.getDateFin());
        if (isPast) return "#7fb77e";
        if (ev.getType() == null) return "#7a6ad8";
        return switch (ev.getType()) {
            case "Workshop"   -> "#667eea";
            case "Conference" -> "#f093fb";
            case "Hackathon"  -> "#4facfe";
            case "Seminar"    -> "#43e97b";
            default           -> "#7a6ad8";
        };
    }

    /** Affiche la modal de détail dans un Stage dédié — centré, animé, avec emojis */
    private void showEventDetail(Evenement ev) {
        javafx.stage.Window owner = calendarGrid.getScene().getWindow();
        double winW = owner.getWidth();
        double winH = owner.getHeight();

        // ── Modal card ───────────────────────────────────────────
        VBox modal = new VBox(0);
        modal.setPrefWidth(560);
        modal.setMaxWidth(560);
        // La modal prend 85% de la hauteur de la fenêtre
        double modalH = winH * 0.85;
        modal.setPrefHeight(modalH);
        modal.setMaxHeight(modalH);
        modal.setStyle("-fx-background-color:white; -fx-background-radius:20;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),30,0,0,8);");

        // ── Header gradient ──────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 24, 22, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#667eea,#764ba2);"
                + "-fx-background-radius:20 20 0 0;");
        Label titre = new Label(ev.getTitre());
        titre.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:white;");
        titre.setWrapText(true);
        titre.setMaxWidth(440);
        HBox.setHgrow(titre, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.25); -fx-text-fill:white;"
                + "-fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%;"
                + "-fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        header.getChildren().addAll(titre, closeBtn);

        // ── Body scrollable — prend tout l'espace restant ────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color:white;");
        body.getChildren().addAll(
            detailRow("🏷️", "TYPE",         ev.getType() != null ? ev.getType() : "N/A"),
            detailRow("📍", "LIEU",          ev.getLieu() != null ? ev.getLieu() : "N/A"),
            detailRow("📅", "DÉBUT",         ev.getDateDebut() != null ? ev.getDateDebut().format(FMT_FULL) : "N/A"),
            detailRow("🏁", "FIN",           ev.getDateFin()   != null ? ev.getDateFin().format(FMT_FULL)   : "N/A"),
            buildStatutRow(ev),
            buildParticipationsRow(ev),
            buildDescriptionRow(ev)
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:white; -fx-background:white; -fx-border-width:0; -fx-padding:0;");
        VBox.setVgrow(scroll, Priority.ALWAYS); // scroll prend tout l'espace vertical disponible

        // ── Footer ───────────────────────────────────────────────
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 24, 18, 24));
        footer.setStyle("-fx-background-color:#f8f9fa; -fx-background-radius:0 0 20 20;"
                + "-fx-border-color:#eeeeee; -fx-border-width:1 0 0 0;");
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color:white; -fx-text-fill:#555; -fx-font-size:13;"
                + "-fx-font-weight:600; -fx-padding:10 28 10 28; -fx-background-radius:25;"
                + "-fx-border-color:#d0d0d0; -fx-border-radius:25; -fx-border-width:1.5;"
                + "-fx-cursor:hand;");
        footer.getChildren().add(fermerBtn);

        modal.getChildren().addAll(header, scroll, footer);

        // ── Stage transparent centré ─────────────────────────────
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dialog.initOwner(owner);

        StackPane root = new StackPane(modal);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:rgba(0,0,0,0.62);");

        Runnable close = () -> {
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(180), root);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> dialog.close());
            ft.play();
        };
        closeBtn.setOnAction(e -> close.run());
        fermerBtn.setOnAction(e -> close.run());
        root.setOnMouseClicked(e -> { if (e.getTarget() == root) close.run(); });

        javafx.scene.Scene scene = new javafx.scene.Scene(root, winW, winH);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.setX(owner.getX());
        dialog.setY(owner.getY());

        // Animation
        root.setOpacity(0);
        modal.setTranslateY(45);
        dialog.show();

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(220), root);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        javafx.animation.TranslateTransition slideUp = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(260), modal);
        slideUp.setFromY(45); slideUp.setToY(0);
        slideUp.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(fadeIn, slideUp).play();
    }

    private HBox detailRow(String icon, String label, String value) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setStyle("-fx-background-color:#f8f9fa; -fx-background-radius:12; -fx-cursor:default;");

        // Hover animation
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color:#eef0ff; -fx-background-radius:12; -fx-cursor:default;"
                + "-fx-translate-x:5;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color:#f8f9fa; -fx-background-radius:12; -fx-cursor:default;"
                + "-fx-translate-x:0;"));

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:22; -fx-min-width:34; -fx-alignment:CENTER;");

        VBox content = new VBox(3);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:#667eea;"
                + "-fx-letter-spacing:0.5;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:14; -fx-text-fill:#333;");
        val.setWrapText(true);
        content.getChildren().addAll(lbl, val);
        HBox.setHgrow(content, Priority.ALWAYS);
        row.getChildren().addAll(iconLbl, content);
        return row;
    }

    private HBox buildStatutRow(Evenement ev) {
        String statut = ev.computeStatus();
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setStyle("-fx-background-color:#f8f9fa; -fx-background-radius:12;");
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color:#eef0ff; -fx-background-radius:12; -fx-translate-x:5;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color:#f8f9fa; -fx-background-radius:12; -fx-translate-x:0;"));

        Label iconLbl = new Label("📊");
        iconLbl.setStyle("-fx-font-size:22; -fx-min-width:34; -fx-alignment:CENTER;");
        VBox content = new VBox(4);
        Label lbl = new Label("STATUT");
        lbl.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:#667eea;");
        Label badge = new Label(statut.toUpperCase());
        String badgeStyle = switch (statut) {
            case "Plannifié" -> "-fx-background-color:rgba(102,126,234,0.15); -fx-text-fill:#667eea;";
            case "En cours"  -> "-fx-background-color:rgba(67,233,123,0.15); -fx-text-fill:#27ae60;";
            case "Passé"     -> "-fx-background-color:rgba(127,183,126,0.2); -fx-text-fill:#5a9e59;";
            case "Annulé"    -> "-fx-background-color:rgba(149,165,166,0.2); -fx-text-fill:#7f8c8d;";
            default          -> "-fx-background-color:#f0f0f0; -fx-text-fill:#666;";
        };
        badge.setStyle(badgeStyle + "-fx-font-size:12; -fx-font-weight:700;"
                + "-fx-background-radius:20; -fx-padding:5 16 5 16;");
        content.getChildren().addAll(lbl, badge);
        row.getChildren().addAll(iconLbl, content);
        return row;
    }

    private HBox buildParticipationsRow(Evenement ev) {
        int nb = equipeService.countByEvenement(ev.getId());
        return detailRow("👥", "PARTICIPATIONS", nb + " / " + ev.getNbMax() + " équipes");
    }

    private HBox buildDescriptionRow(Evenement ev) {
        String desc = ev.getDescription() != null ? ev.getDescription() : "Aucune description";
        if (desc.length() > 120) desc = desc.substring(0, 120) + "…";
        return detailRow("📝", "DESCRIPTION", desc);
    }

    @FXML private void onPrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        renderCalendar();
    }

    @FXML private void onNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        renderCalendar();
    }

    @FXML private void onAujourdhui() {
        currentMonth = YearMonth.now();
        renderCalendar();
    }

    @FXML private void onRetourListe() { FrontNavHelper.goEvenements(); }
    @FXML private void onHome()        { FrontNavHelper.goHome(); }
    @FXML private void onEvenements()  { FrontNavHelper.goEvenements(); }
    @FXML private void onProfile()     { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout()      { FrontNavHelper.goLogout(); }
}
