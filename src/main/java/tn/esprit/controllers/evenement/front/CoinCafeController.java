package tn.esprit.controllers.evenement.front;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CoinCafeController {
    private static final float SR = 44100f;
    // [emoji, nom, accent, bg, desc, tag]
    private static final String[][] CAFES = {
        {"\u2615","Espresso","#6b3a2a","#fff3e0","Court, intense","espresso"},
        {"\uD83E\uDD5B","Cappuccino","#c47c3a","#fce4ec","Mousse onctueuse","cappuccino"},
        {"\uD83C\uDF75","Latte","#d4a96a","#fff8e1","Lait chaud, cremeux","latte"},
        {"\uD83E\uDDCB","Americano","#3e2723","#efebe9","Long, leger","americano"},
        {"\uD83C\uDF6B","Mocha","#4e342e","#fbe9e7","Chocolat & cafe","mocha"},
        {"\uD83E\uDDCA","Iced Coffee","#1565c0","#e3f2fd","Frais, glace","iced coffee"},
        {"\uD83C\uDF3F","Flat White","#795548","#f1f8e9","Double ristretto","flat white"},
        {"\u2728","Frappuccino","#e91e63","#fce4ec","Glace, sucre, festif","frappuccino"},
    };

    public static void show(Window owner) {
        double winW = owner.getWidth(), winH = owner.getHeight();
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color:rgba(0,0,0,0.65);");
        root.setAlignment(Pos.CENTER);
        VBox modal = buildModal(dialog, winH);
        modal.setPrefWidth(720); modal.setMaxWidth(720);
        modal.setPrefHeight(winH * 0.92); modal.setMaxHeight(winH * 0.92);
        root.getChildren().add(modal);
        Runnable close = () -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), root);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> dialog.close()); ft.play();
        };
        root.setOnMouseClicked(e -> { if (e.getTarget() == root) close.run(); });
        Scene scene = new Scene(root, winW, winH);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) close.run(); });
        dialog.setScene(scene);
        dialog.setX(owner.getX()); dialog.setY(owner.getY());
        root.setOpacity(0); modal.setTranslateY(50);
        dialog.show();
        FadeTransition fi = new FadeTransition(Duration.millis(250), root);
        fi.setFromValue(0); fi.setToValue(1);
        TranslateTransition su = new TranslateTransition(Duration.millis(300), modal);
        su.setFromY(50); su.setToY(0); su.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, su).play();
    }
    private static VBox buildModal(Stage dialog, double winH) {
        VBox modal = new VBox(0);
        modal.setStyle("-fx-background-color:#1a0a00;-fx-background-radius:24;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),40,0,0,10);");
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20,24,20,28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#c0392b,#e74c3c,#f39c12);-fx-background-radius:24 24 0 0;");
        Label icon = new Label("\u2615");
        icon.setStyle("-fx-font-size:36;");
        ScaleTransition p = new ScaleTransition(Duration.millis(900), icon);
        p.setFromX(1); p.setToX(1.15); p.setFromY(1); p.setToY(1.15);
        p.setAutoReverse(true); p.setCycleCount(Animation.INDEFINITE); p.play();
        VBox ht = new VBox(3); HBox.setHgrow(ht, Priority.ALWAYS);
        Label t1 = new Label("\u2615  Coin Cafe");
        t1.setStyle("-fx-font-size:22;-fx-font-weight:800;-fx-text-fill:white;");
        Label t2 = new Label("Votre pause cafe virtuelle \u2728  -  Open Food Facts + Quotable API");
        t2.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.85);");
        ht.getChildren().addAll(t1, t2);
        Button cb = new Button("\u2715");
        cb.setStyle("-fx-background-color:rgba(255,255,255,0.2);-fx-text-fill:white;-fx-font-size:15;-fx-font-weight:700;-fx-background-radius:50%;-fx-min-width:34;-fx-min-height:34;-fx-max-width:34;-fx-max-height:34;-fx-cursor:hand;-fx-border-width:0;");
        cb.setOnAction(e -> dialog.close());
        header.getChildren().addAll(icon, ht, cb);
        VBox body = new VBox(0);
        body.setStyle("-fx-background-color:#1a0a00;");
        body.getChildren().addAll(buildMachineSection(), buildCafeGrid(dialog, body));
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:#1a0a00;-fx-background:#1a0a00;-fx-border-width:0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12,24,16,24));
        footer.setStyle("-fx-background-color:#0d0500;-fx-background-radius:0 0 24 24;");
        Button fb = new Button("Fermer");
        fb.setStyle("-fx-background-color:#3d1a00;-fx-text-fill:#f39c12;-fx-font-size:13;-fx-font-weight:600;-fx-padding:10 28 10 28;-fx-background-radius:25;-fx-border-color:#f39c12;-fx-border-radius:25;-fx-border-width:1.5;-fx-cursor:hand;");
        fb.setOnAction(e -> dialog.close());
        footer.getChildren().add(fb);
        modal.getChildren().addAll(header, scroll, footer);
        return modal;
    }

    private static VBox buildMachineSection() {
        VBox sec = new VBox(6);
        sec.setAlignment(Pos.CENTER);
        sec.setPadding(new Insets(18,20,8,20));
        sec.setStyle("-fx-background-color:linear-gradient(to bottom,#2d0a00,#1a0a00);");
        HBox steam = new HBox(10); steam.setAlignment(Pos.CENTER);
        for (String s : new String[]{"\u3030","\u3030\u3030","\u3030"}) {
            Label sl = new Label(s); sl.setStyle("-fx-font-size:16;-fx-text-fill:#bbbbbb;");
            steam.getChildren().add(sl);
        }
        FadeTransition sf = new FadeTransition(Duration.millis(1400), steam);
        sf.setFromValue(0.15); sf.setToValue(1); sf.setAutoReverse(true); sf.setCycleCount(Animation.INDEFINITE); sf.play();
        TranslateTransition sr = new TranslateTransition(Duration.millis(1800), steam);
        sr.setFromY(0); sr.setToY(-8); sr.setAutoReverse(true); sr.setCycleCount(Animation.INDEFINITE); sr.play();
        StackPane machine = new StackPane();
        machine.setPrefSize(220,150); machine.setMaxSize(220,150);
        VBox mb = new VBox(4); mb.setAlignment(Pos.CENTER);
        mb.setPrefSize(200,135); mb.setMaxSize(200,135);
        mb.setStyle("-fx-background-color:linear-gradient(to bottom right,#e74c3c,#c0392b,#922b21);-fx-background-radius:20;-fx-border-color:#f1948a88;-fx-border-radius:20;-fx-border-width:2;-fx-effect:dropshadow(gaussian,rgba(231,76,60,0.7),24,0,0,6);");
        HBox gauges = new HBox(12); gauges.setAlignment(Pos.CENTER); gauges.setPadding(new Insets(10,0,0,0));
        for (int i = 0; i < 3; i++) {
            Label g = new Label(i==1?"\u23F1":"\u2299");
            g.setStyle("-fx-font-size:"+(i==1?16:13)+";-fx-text-fill:#f5cba7;-fx-background-color:#7b241c;-fx-background-radius:50%;-fx-padding:5 6 5 6;-fx-min-width:30;-fx-min-height:30;-fx-alignment:CENTER;");
            if (i==1) { RotateTransition rt = new RotateTransition(Duration.millis(4000), g); rt.setByAngle(360); rt.setCycleCount(Animation.INDEFINITE); rt.setInterpolator(Interpolator.LINEAR); rt.play(); }
            gauges.getChildren().add(g);
        }
        Label arm = new Label("\u2501\u2501\u2501\u2501\u2501"); arm.setStyle("-fx-font-size:13;-fx-text-fill:#d4a96a;");
        Label cup = new Label("\uD83C\uDF75"); cup.setStyle("-fx-font-size:30;");
        mb.getChildren().addAll(gauges, arm, cup);
        machine.getChildren().add(mb);
        TranslateTransition bounce = new TranslateTransition(Duration.millis(1100), machine);
        bounce.setFromY(0); bounce.setToY(-5); bounce.setAutoReverse(true); bounce.setCycleCount(Animation.INDEFINITE); bounce.setInterpolator(Interpolator.EASE_BOTH); bounce.play();
        Label ml = new Label("\uD83D\uDD34  Machine a Cafe Premium  -  Powered by Open Food Facts");
        ml.setStyle("-fx-font-size:11;-fx-font-weight:700;-fx-text-fill:#f39c12;");
        sec.getChildren().addAll(steam, machine, ml);
        return sec;
    }
    private static VBox buildCafeGrid(Stage dialog, VBox parentBody) {
        VBox sec = new VBox(12);
        sec.setPadding(new Insets(14,18,18,18));
        sec.setStyle("-fx-background-color:#1a0a00;");
        Label title = new Label("\u2615  Choisissez votre cafe");
        title.setStyle("-fx-font-size:15;-fx-font-weight:800;-fx-text-fill:#f39c12;");
        sec.getChildren().add(title);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS); cc.setFillWidth(true); cc.setPercentWidth(25);
            grid.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < CAFES.length; i++) {
            VBox card = buildCafeCard(CAFES[i], dialog, parentBody);
            card.setOpacity(0); card.setTranslateY(20);
            grid.add(card, i%4, i/4);
            int delay = i*60;
            FadeTransition ft = new FadeTransition(Duration.millis(350), card);
            ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(delay));
            TranslateTransition tt = new TranslateTransition(Duration.millis(350), card);
            tt.setFromY(20); tt.setToY(0); tt.setDelay(Duration.millis(delay)); tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
        }
        sec.getChildren().add(grid);
        return sec;
    }

    private static VBox buildCafeCard(String[] c, Stage dialog, VBox parentBody) {
        String emoji=c[0],nom=c[1],accent=c[2],bg=c[3],desc=c[4];
        VBox card = new VBox(6); card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14,8,14,8)); card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color:"+bg+";-fx-background-radius:16;-fx-border-color:"+accent+"88;-fx-border-radius:16;-fx-border-width:2;-fx-effect:dropshadow(gaussian,"+accent+"55,10,0,0,3);-fx-cursor:hand;");
        Label el = new Label(emoji);
        el.setStyle("-fx-font-size:52;-fx-alignment:CENTER;");
        el.setMinWidth(64); el.setMinHeight(64); el.setAlignment(javafx.geometry.Pos.CENTER);
        Label nl = new Label(nom);
        nl.setStyle("-fx-font-size:12;-fx-font-weight:800;-fx-text-fill:#1e1e1e;");
        nl.setWrapText(true); nl.setMaxWidth(100); nl.setAlignment(Pos.CENTER);
        Label dl = new Label(desc);
        dl.setStyle("-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.65);");
        dl.setWrapText(true); dl.setMaxWidth(100); dl.setAlignment(Pos.CENTER);
        card.getChildren().addAll(el, nl, dl);
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color:"+accent+"55;-fx-background-radius:16;-fx-border-color:"+accent+";-fx-border-radius:16;-fx-border-width:2.5;-fx-effect:dropshadow(gaussian,"+accent+"88,18,0,0,6);-fx-cursor:hand;");
            ScaleTransition st = new ScaleTransition(Duration.millis(120), card); st.setToX(1.06); st.setToY(1.06); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color:#2C1A0E;-fx-background-radius:16;-fx-border-color:"+accent+";-fx-border-radius:16;-fx-border-width:2;-fx-effect:dropshadow(gaussian,"+accent+"55,10,0,0,3);-fx-cursor:hand;");
            ScaleTransition st = new ScaleTransition(Duration.millis(120), card); st.setToX(1); st.setToY(1); st.play();
        });
        card.setOnMouseClicked(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), card); st.setToX(0.94); st.setToY(0.94);
            st.setOnFinished(ev -> { ScaleTransition st2 = new ScaleTransition(Duration.millis(80), card); st2.setToX(1); st2.setToY(1); st2.setOnFinished(ev2 -> showPreparation(c, dialog, parentBody)); st2.play(); });
            st.play();
        });
        return card;
    }

    private static void showPreparation(String[] cafe, Stage dialog, VBox parentBody) {
        String emoji=cafe[0],nom=cafe[1],accent=cafe[2];
        parentBody.getChildren().clear();
        VBox box = new VBox(18); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(40)); box.setStyle("-fx-background-color:#1a0a00;");
        Label tl = new Label("Preparation en cours..."); tl.setStyle("-fx-font-size:18;-fx-font-weight:800;-fx-text-fill:#f39c12;");
        Label bm = new Label("\u2615"); bm.setStyle("-fx-font-size:80;");
        ScaleTransition mp = new ScaleTransition(Duration.millis(600), bm); mp.setFromX(1); mp.setToX(1.12); mp.setFromY(1); mp.setToY(1.12); mp.setAutoReverse(true); mp.setCycleCount(Animation.INDEFINITE); mp.play();
        HBox sr = new HBox(8); sr.setAlignment(Pos.CENTER);
        for (String s : new String[]{"\uD83D\uDCA8","\u3030","\uD83D\uDCA8","\u3030","\uD83D\uDCA8"}) { Label sl = new Label(s); sl.setStyle("-fx-font-size:20;-fx-text-fill:#cccccc;"); sr.getChildren().add(sl); }
        FadeTransition sa = new FadeTransition(Duration.millis(500), sr); sa.setFromValue(0.2); sa.setToValue(1); sa.setAutoReverse(true); sa.setCycleCount(Animation.INDEFINITE); sa.play();
        Label cl = new Label(emoji+"  "+nom); cl.setStyle("-fx-font-size:22;-fx-font-weight:800;-fx-text-fill:white;");
        ProgressBar pb = new ProgressBar(0); pb.setPrefWidth(320); pb.setPrefHeight(18);
        pb.setStyle("-fx-accent:"+accent+";-fx-background-color:#3d1a00;-fx-background-radius:10;-fx-border-radius:10;");
        Label pl = new Label("Chauffage de l'eau..."); pl.setStyle("-fx-font-size:13;-fx-text-fill:#f39c12;");
        String[] steps = {"Chauffage de l'eau...","Mouture du cafe...","Extraction en cours...","Ajout de la mousse...","Finalisation..."};
        box.getChildren().addAll(tl, bm, sr, cl, pb, pl);
        parentBody.getChildren().add(box);
        playCoffeeMachineSound();
        Timeline tl2 = new Timeline();
        for (int i = 0; i <= 100; i++) {
            final double prog = i/100.0; final String step = steps[Math.min(i/20, steps.length-1)];
            tl2.getKeyFrames().add(new KeyFrame(Duration.millis(i*40), ev -> { pb.setProgress(prog); pl.setText(step); }));
        }
        tl2.setOnFinished(ev -> showCafeReady(cafe, dialog, parentBody));
        tl2.play();
    }
    private static void showCafeReady(String[] cafe, Stage dialog, VBox parentBody) {
        String emoji=cafe[0],nom=cafe[1],accent=cafe[2],tag=cafe[5];
        parentBody.getChildren().clear();
        VBox box = new VBox(16); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(30,40,30,40)); box.setStyle("-fx-background-color:#1a0a00;");
        Label be = new Label(emoji); be.setStyle("-fx-font-size:90;-fx-effect:dropshadow(gaussian,"+accent+"99,20,0,0,0);"); be.setOpacity(0); be.setScaleX(0.3); be.setScaleY(0.3);
        Label ba = new Label("\uD83D\uDE0A  Bon appetit !  \u2615\u2728"); ba.setStyle("-fx-font-size:26;-fx-font-weight:800;-fx-text-fill:#f39c12;"); ba.setOpacity(0);
        Label nl = new Label(nom+" est pret !"); nl.setStyle("-fx-font-size:18;-fx-font-weight:700;-fx-text-fill:white;"); nl.setOpacity(0);
        HBox conf = new HBox(8); conf.setAlignment(Pos.CENTER);
        for (String s : new String[]{"\uD83C\uDF89","\u2B50","\u2728","\uD83C\uDF8A","\uD83D\uDCAB","\uD83C\uDF1F","\uD83C\uDF89"}) { Label cl = new Label(s); cl.setStyle("-fx-font-size:22;"); conf.getChildren().add(cl); }
        conf.setOpacity(0);
        VBox nutBox = new VBox(8); nutBox.setAlignment(Pos.CENTER_LEFT); nutBox.setPadding(new Insets(14,18,14,18));
        nutBox.setStyle("-fx-background-color:#2d0a00;-fx-background-radius:14;-fx-border-color:"+accent+"66;-fx-border-radius:14;-fx-border-width:1.5;");
        Label nutTitle = new Label("\uD83D\uDCCA  Infos nutritionnelles  -  Open Food Facts"); nutTitle.setStyle("-fx-font-size:12;-fx-font-weight:700;-fx-text-fill:"+accent+";");
        Label nutLoad = new Label("\u23F3  Chargement des donnees..."); nutLoad.setStyle("-fx-font-size:12;-fx-text-fill:#aaaaaa;");
        nutBox.getChildren().addAll(nutTitle, nutLoad); nutBox.setOpacity(0);
        VBox qBox = new VBox(6); qBox.setAlignment(Pos.CENTER); qBox.setPadding(new Insets(12,18,12,18));
        qBox.setStyle("-fx-background-color:#2d1500;-fx-background-radius:14;-fx-border-color:#f39c1266;-fx-border-radius:14;-fx-border-width:1.5;");
        Label qTitle = new Label("\uD83D\uDCAC  Citation du moment  -  Quotable API"); qTitle.setStyle("-fx-font-size:11;-fx-font-weight:700;-fx-text-fill:#f39c12;");
        Label qLoad = new Label("\u23F3  Chargement..."); qLoad.setStyle("-fx-font-size:12;-fx-text-fill:#aaaaaa;-fx-font-style:italic;");
        qBox.getChildren().addAll(qTitle, qLoad); qBox.setOpacity(0);
        Button rb = new Button("\u2615  Choisir un autre cafe");
        rb.setStyle("-fx-background-color:linear-gradient(to right,#c0392b,#f39c12);-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:700;-fx-padding:12 28 12 28;-fx-background-radius:25;-fx-cursor:hand;-fx-border-width:0;");
        rb.setOpacity(0);
        rb.setOnAction(e -> { parentBody.getChildren().clear(); parentBody.getChildren().addAll(buildMachineSection(), buildCafeGrid(dialog, parentBody)); });
        VBox deliveryBox = new VBox(6); deliveryBox.setAlignment(javafx.geometry.Pos.CENTER);
        deliveryBox.setPadding(new Insets(12,20,12,20));
        deliveryBox.setStyle("-fx-background-color:linear-gradient(to right,#1a4a1a,#0d2d0d);-fx-background-radius:14;-fx-border-color:#27ae6088;-fx-border-radius:14;-fx-border-width:1.5;");
        Label deliveryIcon = new Label("\uD83D\uDEB6  Livraison en cours");
        deliveryIcon.setStyle("-fx-font-size:13;-fx-font-weight:800;-fx-text-fill:#2ecc71;");
        Label deliveryMsg = new Label("Un membre de l'equipe organisatrice se dirige vers votre table. Votre commande vous sera remise dans quelques instants.");
        deliveryMsg.setStyle("-fx-font-size:11;-fx-text-fill:#a8d5a2;-fx-font-style:italic;");
        deliveryMsg.setWrapText(true); deliveryMsg.setMaxWidth(400); deliveryMsg.setAlignment(javafx.geometry.Pos.CENTER);
        deliveryBox.getChildren().addAll(deliveryIcon, deliveryMsg);
        deliveryBox.setOpacity(0);
        box.getChildren().addAll(be, ba, nl, conf, deliveryBox, nutBox, qBox, rb);
        parentBody.getChildren().add(box);
        FadeTransition fe = new FadeTransition(Duration.millis(400), be); fe.setFromValue(0); fe.setToValue(1);
        ScaleTransition se = new ScaleTransition(Duration.millis(500), be); se.setFromX(0.3); se.setFromY(0.3); se.setToX(1); se.setToY(1); se.setInterpolator(Interpolator.EASE_OUT);
        se.setOnFinished(ev -> { ScaleTransition b = new ScaleTransition(Duration.millis(150), be); b.setToX(1.15); b.setToY(1.15); b.setOnFinished(ev2 -> { ScaleTransition b2 = new ScaleTransition(Duration.millis(150), be); b2.setToX(1); b2.setToY(1); b2.play(); }); b.play(); });
        FadeTransition fb = new FadeTransition(Duration.millis(350), ba); fb.setFromValue(0); fb.setToValue(1); fb.setDelay(Duration.millis(400));
        FadeTransition fn = new FadeTransition(Duration.millis(350), nl); fn.setFromValue(0); fn.setToValue(1); fn.setDelay(Duration.millis(600));
        FadeTransition fc = new FadeTransition(Duration.millis(350), conf); fc.setFromValue(0); fc.setToValue(1); fc.setDelay(Duration.millis(800));
        FadeTransition fd = new FadeTransition(Duration.millis(400), deliveryBox); fd.setFromValue(0); fd.setToValue(1); fd.setDelay(Duration.millis(950));
        FadeTransition fnu = new FadeTransition(Duration.millis(350), nutBox); fnu.setFromValue(0); fnu.setToValue(1); fnu.setDelay(Duration.millis(1150));
        FadeTransition fq = new FadeTransition(Duration.millis(350), qBox); fq.setFromValue(0); fq.setToValue(1); fq.setDelay(Duration.millis(1200));
        FadeTransition fr = new FadeTransition(Duration.millis(350), rb); fr.setFromValue(0); fr.setToValue(1); fr.setDelay(Duration.millis(1400));
        new ParallelTransition(new ParallelTransition(fe,se),fb,fn,fc,fd,fnu,fq,fr).play();
        for (int i = 0; i < conf.getChildren().size(); i++) {
            javafx.scene.Node n = conf.getChildren().get(i);
            TranslateTransition tt = new TranslateTransition(Duration.millis(600+i*80), n);
            tt.setFromY(0); tt.setToY(-12); tt.setAutoReverse(true); tt.setCycleCount(Animation.INDEFINITE); tt.setDelay(Duration.millis(i*100)); tt.play();
        }
        playSuccessSound();
        fetchNutrition(tag, nutBox, accent);
        fetchQuote(qBox);
    }

    private static void fetchNutrition(String tag, VBox box, String accent) {
        Task<String[]> task = new Task<>() {
            @Override protected String[] call() {
                try {
                    String enc = tag.replace(" ","%20");
                    URL url = new URL("https://world.openfoodfacts.org/cgi/search.pl?search_terms="+enc+"&search_simple=1&action=process&json=1&page_size=1");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("GET"); c.setConnectTimeout(4000); c.setReadTimeout(4000);
                    c.setRequestProperty("User-Agent","AutoLearn/1.0");
                    if (c.getResponseCode()==200) {
                        String json = new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        return new String[]{jv(json,"energy-kcal_100g"),jv(json,"caffeine_100g"),jv(json,"sugars_100g"),jv(json,"fat_100g")};
                    }
                } catch (Exception ignored) {}
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            box.getChildren().clear();
            Label title = new Label("\uD83D\uDCCA  Infos nutritionnelles  -  Open Food Facts"); title.setStyle("-fx-font-size:12;-fx-font-weight:700;-fx-text-fill:"+accent+";");
            box.getChildren().add(title);
            String[] d = task.getValue();
            if (d!=null && d[0]!=null && !d[0].isEmpty()) {
                HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
                String[][] cells = {{"\uD83D\uDD25 Calories",d[0]+" kcal"},{"\u26A1 Cafeine",d[1]!=null?d[1]+" mg":"~80mg"},{"\uD83C\uDF6C Sucres",d[2]!=null?d[2]+" g":"~0g"},{"\uD83E\uDDC8 Lipides",d[3]!=null?d[3]+" g":"~0g"}};
                for (String[] cell : cells) {
                    VBox cv = new VBox(2); cv.setAlignment(Pos.CENTER); cv.setPadding(new Insets(6,10,6,10)); cv.setStyle("-fx-background-color:#3d1a00;-fx-background-radius:12;-fx-border-color:"+accent+"44;-fx-border-radius:12;-fx-border-width:1;-fx-min-width:80;");
                    Label v = new Label(cell[1]); v.setStyle("-fx-font-size:14;-fx-font-weight:800;-fx-text-fill:"+accent+";");
                    Label k = new Label(cell[0]); k.setStyle("-fx-font-size:10;-fx-text-fill:#cccccc;-fx-font-weight:600;");
                    cv.getChildren().addAll(v,k); row.getChildren().add(cv);
                }
                box.getChildren().add(row);
            } else {
                Label fb = new Label("\u2615 ~2 kcal  -  \u26A1 ~80mg cafeine  -  \uD83C\uDF6C ~0g sucres"); fb.setStyle("-fx-font-size:11;-fx-text-fill:#aaaaaa;-fx-font-style:italic;"); box.getChildren().add(fb);
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> { box.getChildren().clear(); Label fb = new Label("\u2615 ~2 kcal  -  \u26A1 ~80mg cafeine  -  \uD83C\uDF6C ~0g sucres"); fb.setStyle("-fx-font-size:11;-fx-text-fill:#aaaaaa;"); box.getChildren().add(fb); }));
        Thread t = new Thread(task,"nutrition-api"); t.setDaemon(true); t.start();
    }

    private static void fetchQuote(VBox box) {
        Task<String[]> task = new Task<>() {
            @Override protected String[] call() {
                try {
                    URL url = new URL("https://api.quotable.io/random?tags=inspirational|motivational&maxLength=120");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("GET"); c.setConnectTimeout(4000); c.setReadTimeout(4000);
                    c.setRequestProperty("User-Agent","AutoLearn/1.0");
                    if (c.getResponseCode()==200) {
                        String json = new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        String content = jv(json,"content"), author = jv(json,"author");
                        if (content!=null && !content.isEmpty()) return new String[]{content,author};
                    }
                } catch (Exception ignored) {}
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            box.getChildren().clear();
            Label title = new Label("\uD83D\uDCAC  Citation du moment  -  Quotable API"); title.setStyle("-fx-font-size:11;-fx-font-weight:700;-fx-text-fill:#f39c12;");
            String[] d = task.getValue();
            String q = d!=null&&d[0]!=null ? d[0] : "Le cafe transforme chaque matin en quelque chose de possible.";
            String a = d!=null&&d[1]!=null ? "- "+d[1] : "- Anonyme";
            Label ql = new Label("\""+q+"\""); ql.setStyle("-fx-font-size:12;-fx-text-fill:white;-fx-font-style:italic;"); ql.setWrapText(true); ql.setMaxWidth(580);
            Label al = new Label(a); al.setStyle("-fx-font-size:11;-fx-text-fill:#f39c12;-fx-font-weight:600;");
            box.getChildren().addAll(title, ql, al);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> { box.getChildren().clear(); Label ql = new Label("\"Le cafe transforme chaque matin en quelque chose de possible.\""); ql.setStyle("-fx-font-size:12;-fx-text-fill:white;-fx-font-style:italic;"); ql.setWrapText(true); ql.setMaxWidth(580); box.getChildren().add(ql); }));
        Thread t = new Thread(task,"quote-api"); t.setDaemon(true); t.start();
    }

    private static String jv(String json, String key) {
        try {
            String s = "\""+key+"\":"; int idx = json.indexOf(s); if (idx<0) return null;
            int start = idx+s.length();
            while (start<json.length() && json.charAt(start)==' ') start++;
            if (start>=json.length()) return null;
            if (json.charAt(start)=='"') { int end = json.indexOf('"', start+1); return end>start ? json.substring(start+1,end) : null; }
            else { int end = start; while (end<json.length() && ",}\n".indexOf(json.charAt(end))<0) end++; return json.substring(start,end).trim(); }
        } catch (Exception e) { return null; }
    }

    private static void playCoffeeMachineSound() {
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR,16,1,true,false);
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                b.write(genNoise(800,0.3f));
                for (int f=80;f<=220;f+=5) b.write(genTone(f,30,0.25f));
                b.write(genVibrato(180,1500,0.3f));
                b.write(genNoise(600,0.2f));
                b.write(genTone(1047,80,0.4f)); b.write(genTone(1319,80,0.4f)); b.write(genTone(1568,200,0.45f));
                playBytes(b.toByteArray(),fmt);
            } catch (Exception ignored) {}
        },"coffee-sound"); t.setDaemon(true); t.start();
    }

    private static void playSuccessSound() {
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR,16,1,true,false);
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                for (double[] n : new double[][]{{523.25,80},{659.25,80},{783.99,80},{1046.5,120},{1318.5,80},{1567.98,200}}) b.write(genTone((int)n[0],(int)n[1],0.45f));
                b.write(genVibrato(1800,250,0.35f));
                playBytes(b.toByteArray(),fmt);
            } catch (Exception ignored) {}
        },"success-sound"); t.setDaemon(true); t.start();
    }

    private static byte[] genTone(int freq, int ms, float vol) {
        int n=(int)(SR*ms/1000.0); byte[] buf=new byte[n*2]; int fi=Math.max(1,n/8),fo=Math.max(1,n/5);
        for (int i=0;i<n;i++) { double t=i/SR; double w=(Math.sin(2*Math.PI*freq*t)+0.3*Math.sin(4*Math.PI*freq*t)+0.15*Math.sin(6*Math.PI*freq*t))/1.45; double env=i<fi?(double)i/fi:i>n-fo?(double)(n-i)/fo:1.0; short s=(short)(w*env*vol*Short.MAX_VALUE); buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF); }
        return buf;
    }

    private static byte[] genNoise(int ms, float vol) {
        int n=(int)(SR*ms/1000.0); byte[] buf=new byte[n*2]; java.util.Random rnd=new java.util.Random(42); int fo=n/4;
        for (int i=0;i<n;i++) { double env=i>n-fo?(double)(n-i)/fo:1.0; short s=(short)(rnd.nextGaussian()*env*vol*Short.MAX_VALUE*0.5); buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF); }
        return buf;
    }

    private static byte[] genVibrato(double freq, int ms, float vol) {
        int n=(int)(SR*ms/1000.0); byte[] buf=new byte[n*2]; int fo=n/4;
        for (int i=0;i<n;i++) { double t=i/SR; double vib=freq+20*Math.sin(2*Math.PI*8*t); double w=(Math.sin(2*Math.PI*vib*t)+0.3*Math.sin(4*Math.PI*vib*t))/1.3; double env=i>n-fo?(double)(n-i)/fo:1.0; short s=(short)(w*env*vol*Short.MAX_VALUE); buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF); }
        return buf;
    }

    private static void playBytes(byte[] data, AudioFormat fmt) throws Exception {
        ByteArrayInputStream bais=new ByteArrayInputStream(data);
        AudioInputStream ais=new AudioInputStream(bais,fmt,data.length/fmt.getFrameSize());
        DataLine.Info info=new DataLine.Info(Clip.class,fmt);
        if (!AudioSystem.isLineSupported(info)) return;
        Clip clip=(Clip)AudioSystem.getLine(info);
        clip.open(ais); clip.start();
        Thread.sleep(clip.getMicrosecondLength()/1000+80);
        clip.close();
    }
}