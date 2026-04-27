package tn.esprit.controllers.evenement.front;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Evenement;
import tn.esprit.services.ParticipationService;

import java.util.*;

/**
 * Espace 3D — Vraie caméra 3D dans un espace monde fixe.
 * Les éléments (tables, étagères, portes) sont FIXES dans l'espace monde.
 * C'est la caméra (camPan) qui se déplace pour explorer la salle.
 * Portes du couloir : vue latérale en perspective sur les murs gauche/droite.
 * Images PNG intégrées pour tables, étagères, vending machine.
 */
public class SalleReservationController {

    @FXML private tn.esprit.controllers.NavbarController navbarController;
    @FXML private StackPane scene3DContainer;
    @FXML private Canvas canvas3D;
    @FXML private Label labelEventName;
    @FXML private Label labelStatus;
    @FXML private Button btnReserver;
    @FXML private Button btnLiberer;

    private Evenement evenement;
    private Equipe equipe;
    private final ParticipationService participationService = new ParticipationService();

    private int currentRoom = 0; // 0=corridor, 1=salleA, 2=salleB, 3=salleC
    private static final String[] ROOM_NAMES = {
        "Couloir Principal", "Salle A — Hackathon", "Salle B — Workshop", "Salle C — Gaming"
    };

    // Caméra : pan horizontal pour explorer la salle (éléments restent fixes)
    // camPan = 0 → vue centrale, -1 → vue gauche, +1 → vue droite
    private double camPan = 0.0;

    // Selection
    private int pendingSalleIdx = -1, pendingTableNum = -1;

    private final Map<Integer,Integer> reservations = new HashMap<>();
    private int myEquipeId = -1;

    private final List<double[]> tableHits = new ArrayList<>();
    private final List<double[]> doorHits  = new ArrayList<>();
    private double[] btnL, btnR, btnC;

    private AnimationTimer timer;

    // Images 3D (chargées une seule fois)
    private Image imgTable   = null;
    private Image imgShelf   = null;
    private Image imgVending = null;

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color WALL    = Color.web("#f0ebe0");
    private static final Color WALL_S  = Color.web("#ddd5c5");
    private static final Color WALL_D  = Color.web("#c8c0b0");
    private static final Color FLOOR_L = Color.web("#d4b888");
    private static final Color FLOOR_D = Color.web("#a07840");
    private static final Color CEIL_L  = Color.web("#e8e0d0");
    private static final Color CEIL_D  = Color.web("#c8c0b0");
    private static final Color GOLD    = Color.web("#c8a040");
    private static final Color GOLD2   = Color.web("#a07820");
    private static final Color FRAME   = Color.web("#8b6828");
    private static final Color FRAME2  = Color.web("#5a3810");
    private static final Color ART_BG  = Color.web("#f8f4ee");
    private static final Color INK     = Color.web("#1a1008");
    private static final Color TBL_L   = Color.web("#d4b880");
    private static final Color TBL_D   = Color.web("#a88050");
    private static final Color TBL_LEG = Color.web("#8b6040");
    private static final Color CHR     = Color.web("#c4a870");
    private static final Color FREE    = Color.web("#22c55e");
    private static final Color OCC     = Color.web("#ef4444");
    private static final Color MINE    = Color.web("#3b82f6");
    private static final Color PLT_G   = Color.web("#5a8040");
    private static final Color PLT_G2  = Color.web("#3a5828");
    private static final Color PLT_POT = Color.web("#b07850");
    private static final Color SHELF_C = Color.web("#b89060");
    private static final Color DOOR_C  = Color.web("#8b6040");
    private static final Color DOOR_C2 = Color.web("#5a3820");
    private static final Color BG      = Color.web("#1a0e06");
    private static final Color VM_BODY = Color.web("#e8e0d8");
    private static final Color VM_ACC  = Color.web("#d04030");

    @FXML public void initialize() {
        loadImages();
        if (btnReserver != null) btnReserver.setDisable(true);
        if (btnLiberer  != null) btnLiberer.setDisable(true);
    }

    /** Charge les images PNG/JPG pour tables, étagères, vending machine */
    private void loadImages() {
        // Essayer SVG d'abord (si converti en PNG), sinon PNG direct
        imgTable   = loadImg("/views/frontoffice/3d/table.svg");
        if (imgTable == null) imgTable = loadImg("/views/frontoffice/3d/table.png");
        
        imgShelf   = loadImg("/views/frontoffice/3d/shelf.svg");
        if (imgShelf == null) imgShelf = loadImg("/views/frontoffice/3d/shelf.png");
        
        imgVending = loadImg("/views/frontoffice/3d/vending.svg");
        if (imgVending == null) imgVending = loadImg("/views/frontoffice/3d/vending.png");
    }

    private Image loadImg(String path) {
        try {
            var url = getClass().getResource(path);
            if (url != null) return new Image(url.toExternalForm(), true);
        } catch (Exception ignored) {}
        return null;
    }

    public void setData(Evenement ev, Equipe eq) {
        this.evenement = ev; this.equipe = eq;
        this.myEquipeId = (eq != null) ? eq.getId() : -1;
        if (labelEventName != null) labelEventName.setText(ev.getTitre() + "  —  " + ev.getLieu());
        try { reservations.putAll(participationService.getReservationsTable(ev.getId())); }
        catch (Exception e) { System.err.println("Réservations: " + e.getMessage()); }
        setupCanvas(); startRender(); updateButtons();
    }

    private void setupCanvas() {
        canvas3D.widthProperty().bind(scene3DContainer.widthProperty());
        canvas3D.heightProperty().bind(scene3DContainer.heightProperty());
        canvas3D.widthProperty().addListener(o -> redraw());
        canvas3D.heightProperty().addListener(o -> redraw());
        canvas3D.setOnMouseClicked(this::onClick);
        canvas3D.setFocusTraversable(true);
        canvas3D.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT)  { camPan = Math.max(-1, camPan - 0.18); e.consume(); }
            if (e.getCode() == KeyCode.RIGHT) { camPan = Math.min( 1, camPan + 0.18); e.consume(); }
        });
        canvas3D.requestFocus();
    }

    private void startRender() {
        timer = new AnimationTimer() { @Override public void handle(long n) { redraw(); } };
        timer.start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  REDRAW
    // ════════════════════════════════════════════════════════════════════════
    private void redraw() {
        double W = canvas3D.getWidth(), H = canvas3D.getHeight();
        if (W <= 0 || H <= 0) return;
        GraphicsContext g = canvas3D.getGraphicsContext2D();
        tableHits.clear(); doorHits.clear();
        g.setFill(BG); g.fillRect(0, 0, W, H);
        if (currentRoom == 0) drawCorridor(g, W, H);
        else                  drawSalle(g, W, H);
        drawMoveBtns(g, W, H);
        drawNavBar(g, W, H);
        drawHUD(g, W, H);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GEOMETRY — horizon très bas = sol très visible (74% de l'écran)
    //  camPan décale le POINT DE VUE (vanishing point) horizontalement
    //  → les éléments restent fixes, c'est la caméra qui tourne
    // ════════════════════════════════════════════════════════════════════════
    /** Returns {rL,rT,rR,rB, bwL,bwT,bwR,hz, vpX} */
    private double[] geo(double W, double H) {
        double rL = W * 0.01, rR = W * 0.99, rT = H * 0.02, rB = H * 0.88;
        double hz  = H * 0.26; // horizon à 26% → sol = 74%
        // Point de fuite (vanishing point) décalé par camPan
        double vpX = W / 2.0 + camPan * W * 0.28;
        // Mur du fond : centré sur le point de fuite
        double bwHalf = W * 0.22;
        double bwL = vpX - bwHalf;
        double bwR = vpX + bwHalf;
        double wallH = H * 0.22;
        double bwT = hz - wallH;
        return new double[]{rL, rT, rR, rB, bwL, bwT, bwR, hz, vpX};
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DRAW SALLE (rooms A/B/C)
    // ════════════════════════════════════════════════════════════════════════
    private void drawSalle(GraphicsContext g, double W, double H) {
        double[] d = geo(W, H);
        double rL=d[0],rT=d[1],rR=d[2],rB=d[3],bwL=d[4],bwT=d[5],bwR=d[6],hz=d[7],vpX=d[8];
        drawCeil(g, rL, rT, rR, bwL, bwT, bwR, hz, vpX);
        drawFloor(g, rL, rB, rR, bwL, hz, bwR);
        drawLeftWall(g, rL, rT, rB, bwL, bwT, hz);
        drawRightWall(g, rR, rT, rB, bwR, bwT, hz);
        drawBackWall(g, bwL, bwT, bwR, hz);
        drawMoldings(g, rL, rT, rR, rB, bwL, bwT, bwR, hz);
        drawChandelier(g, W, H, vpX, bwT + H * 0.04);
        drawBackArtworks(g, bwL, bwT, bwR, hz);
        drawSideArtworks(g, rL, rT, rB, bwL, bwT, hz, rR, bwR, vpX);
        drawShelves(g, W, H, rL, rT, rB, bwL, bwT, hz, rR, bwR, vpX);
        drawFloorPlants(g, W, H, rL, rR, rB, vpX);
        if (currentRoom == 2) drawCoffeeCorner(g, W, H, rL, rB, vpX);
        drawTablesSpacious(g, W, H, rL, rR, rB, hz, vpX);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DRAW CORRIDOR — exactement comme la photo de référence :
    //  Vue frontale du couloir, grandes portes marron sur les murs latéraux
    //  gauche ET droite, plafond clair avec spots, sol clair, mur du fond
    //  avec fenêtre. Portes occupent ~80% de la hauteur du mur latéral.
    // ════════════════════════════════════════════════════════════════════════
    private void drawCorridor(GraphicsContext g, double W, double H) {
        // Couloir : point de fuite FIXE au centre (pas affecté par camPan)
        // pour que les portes restent bien intégrées dans les murs
        double rL = W*0.01, rR = W*0.99, rT = H*0.02, rB = H*0.88;
        double hz  = H*0.30; // horizon couloir légèrement plus haut
        double vpX = W/2.0;  // point de fuite FIXE au centre pour le couloir
        double bwHalf = W*0.18;
        double bwL = vpX - bwHalf, bwR = vpX + bwHalf;
        double wallH = H*0.20;
        double bwT = hz - wallH;

        // Sol couloir — clair comme la photo
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web("#e8e4dc")), new Stop(1,Color.web("#c8c0b0"))));
        g.fillPolygon(new double[]{rL,rR,bwR,bwL}, new double[]{rB,rB,hz,hz}, 4);
        // Lignes sol
        g.setStroke(Color.web("#b0a898",0.3)); g.setLineWidth(0.8);
        for (int i=0;i<=12;i++){double t=(double)i/12;g.strokeLine(bwL+t*(bwR-bwL),hz,rL+t*(rR-rL),rB);}
        for (int j=1;j<=8;j++){double t=(double)j/9;double y=rB-t*(rB-hz);double lx=rL+t*(bwL-rL);double rx=rR-t*(rR-bwR);g.strokeLine(lx,y,rx,y);}

        // Plafond couloir — clair avec grille
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web("#f0ece4")), new Stop(1,Color.web("#e0dcd4"))));
        g.fillPolygon(new double[]{rL,rR,bwR,bwL}, new double[]{rT,rT,bwT,bwT}, 4);
        g.setStroke(Color.web("#c8c4bc",0.4)); g.setLineWidth(0.6);
        for (int i=0;i<=12;i++){double t=(double)i/12;g.strokeLine(rL+t*(rR-rL),rT,bwL+t*(bwR-bwL),bwT);}
        for (int j=0;j<=6;j++){double t=(double)j/6;double y=rT+t*(bwT-rT);double lx=rL+t*(bwL-rL);double rx=rR-t*(rR-bwR);g.strokeLine(lx,y,rx,y);}

        // Spots lumineux au plafond (comme la photo)
        double[] spotXs = {vpX-W*0.18, vpX, vpX+W*0.18};
        double[] spotYs = {rT+H*0.04, rT+H*0.06, rT+H*0.04};
        for (int i=0;i<3;i++){
            g.setFill(new RadialGradient(0,0,spotXs[i],spotYs[i],W*0.04,false,CycleMethod.NO_CYCLE,
                new Stop(0,Color.web("#ffffff",0.9)),new Stop(1,Color.TRANSPARENT)));
            g.fillOval(spotXs[i]-W*0.04,spotYs[i]-W*0.04,W*0.08,W*0.08);
            g.setFill(Color.web("#e8e0d0")); g.fillOval(spotXs[i]-W*0.018,spotYs[i]-W*0.012,W*0.036,W*0.024);
            g.setStroke(Color.web("#c0b8a8")); g.setLineWidth(1.5);
            g.strokeOval(spotXs[i]-W*0.018,spotYs[i]-W*0.012,W*0.036,W*0.024);
        }

        // Mur gauche
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web("#d8d4cc")), new Stop(1,Color.web("#e8e4dc"))));
        g.fillPolygon(new double[]{rL,bwL,bwL,rL}, new double[]{rT,bwT,hz,rB}, 4);
        // Mur droit
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web("#e8e4dc")), new Stop(1,Color.web("#d8d4cc"))));
        g.fillPolygon(new double[]{bwR,rR,rR,bwR}, new double[]{bwT,rT,rB,hz}, 4);
        // Mur du fond
        g.setFill(Color.web("#f0ece4"));
        g.fillRect(bwL, bwT, bwR-bwL, hz-bwT);

        // Plinthe couloir (gris-bleu comme la photo)
        double ph = (rB-hz)*0.06;
        g.setFill(Color.web("#8090a0"));
        g.fillPolygon(new double[]{rL,bwL,bwL,rL},   new double[]{rB-ph,hz,hz+ph*0.3,rB}, 4);
        g.fillPolygon(new double[]{bwR,rR,rR,bwR},   new double[]{hz,rB-ph,rB,hz+ph*0.3}, 4);
        g.fillRect(bwL, hz-ph*0.15, bwR-bwL, ph*0.15);

        // Fenêtre sur le mur du fond (comme la photo)
        double wW=bwR-bwL, wH=hz-bwT;
        double winW=wW*0.55, winH=wH*0.55;
        double winX=bwL+wW/2-winW/2, winY=bwT+wH*0.18;
        g.setFill(Color.web("#d0e8f8")); g.fillRect(winX,winY,winW,winH);
        g.setFill(Color.web("#e8f4ff",0.8)); g.fillRect(winX+2,winY+2,winW-4,winH-4);
        // Croisillons fenêtre
        g.setStroke(Color.web("#a0b0c0")); g.setLineWidth(2);
        g.strokeRect(winX,winY,winW,winH);
        g.strokeLine(winX+winW/3,winY,winX+winW/3,winY+winH);
        g.strokeLine(winX+winW*2/3,winY,winX+winW*2/3,winY+winH);
        g.strokeLine(winX,winY+winH/2,winX+winW,winY+winH/2);

        // Petits interrupteurs sur les murs (comme la photo)
        double swY = (rT+rB)/2;
        // Gauche
        double swLx = bwL-(bwL-rL)*0.35;
        g.setFill(Color.web("#c0b8b0")); g.fillRoundRect(swLx-W*0.012,swY-H*0.02,W*0.024,H*0.04,3,3);
        g.setStroke(Color.web("#a0988e")); g.setLineWidth(1); g.strokeRoundRect(swLx-W*0.012,swY-H*0.02,W*0.024,H*0.04,3,3);
        // Droit
        double swRx = bwR+(rR-bwR)*0.35;
        g.setFill(Color.web("#c0b8b0")); g.fillRoundRect(swRx-W*0.012,swY-H*0.02,W*0.024,H*0.04,3,3);
        g.setStroke(Color.web("#a0988e")); g.setLineWidth(1); g.strokeRoundRect(swRx-W*0.012,swY-H*0.02,W*0.024,H*0.04,3,3);

        // GRANDES PORTES sur les murs latéraux (comme la photo)
        drawCorridorDoorOnLeftWall(g, W, H, rL, rT, rB, bwL, bwT, hz, vpX);
        drawCorridorDoorOnRightWall(g, W, H, rR, rT, rB, bwR, bwT, hz, vpX);
        drawCorridorDoorBack(g, bwL, bwT, bwR, hz);

        // Vending machine
        drawVendingMachine(g, W, H, rL, rT, rB, bwL, hz, vpX);
        drawFloorPlants(g, W, H, rL, rR, rB, vpX);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CEILING — grille en perspective convergeant vers le point de fuite
    // ════════════════════════════════════════════════════════════════════════
    private void drawCeil(GraphicsContext g, double rL, double rT, double rR,
                           double bwL, double bwT, double bwR, double hz, double vpX) {
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,CEIL_D), new Stop(1,CEIL_L)));
        g.fillPolygon(new double[]{rL,rR,bwR,bwL}, new double[]{rT,rT,bwT,bwT}, 4);
        g.setStroke(Color.web("#a09880",0.35)); g.setLineWidth(0.7);
        // Lignes convergeant vers le point de fuite
        for (int i = 0; i <= 14; i++) {
            double t = (double)i / 14;
            double fx = rL + t * (rR - rL);
            double bx = bwL + t * (bwR - bwL);
            g.strokeLine(fx, rT, bx, bwT);
        }
        for (int j = 0; j <= 8; j++) {
            double t = (double)j / 8;
            double y = rT + t * (bwT - rT);
            double lx = rL + t * (bwL - rL);
            double rx = rR - t * (rR - bwR);
            g.strokeLine(lx, y, rx, y);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FLOOR — sol très visible (74% de l'écran)
    // ════════════════════════════════════════════════════════════════════════
    private void drawFloor(GraphicsContext g, double rL, double rB, double rR,
                            double bwL, double hz, double bwR) {
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,FLOOR_L), new Stop(1,FLOOR_D)));
        g.fillPolygon(new double[]{rL,rR,bwR,bwL}, new double[]{rB,rB,hz,hz}, 4);
        g.setStroke(Color.web("#7a5020",0.18)); g.setLineWidth(0.8);
        for (int i = 0; i <= 14; i++) {
            double t = (double)i / 14;
            g.strokeLine(bwL + t*(bwR-bwL), hz, rL + t*(rR-rL), rB);
        }
        for (int j = 1; j <= 10; j++) {
            double t = (double)j / 11;
            double y = rB - t * (rB - hz);
            double lx = rL + t * (bwL - rL);
            double rx = rR - t * (rR - bwR);
            g.strokeLine(lx, y, rx, y);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  WALLS
    // ════════════════════════════════════════════════════════════════════════
    private void drawLeftWall(GraphicsContext g, double rL, double rT, double rB,
                               double bwL, double bwT, double hz) {
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,WALL_D), new Stop(1,WALL)));
        g.fillPolygon(new double[]{rL,bwL,bwL,rL}, new double[]{rT,bwT,hz,rB}, 4);
    }

    private void drawRightWall(GraphicsContext g, double rR, double rT, double rB,
                                double bwR, double bwT, double hz) {
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,WALL), new Stop(1,WALL_D)));
        g.fillPolygon(new double[]{bwR,rR,rR,bwR}, new double[]{bwT,rT,rB,hz}, 4);
    }

    private void drawBackWall(GraphicsContext g, double bwL, double bwT, double bwR, double hz) {
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,WALL), new Stop(1,WALL_S)));
        g.fillRect(bwL, bwT, bwR - bwL, hz - bwT);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MOLDINGS (plinthe + corniche dorées)
    // ════════════════════════════════════════════════════════════════════════
    private void drawMoldings(GraphicsContext g, double rL, double rT, double rR, double rB,
                               double bwL, double bwT, double bwR, double hz) {
        LinearGradient gld = new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,GOLD), new Stop(1,GOLD2));
        g.setFill(gld);
        double bh = (rB - hz) * 0.055;
        g.fillPolygon(new double[]{rL,bwL,bwL,rL},   new double[]{rB-bh,hz,hz+bh*0.3,rB}, 4);
        g.fillPolygon(new double[]{bwR,rR,rR,bwR},   new double[]{hz,rB-bh,rB,hz+bh*0.3}, 4);
        g.fillRect(bwL, hz - bh*0.2, bwR - bwL, bh*0.2);
        double ch = (hz - bwT) * 0.11;
        g.fillPolygon(new double[]{rL,bwL,bwL,rL},   new double[]{rT,bwT,bwT+ch*0.3,rT+ch}, 4);
        g.fillPolygon(new double[]{bwR,rR,rR,bwR},   new double[]{bwT,rT,rT+ch,bwT+ch*0.3}, 4);
        g.fillRect(bwL, bwT, bwR - bwL, ch*0.3);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CHANDELIER
    // ════════════════════════════════════════════════════════════════════════
    private void drawChandelier(GraphicsContext g, double W, double H, double cx, double topY) {
        double bot = topY + H*0.1, rr = W*0.032;
        g.setFill(new RadialGradient(0,0,cx,bot,rr*3,false,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web("#ffd080",0.4)), new Stop(1,Color.TRANSPARENT)));
        g.fillOval(cx-rr*3, bot-rr*3, rr*6, rr*6);
        g.setStroke(GOLD); g.setLineWidth(2.5);
        g.strokeLine(cx, topY, cx, bot-rr);
        g.setStroke(GOLD); g.setLineWidth(3);
        g.strokeOval(cx-rr, bot-rr, rr*2, rr*2);
        g.setStroke(GOLD2); g.setLineWidth(1.5);
        g.strokeOval(cx-rr*0.5, bot-rr*0.5, rr, rr);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            double ex = cx + Math.cos(a)*rr*1.1, ey = bot + Math.sin(a)*rr*0.5;
            g.setStroke(GOLD2); g.setLineWidth(1.8);
            g.strokeLine(cx, bot, ex, ey);
            double br = W*0.005;
            g.setFill(new RadialGradient(0,0,ex,ey,br*3,false,CycleMethod.NO_CYCLE,
                new Stop(0,Color.web("#fff8c0",0.9)), new Stop(1,Color.TRANSPARENT)));
            g.fillOval(ex-br*3, ey-br*3, br*6, br*6);
            g.setFill(Color.web("#ffd060")); g.fillOval(ex-br, ey-br, br*2, br*2);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ARTWORKS — mur du fond
    // ════════════════════════════════════════════════════════════════════════
    private void drawBackArtworks(GraphicsContext g, double bwL, double bwT, double bwR, double hz) {
        double wW = bwR - bwL, wH = hz - bwT;
        String[][] arts = switch (currentRoom) {
            case 1 -> new String[][]{{"","woman"},{"","lines"},{"","flowerface"}};
            case 2 -> new String[][]{{"","profile"},{"","arcs"},{"","duo"}};
            case 3 -> new String[][]{{"","womanside"},{"","waves"},{"","flowerface"}};
            default -> new String[][]{{"","woman"},{"","lines"},{"","profile"}};
        };
        double aW = wW*0.22, aH = wH*0.72, aY = bwT + wH*0.08;
        double[] aXs = {bwL + wW*0.06, bwL + wW*0.39, bwL + wW*0.70};
        for (int i = 0; i < 3; i++) artFrame(g, aXs[i], aY, aW, aH, arts[i][0], arts[i][1]);
    }

    // Tableaux sur les murs latéraux — en perspective selon leur position
    private void drawSideArtworks(GraphicsContext g, double rL, double rT, double rB,
                                   double bwL, double bwT, double hz,
                                   double rR, double bwR, double vpX) {
        // Mur gauche : tableau en perspective (plus petit vers le fond)
        double lwW = (bwL - rL) * 0.42, lwH = (rB - rT) * 0.18;
        double lwX = (rL + bwL) / 2 - lwW / 2;
        double lwY = (rT + hz) / 2 - lwH / 2;
        // Appliquer une légère déformation perspective (trapézoïde)
        drawSidePerspectiveFrame(g, lwX, lwY, lwW, lwH, true, "arcs");
        // Mur droit
        double rwW = (rR - bwR) * 0.42, rwH = (rB - rT) * 0.18;
        double rwX = (rR + bwR) / 2 - rwW / 2;
        double rwY = (rT + hz) / 2 - rwH / 2;
        drawSidePerspectiveFrame(g, rwX, rwY, rwW, rwH, false, "waves");
    }

    /** Tableau en perspective sur un mur latéral (trapézoïde) */
    private void drawSidePerspectiveFrame(GraphicsContext g, double x, double y, double w, double h,
                                           boolean isLeft, String artType) {
        double skew = w * 0.12 * (isLeft ? 1 : -1);
        double[] xs = isLeft
            ? new double[]{x+skew, x+w+skew*0.3, x+w+skew*0.3, x+skew}
            : new double[]{x-skew*0.3, x+w-skew, x+w-skew, x-skew*0.3};
        double[] ys = {y, y, y+h, y+h};
        g.setFill(FRAME2); g.fillPolygon(
            new double[]{xs[0]-3,xs[1]+3,xs[2]+3,xs[3]-3},
            new double[]{ys[0]-3,ys[1]-3,ys[2]+3,ys[3]+3}, 4);
        g.setFill(FRAME); g.fillPolygon(
            new double[]{xs[0]-1,xs[1]+1,xs[2]+1,xs[3]-1},
            new double[]{ys[0]-1,ys[1]-1,ys[2]+1,ys[3]+1}, 4);
        g.setFill(ART_BG); g.fillPolygon(xs, ys, 4);
        // Art simplifié dans le trapézoïde
        double cx = (xs[0]+xs[1])/2, cy = y + h/2;
        g.setStroke(INK); g.setLineWidth(1.2);
        g.setLineCap(StrokeLineCap.ROUND);
        if ("arcs".equals(artType)) {
            for (int i = 1; i <= 4; i++) {
                double r = i * Math.min(w,h) * 0.07;
                g.strokeArc(cx-r, cy-r, r*2, r*2, 0, 180, ArcType.OPEN);
            }
        } else {
            for (int i = 0; i < 5; i++) {
                double yy = y + h*0.15 + i*h*0.14;
                g.beginPath(); g.moveTo(xs[0]+w*0.05, yy);
                for (int t = 0; t <= 16; t++) {
                    double tx = xs[0]+w*0.05 + t*(w*0.9/16);
                    double ty = yy + Math.sin(t*Math.PI/3)*h*0.035;
                    if (t==0) g.moveTo(tx,ty); else g.lineTo(tx,ty);
                }
                g.stroke();
            }
        }
    }

    private void artFrame(GraphicsContext g, double x, double y, double w, double h,
                           String label, String type) {
        g.setFill(Color.web("#000",0.15)); g.fillRect(x+3, y+3, w, h);
        g.setFill(FRAME2); g.fillRect(x-5, y-5, w+10, h+10);
        g.setFill(FRAME);  g.fillRect(x-3, y-3, w+6,  h+6);
        g.setFill(ART_BG); g.fillRect(x, y, w, h);
        lineArt(g, x+6, y+6, w-12, h-12, type);
        if (!label.isEmpty()) {
            g.setFill(FRAME); g.setFont(Font.font("Georgia", 9));
            g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.TOP);
            g.fillText(label, x+w/2, y+h+4);
        }
    }

    /** Dessins minimalistes en ligne noire */
    private void lineArt(GraphicsContext g, double x, double y, double w, double h, String type) {
        double cx = x+w/2, cy = y+h/2;
        g.setStroke(INK); g.setLineWidth(1.5);
        g.setLineCap(StrokeLineCap.ROUND); g.setLineJoin(StrokeLineJoin.ROUND);
        switch (type) {
            case "woman" -> {
                g.beginPath();
                g.moveTo(cx-w*0.05, cy-h*0.42);
                g.bezierCurveTo(cx+w*0.28,cy-h*0.42, cx+w*0.32,cy-h*0.1, cx+w*0.18,cy+h*0.05);
                g.bezierCurveTo(cx+w*0.28,cy+h*0.18, cx+w*0.22,cy+h*0.38, cx,cy+h*0.42);
                g.bezierCurveTo(cx-w*0.22,cy+h*0.38, cx-w*0.28,cy+h*0.18, cx-w*0.18,cy+h*0.05);
                g.bezierCurveTo(cx-w*0.32,cy-h*0.1, cx-w*0.28,cy-h*0.42, cx-w*0.05,cy-h*0.42);
                g.stroke();
                g.beginPath(); g.moveTo(cx-w*0.12,cy-h*0.08);
                g.bezierCurveTo(cx-w*0.06,cy-h*0.14, cx+w*0.06,cy-h*0.14, cx+w*0.12,cy-h*0.08); g.stroke();
                g.beginPath(); g.moveTo(cx,cy-h*0.04); g.lineTo(cx-w*0.06,cy+h*0.06); g.stroke();
                g.beginPath(); g.moveTo(cx-w*0.1,cy+h*0.14);
                g.quadraticCurveTo(cx,cy+h*0.22, cx+w*0.1,cy+h*0.14); g.stroke();
                g.beginPath(); g.moveTo(cx-w*0.05,cy-h*0.42);
                g.bezierCurveTo(cx-w*0.35,cy-h*0.35, cx-w*0.42,cy, cx-w*0.3,cy+h*0.3); g.stroke();
            }
            case "lines" -> {
                for (int i = 0; i < 5; i++) {
                    double off = i*h*0.12 - h*0.24;
                    g.beginPath(); g.moveTo(x+w*0.1, cy+off);
                    g.bezierCurveTo(cx-w*0.1,cy+off-h*0.15, cx+w*0.1,cy+off+h*0.15, x+w*0.9,cy+off);
                    g.stroke();
                }
            }
            case "flowerface" -> {
                g.beginPath();
                g.moveTo(cx+w*0.1,cy-h*0.42);
                g.bezierCurveTo(cx+w*0.3,cy-h*0.3, cx+w*0.28,cy-h*0.05, cx+w*0.15,cy+h*0.1);
                g.bezierCurveTo(cx+w*0.25,cy+h*0.25, cx+w*0.1,cy+h*0.42, cx-w*0.1,cy+h*0.42);
                g.stroke();
                for (int i = 0; i < 3; i++) {
                    double fx = cx-w*0.15+i*w*0.15, fy = cy-h*0.38+i*h*0.04;
                    for (int p = 0; p < 5; p++) {
                        double a = p*Math.PI*2/5;
                        g.strokeOval(fx+Math.cos(a)*w*0.06-w*0.04, fy+Math.sin(a)*h*0.05-h*0.04, w*0.08, h*0.07);
                    }
                    g.strokeOval(fx-w*0.03, fy-h*0.03, w*0.06, h*0.05);
                }
            }
            case "profile" -> {
                g.beginPath();
                g.moveTo(cx+w*0.05,cy-h*0.42);
                g.bezierCurveTo(cx+w*0.28,cy-h*0.38, cx+w*0.3,cy-h*0.2, cx+w*0.22,cy-h*0.05);
                g.bezierCurveTo(cx+w*0.32,cy+h*0.05, cx+w*0.28,cy+h*0.2, cx+w*0.1,cy+h*0.3);
                g.lineTo(cx-w*0.15, cy+h*0.42); g.stroke();
                g.strokeOval(cx+w*0.08, cy-h*0.18, w*0.1, h*0.07);
            }
            case "arcs" -> {
                for (int i = 1; i <= 5; i++) {
                    double r = i * Math.min(w,h) * 0.08;
                    g.strokeArc(cx-r, cy-r, r*2, r*2, 0, 180, ArcType.OPEN);
                }
            }
            case "duo" -> {
                g.beginPath();
                g.moveTo(cx-w*0.05,cy-h*0.38);
                g.bezierCurveTo(cx+w*0.2,cy-h*0.38, cx+w*0.22,cy+h*0.1, cx,cy+h*0.38);
                g.bezierCurveTo(cx-w*0.22,cy+h*0.1, cx-w*0.2,cy-h*0.38, cx-w*0.05,cy-h*0.38);
                g.stroke();
                g.beginPath();
                g.moveTo(cx+w*0.12,cy-h*0.28);
                g.bezierCurveTo(cx+w*0.38,cy-h*0.28, cx+w*0.4,cy+h*0.15, cx+w*0.18,cy+h*0.38);
                g.stroke();
            }
            case "womanside" -> {
                g.beginPath();
                g.moveTo(cx,cy-h*0.42);
                g.bezierCurveTo(cx+w*0.25,cy-h*0.35, cx+w*0.28,cy-h*0.1, cx+w*0.18,cy+h*0.05);
                g.bezierCurveTo(cx+w*0.28,cy+h*0.2, cx+w*0.2,cy+h*0.42, cx,cy+h*0.42);
                g.stroke();
                g.strokeOval(cx+w*0.06, cy-h*0.2, w*0.1, h*0.07);
                g.beginPath(); g.moveTo(cx,cy-h*0.42);
                g.bezierCurveTo(cx-w*0.3,cy-h*0.3, cx-w*0.35,cy+h*0.1, cx-w*0.2,cy+h*0.35); g.stroke();
            }
            case "waves" -> {
                for (int i = 0; i < 6; i++) {
                    double yy = y + h*0.12 + i*h*0.13;
                    g.beginPath();
                    for (int t = 0; t <= 20; t++) {
                        double tx = x+w*0.05 + t*(w*0.9/20);
                        double ty = yy + Math.sin(t*Math.PI/3.5)*h*0.04;
                        if (t==0) g.moveTo(tx,ty); else g.lineTo(tx,ty);
                    }
                    g.stroke();
                }
            }
            case "vendingSketch" -> {
                // Vending machine minimaliste en ligne noire
                double vx = x+w*0.2, vy = y+h*0.05, vw = w*0.6, vh = h*0.88;
                g.strokeRoundRect(vx, vy, vw, vh, 4, 4);
                // Vitre
                g.strokeRoundRect(vx+vw*0.1, vy+vh*0.08, vw*0.8, vh*0.42, 2, 2);
                // Grille bouteilles
                for (int r = 0; r < 3; r++) for (int c2 = 0; c2 < 2; c2++) {
                    double bx2 = vx+vw*0.18+c2*vw*0.38, by2 = vy+vh*0.12+r*vh*0.12;
                    g.strokeRoundRect(bx2, by2, vw*0.22, vh*0.09, 2, 2);
                }
                // Bande centrale
                g.strokeLine(vx, vy+vh*0.54, vx+vw, vy+vh*0.54);
                // Fente
                g.strokeRoundRect(vx+vw*0.3, vy+vh*0.62, vw*0.4, vh*0.055, 2, 2);
                // Bac
                g.strokeRect(vx+vw*0.1, vy+vh*0.87, vw*0.8, vh*0.04);
            }
            default -> g.strokeOval(cx-w*0.28, cy-h*0.32, w*0.56, h*0.52);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SHELVES — étagères avec livres, petite plante, petit cadre
    //  Position FIXE dans l'espace monde (ne bougent pas avec camPan)
    // ════════════════════════════════════════════════════════════════════════
    private void drawShelves(GraphicsContext g, double W, double H,
                              double rL, double rT, double rB,
                              double bwL, double bwT, double hz,
                              double rR, double bwR, double vpX) {
        // Positions FIXES — ne dépendent PAS de bwL/bwR (qui changent avec camPan)
        double sY  = hz + (rB - hz) * 0.32;
        double sWL = W * 0.13, sWR = W * 0.13;
        double fixedLx = W * 0.07;  // position fixe mur gauche
        double fixedRx = W * 0.80;  // position fixe mur droit
        drawShelf(g, fixedLx, sY, sWL, H*0.01, W, H);
        drawShelf(g, fixedRx, sY, sWR, H*0.01, W, H);
    }

    private void drawShelf(GraphicsContext g, double sx, double sy, double sw, double sh, double W, double H) {
        // Si image disponible, l'afficher
        if (imgShelf != null) {
            double iW = sw * 1.1, iH = H * 0.12;
            g.drawImage(imgShelf, sx - iW*0.05, sy - iH + sh, iW, iH);
            return;
        }
        // Fallback dessin
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,SHELF_C), new Stop(1,SHELF_C.darker())));
        g.fillRect(sx, sy, sw, sh);
        g.setFill(SHELF_C.brighter()); g.fillRect(sx, sy, sw, sh*0.3);
        // Livres
        double bx = sx + sw*0.04, bH = H*0.038, bW = sw*0.09;
        Color[] bc = {Color.web("#8b4040"),Color.web("#4060a0"),Color.web("#406040"),
                      Color.web("#a07030"),Color.web("#604080")};
        for (int i = 0; i < 5; i++) {
            double bh = bH * (0.8 + i%3*0.1);
            g.setFill(bc[i]); g.fillRect(bx, sy-bh, bW*0.88, bh);
            g.setFill(bc[i].brighter()); g.fillRect(bx, sy-bh, bW*0.15, bh);
            g.setStroke(bc[i].darker()); g.setLineWidth(0.5); g.strokeRect(bx, sy-bh, bW*0.88, bh);
            bx += bW;
        }
        // Petite plante
        double px = sx + sw*0.68, potW = sw*0.1, potH = H*0.026;
        g.setFill(PLT_POT);
        g.fillPolygon(new double[]{px-potW*0.5,px+potW*0.5,px+potW*0.4,px-potW*0.4},
                      new double[]{sy-potH,sy-potH,sy,sy}, 4);
        g.setFill(PLT_G);
        g.save(); g.translate(px, sy-potH); g.rotate(-22);
        g.fillOval(-potW*0.3,-potH*1.1,potW*0.6,potH*0.9); g.restore();
        g.save(); g.translate(px, sy-potH); g.rotate(22);
        g.fillOval(-potW*0.3,-potH*1.1,potW*0.6,potH*0.9); g.restore();
        // Petit cadre
        double fW = sw*0.12, fH = H*0.035, fX = sx+sw*0.83, fY = sy-fH;
        g.setFill(FRAME2); g.fillRect(fX-2, fY-2, fW+4, fH+4);
        g.setFill(ART_BG); g.fillRect(fX, fY, fW, fH);
        g.setStroke(INK); g.setLineWidth(1);
        g.strokeOval(fX+fW*0.28, fY+fH*0.18, fW*0.44, fH*0.52);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FLOOR PLANTS — plantes au sol dans les coins
    // ════════════════════════════════════════════════════════════════════════
    private void drawFloorPlants(GraphicsContext g, double W, double H,
                                  double rL, double rR, double rB, double vpX) {
        for (double px : new double[]{rL + W*0.05, rR - W*0.05}) {
            double py = rB, pW = W*0.028, pH = H*0.065;
            g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
                new Stop(0,PLT_POT.darker()), new Stop(1,PLT_POT)));
            g.fillPolygon(new double[]{px-pW*0.6,px+pW*0.6,px+pW*0.5,px-pW*0.5},
                          new double[]{py-pH,py-pH,py,py}, 4);
            g.setFill(PLT_POT.brighter()); g.fillRect(px-pW*0.6, py-pH, pW*1.2, pH*0.1);
            g.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
                new Stop(0,PLT_G), new Stop(1,PLT_G2)));
            g.save(); g.translate(px, py-pH); g.rotate(-30);
            g.fillOval(-pW*0.32,-pH*1.3,pW*0.72,pH*1.1); g.restore();
            g.save(); g.translate(px, py-pH); g.rotate(30);
            g.fillOval(-pW*0.35,-pH*1.3,pW*0.72,pH*1.1); g.restore();
            g.setFill(PLT_G2); g.fillOval(px-pW*0.2, py-pH-pH*1.45, pW*0.4, pH*1.25);
            g.setStroke(PLT_G2); g.setLineWidth(1.5);
            g.strokeLine(px, py-pH, px, py-pH-pH*0.5);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COFFEE CORNER (Salle B)
    // ════════════════════════════════════════════════════════════════════════
    private void drawCoffeeCorner(GraphicsContext g, double W, double H, double rL, double rB, double vpX) {
        double cx = rL + W*0.11, cy = rB - H*0.17;
        double cW = W*0.085, cH = H*0.13;
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,SHELF_C), new Stop(1,SHELF_C.darker())));
        g.fillRoundRect(cx-cW/2, cy, cW, cH*0.32, 6, 6);
        double cupX = cx-W*0.011, cupY = cy-H*0.038;
        double cupW = W*0.022, cupH = H*0.03;
        g.setFill(Color.web("#6b9060")); g.fillOval(cupX-cupW*0.1, cupY+cupH*0.8, cupW*1.2, cupH*0.22);
        g.setFill(Color.web("#7a9870")); g.fillRoundRect(cupX, cupY, cupW, cupH, 4, 4);
        g.setFill(Color.web("#3a2010")); g.fillOval(cupX+cupW*0.1, cupY+cupH*0.15, cupW*0.8, cupH*0.55);
        g.setStroke(Color.web("#ffffff",0.45)); g.setLineWidth(1.1);
        for (int i = 0; i < 2; i++) {
            double sx = cupX + cupW*0.3 + i*cupW*0.35;
            g.beginPath(); g.moveTo(sx, cupY);
            g.bezierCurveTo(sx-cupW*0.1,cupY-cupH*0.4, sx+cupW*0.1,cupY-cupH*0.7, sx,cupY-cupH);
            g.stroke();
        }
        g.setFill(GOLD); g.setFont(Font.font("Georgia", 8));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.TOP);
        g.fillText("☕ Café", cx, cy+cH*0.35);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VENDING MACHINE — couloir, sur le mur gauche
    //  Si image disponible → afficher l'image, sinon dessin vectoriel
    // ════════════════════════════════════════════════════════════════════════
    private void drawVendingMachine(GraphicsContext g, double W, double H,
                                     double rL, double rT, double rB,
                                     double bwL, double hz, double vpX) {
        double vmX = rL + W*0.07, vmY = rB - H*0.30;
        double vmW = W*0.06, vmH = H*0.26;
        if (imgVending != null) {
            g.drawImage(imgVending, vmX, vmY, vmW, vmH);
            return;
        }
        // Fallback dessin vectoriel 3D
        // Corps principal
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,VM_BODY.darker()), new Stop(0.5,VM_BODY), new Stop(1,VM_BODY.darker())));
        g.fillRoundRect(vmX, vmY, vmW, vmH, 8, 8);
        // Face latérale (effet 3D)
        g.setFill(VM_BODY.darker().darker());
        g.fillPolygon(new double[]{vmX+vmW, vmX+vmW+vmW*0.12, vmX+vmW+vmW*0.12, vmX+vmW},
                      new double[]{vmY, vmY+vmH*0.06, vmY+vmH+vmH*0.06, vmY+vmH}, 4);
        // Dessus (effet 3D)
        g.setFill(VM_BODY.brighter());
        g.fillPolygon(new double[]{vmX, vmX+vmW, vmX+vmW+vmW*0.12, vmX+vmW*0.12},
                      new double[]{vmY, vmY, vmY+vmH*0.06, vmY+vmH*0.06}, 4);
        // Vitre
        g.setFill(Color.web("#b0d8e8",0.7));
        g.fillRoundRect(vmX+vmW*0.1, vmY+vmH*0.08, vmW*0.8, vmH*0.44, 4, 4);
        // Bouteilles
        Color[] bc = {Color.web("#e06030"),Color.web("#30a060"),Color.web("#3060c0"),
                      Color.web("#c0a030"),Color.web("#a03060"),Color.web("#30a0a0")};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 2; col++) {
            double bx = vmX+vmW*0.18+col*vmW*0.35, by = vmY+vmH*0.12+row*vmH*0.12;
            g.setFill(bc[row*2+col]);
            g.fillRoundRect(bx, by, vmW*0.22, vmH*0.09, 3, 3);
        }
        g.setFill(VM_ACC); g.fillRect(vmX, vmY+vmH*0.54, vmW, vmH*0.04);
        g.setFill(Color.web("#888")); g.fillRoundRect(vmX+vmW*0.3, vmY+vmH*0.61, vmW*0.4, vmH*0.055, 3, 3);
        g.setFill(Color.web("#aaa")); g.fillRect(vmX+vmW*0.1, vmY+vmH*0.87, vmW*0.8, vmH*0.04);
        g.setFill(Color.web("#4a2c0a")); g.setFont(Font.font("Georgia", FontWeight.BOLD, 7));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.TOP);
        g.fillText("Boissons", vmX+vmW/2, vmY+vmH+3);
    }

    // Dessin minimaliste sur le mur du fond du couloir
    private void drawCorridorWallArt(GraphicsContext g, double bwL, double bwT, double bwR, double hz) {
        double wW = bwR - bwL, wH = hz - bwT;
        // Petit dessin vending machine minimaliste (ligne noire)
        double ax = bwL + wW*0.72, ay = bwT + wH*0.1, aw = wW*0.18, ah = wH*0.72;
        artFrame(g, ax, ay, aw, ah, "", "vendingSketch");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TABLES — position FIXE dans l'espace monde
    //  La perspective est calculée selon la position fixe de chaque table
    //  camPan décale le point de fuite → les tables restent à leur place
    // ════════════════════════════════════════════════════════════════════════
    private void drawTablesSpacious(GraphicsContext g, double W, double H,
                                     double rL, double rR, double rB, double hz, double vpX) {
        double floorH = rB - hz;
        // 6 tables réparties sur toute la surface du sol avec espacement optimal
        // Position fixe dans l'espace monde : {relX, relY, scale}
        // relX = 0..1 (gauche→droite), relY = 0..1 (fond→devant)
        double[][] positions = {
            {0.15, 0.25, 0.55}, {0.50, 0.25, 0.55}, {0.85, 0.25, 0.55},
            {0.15, 0.70, 1.05}, {0.50, 0.70, 1.05}, {0.85, 0.70, 1.05},
        };
        double baseW = W*0.15, baseH = H*0.045, baseLeg = H*0.050;
        for (int i = 0; i < positions.length; i++) {
            double relX = positions[i][0], relY = positions[i][1], sc = positions[i][2];
            // Projection perspective correcte
            double worldX = rL + relX * (rR - rL); // position monde fixe
            double worldY = hz + relY * floorH;     // position monde fixe
            // Décalage perspective basé sur la profondeur
            double perspShift = (worldX - vpX) * relY * 0.15;
            double tx = worldX + perspShift;
            double ty = worldY;
            double tw = baseW * sc, th = baseH * sc, tl = baseLeg * sc;
            int key = currentRoom * 100 + i;
            drawTable(g, tx - tw/2, ty - th, tw, th, tl, getStatus(key), i, key, sc);
        }
    }

    private void drawTable(GraphicsContext g, double x, double y, double w, double h, double lH,
                            String status, int num, int key, double sc) {
        // Si image disponible, l'afficher en perspective
        if (imgTable != null) {
            double iW = w * 1.6, iH = (h + lH) * 1.8;
            g.drawImage(imgTable, x - iW*0.1, y - iH*0.35, iW, iH);
            // Indicateur de statut par-dessus l'image
            Color dc = switch (status) { case "mine" -> MINE; case "occupied" -> OCC; default -> FREE; };
            double dr = Math.min(w,h) * 0.18;
            g.setFill(dc); g.fillOval(x+w/2-dr, y+h*0.1, dr*2, dr*2);
            g.setStroke(Color.WHITE); g.setLineWidth(1.5); g.strokeOval(x+w/2-dr, y+h*0.1, dr*2, dr*2);
            g.setFill(Color.WHITE); g.setFont(Font.font("Arial", FontWeight.BOLD, Math.max(9,h*0.5)));
            g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
            g.fillText(String.valueOf(num+1), x+w/2, y+h*0.5);
            tableHits.add(new double[]{x-iW*0.1, y-iH*0.35, iW, iH, currentRoom, num, key});
            return;
        }
        // Fallback dessin vectoriel 3D
        Color top  = switch (status) { case "mine" -> MINE; case "occupied" -> OCC; default -> TBL_L; };
        Color top2 = switch (status) { case "mine" -> MINE.darker(); case "occupied" -> OCC.darker(); default -> TBL_D; };
        // Ombre
        g.setFill(Color.web("#000",0.1));
        g.fillOval(x+w*0.08, y+h+lH-3, w*0.84, 5*sc);
        // Pieds
        double lw = w*0.032;
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,TBL_LEG.darker()), new Stop(1,TBL_LEG)));
        g.fillRect(x+w*0.08, y+h, lw, lH);
        g.fillRect(x+w*0.88-lw, y+h, lw, lH);
        g.setFill(TBL_LEG.darker());
        g.fillRect(x+w*0.2, y+h*0.3, lw*0.7, lH*0.5);
        g.fillRect(x+w*0.76-lw, y+h*0.3, lw*0.7, lH*0.5);
        // Face avant
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,top), new Stop(1,top2)));
        g.fillPolygon(new double[]{x,x+w,x+w-w*0.03,x+w*0.03},
                      new double[]{y+h*0.28,y+h*0.28,y+h,y+h}, 4);
        // Surface du dessus
        g.setFill(top.brighter());
        g.fillPolygon(new double[]{x+w*0.03,x+w-w*0.03,x+w,x},
                      new double[]{y+h*0.28,y+h*0.28,y,y}, 4);
        g.setStroke(top2.darker()); g.setLineWidth(0.8);
        g.strokePolygon(new double[]{x,x+w,x+w-w*0.03,x+w*0.03},
                        new double[]{y+h*0.28,y+h*0.28,y+h,y+h}, 4);
        // Chaises
        g.setFill(CHR);
        g.fillRoundRect(x+w*0.12, y+h+lH*0.85, w*0.14, h*0.42, 3, 3);
        g.fillRoundRect(x+w*0.72, y+h+lH*0.85, w*0.14, h*0.42, 3, 3);
        g.setFill(CHR.darker());
        g.fillRoundRect(x+w*0.16, y-h*0.35, w*0.12, h*0.28, 2, 2);
        g.fillRoundRect(x+w*0.68, y-h*0.35, w*0.12, h*0.28, 2, 2);
        // Indicateur statut
        Color dc = switch (status) { case "mine" -> MINE; case "occupied" -> OCC; default -> FREE; };
        double dr = Math.min(w,h) * 0.14;
        g.setFill(dc); g.fillOval(x+w/2-dr, y+h*0.05, dr*2, dr*2);
        g.setStroke(Color.WHITE); g.setLineWidth(1); g.strokeOval(x+w/2-dr, y+h*0.05, dr*2, dr*2);
        g.setFill(Color.WHITE); g.setFont(Font.font("Arial", FontWeight.BOLD, Math.max(9,h*0.5)));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
        g.fillText(String.valueOf(num+1), x+w/2, y+h*0.62);
        tableHits.add(new double[]{x, y, w, h+lH, currentRoom, num, key});
    }

    private String getStatus(int key) {
        if (!reservations.containsKey(key)) return "free";
        return reservations.get(key) == myEquipeId ? "mine" : "occupied";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CORRIDOR DOORS — grandes portes marron intégrées dans les murs latéraux
    //  Exactement comme la photo : porte occupe ~80% de la hauteur du mur,
    //  cadre épais, poignée ronde, perspective trapézoïde réelle
    // ════════════════════════════════════════════════════════════════════════
    private void drawCorridorDoorOnLeftWall(GraphicsContext g, double W, double H,
                                             double rL, double rT, double rB,
                                             double bwL, double bwT, double hz, double vpX) {
        // Le mur gauche est le trapézoïde : coins {rL,rT},{bwL,bwT},{bwL,hz},{rL,rB}
        // La porte est GRANDE : occupe 80% de la hauteur du mur, centrée verticalement
        // Position sur le mur : à 35% du fond (t=0.35)
        double t = 0.35;
        // Interpolation perspective : position sur le mur gauche
        double wallTopY  = rT  + t*(bwT-rT);
        double wallBotY  = rB  + t*(hz-rB);
        double wallX     = rL  + t*(bwL-rL);
        double wallH_px  = wallBotY - wallTopY;
        // Porte : 80% de la hauteur du mur, centrée
        double doorH = wallH_px * 0.80;
        double doorY = wallTopY + wallH_px * 0.10;
        // Largeur de la porte en perspective (plus étroite vers le fond)
        double doorW = (bwL-rL) * 0.28 * (0.3 + t*0.7);
        double doorX = wallX - doorW * 0.5;
        // Cadre extérieur (marron foncé)
        double frameThick = doorW * 0.08;
        g.setFill(Color.web("#5a3010"));
        g.fillRect(doorX - frameThick, doorY - frameThick,
                   doorW + frameThick*2, doorH + frameThick*2);
        // Porte principale (marron moyen comme la photo)
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web("#7a4820")), new Stop(0.4,Color.web("#a06030")),
            new Stop(1,Color.web("#7a4820"))));
        g.fillRect(doorX, doorY, doorW, doorH);
        // Panneau décoratif haut
        double panW = doorW*0.75, panH = doorH*0.35;
        double panX = doorX + doorW*0.125, panY = doorY + doorH*0.06;
        g.setFill(Color.web("#8b5228")); g.fillRect(panX, panY, panW, panH);
        g.setStroke(Color.web("#5a3010")); g.setLineWidth(1.5);
        g.strokeRect(panX, panY, panW, panH);
        // Panneau décoratif bas
        double panY2 = doorY + doorH*0.52;
        double panH2 = doorH*0.38;
        g.setFill(Color.web("#8b5228")); g.fillRect(panX, panY2, panW, panH2);
        g.setStroke(Color.web("#5a3010")); g.setLineWidth(1.5);
        g.strokeRect(panX, panY2, panW, panH2);
        // Poignée ronde (comme la photo)
        double knobX = doorX + doorW*0.72, knobY = doorY + doorH*0.50;
        double knobR = doorW*0.07;
        g.setFill(Color.web("#909090"));
        g.fillOval(knobX-knobR, knobY-knobR, knobR*2, knobR*2);
        g.setStroke(Color.web("#606060")); g.setLineWidth(1.5);
        g.strokeOval(knobX-knobR, knobY-knobR, knobR*2, knobR*2);
        // Petite plaque de serrure
        g.setFill(Color.web("#808080"));
        g.fillRoundRect(knobX-knobR*0.4, knobY+knobR*1.2, knobR*0.8, knobR*1.5, 2, 2);
        // Label
        g.setFill(Color.web("#f5e6c8")); g.setFont(Font.font("Georgia", FontWeight.BOLD, Math.max(8, doorW*0.16)));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.BOTTOM);
        g.fillText("Salle A", doorX+doorW/2, doorY - frameThick - 3);
        doorHits.add(new double[]{doorX-frameThick, doorY-frameThick,
                                   doorW+frameThick*2, doorH+frameThick*2, 1});
    }

    private void drawCorridorDoorOnRightWall(GraphicsContext g, double W, double H,
                                              double rR, double rT, double rB,
                                              double bwR, double bwT, double hz, double vpX) {
        double t = 0.35;
        double wallTopY = rT  + t*(bwT-rT);
        double wallBotY = rB  + t*(hz-rB);
        double wallX    = rR  - t*(rR-bwR);
        double wallH_px = wallBotY - wallTopY;
        double doorH = wallH_px * 0.80;
        double doorY = wallTopY + wallH_px * 0.10;
        double doorW = (rR-bwR) * 0.28 * (0.3 + t*0.7);
        double doorX = wallX - doorW * 0.5;
        double frameThick = doorW * 0.08;
        // Cadre
        g.setFill(Color.web("#5a3010"));
        g.fillRect(doorX-frameThick, doorY-frameThick, doorW+frameThick*2, doorH+frameThick*2);
        // Porte
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web("#7a4820")), new Stop(0.6,Color.web("#a06030")),
            new Stop(1,Color.web("#7a4820"))));
        g.fillRect(doorX, doorY, doorW, doorH);
        // Panneaux
        double panW = doorW*0.75, panX = doorX+doorW*0.125;
        g.setFill(Color.web("#8b5228")); g.fillRect(panX, doorY+doorH*0.06, panW, doorH*0.35);
        g.setStroke(Color.web("#5a3010")); g.setLineWidth(1.5);
        g.strokeRect(panX, doorY+doorH*0.06, panW, doorH*0.35);
        g.setFill(Color.web("#8b5228")); g.fillRect(panX, doorY+doorH*0.52, panW, doorH*0.38);
        g.strokeRect(panX, doorY+doorH*0.52, panW, doorH*0.38);
        // Poignée
        double knobX = doorX+doorW*0.28, knobY = doorY+doorH*0.50, knobR = doorW*0.07;
        g.setFill(Color.web("#909090")); g.fillOval(knobX-knobR,knobY-knobR,knobR*2,knobR*2);
        g.setStroke(Color.web("#606060")); g.setLineWidth(1.5);
        g.strokeOval(knobX-knobR,knobY-knobR,knobR*2,knobR*2);
        g.setFill(Color.web("#808080"));
        g.fillRoundRect(knobX-knobR*0.4,knobY+knobR*1.2,knobR*0.8,knobR*1.5,2,2);
        // Label
        g.setFill(Color.web("#f5e6c8")); g.setFont(Font.font("Georgia",FontWeight.BOLD,Math.max(8,doorW*0.16)));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.BOTTOM);
        g.fillText("Salle B", doorX+doorW/2, doorY-frameThick-3);
        doorHits.add(new double[]{doorX-frameThick,doorY-frameThick,doorW+frameThick*2,doorH+frameThick*2,2});
    }

    private void drawCorridorDoorBack(GraphicsContext g, double bwL, double bwT, double bwR, double hz) {
        double wW = bwR - bwL, wH = hz - bwT;
        double dW = wW*0.28, dH = wH*0.75;
        double dX = bwL + wW/2 - dW/2, dY = bwT + wH*0.16;
        g.setFill(FRAME2); g.fillRect(dX-4, dY-4, dW+8, dH+8);
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,DOOR_C2), new Stop(0.5,DOOR_C), new Stop(1,DOOR_C2)));
        g.fillRect(dX, dY, dW, dH);
        g.fillArc(dX, dY-dW*0.5, dW, dW, 0, 180, ArcType.CHORD);
        g.setFill(GOLD); g.fillOval(dX+dW*0.72, dY+dH*0.45, dW*0.1, dW*0.1);
        g.setFill(Color.web("#f5e6c8")); g.setFont(Font.font("Georgia", FontWeight.BOLD, Math.max(8, dW*0.18)));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.BOTTOM);
        g.fillText("Salle C", dX+dW/2, dY-dW*0.55);
        doorHits.add(new double[]{dX, dY-dW*0.5, dW, dH+dW*0.5, 3});
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MOVE BUTTONS — déplacer la caméra (pas les éléments)
    // ════════════════════════════════════════════════════════════════════════
    private void drawMoveBtns(GraphicsContext g, double W, double H) {
        double s = H*0.05, bx = W*0.01, by = H*0.43;
        btnL = moveBtn(g, bx,        by+s*1.1, s, "◀", camPan > -0.9);
        btnR = moveBtn(g, bx+s*1.1,  by+s*1.1, s, "▶", camPan <  0.9);
        btnC = moveBtn(g, bx+s*0.55, by,       s, "⌂", true);
        g.setFill(Color.web("#c9a96e",0.6)); g.setFont(Font.font("Georgia", 7));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.TOP);
        g.fillText("Vue", bx+s*0.55+s/2, by+s*2.3);
    }

    private double[] moveBtn(GraphicsContext g, double x, double y, double s, String lbl, boolean on) {
        Color bg = on ? Color.web("#f5e6c8",0.93) : Color.web("#8b6614",0.3);
        Color bd = on ? GOLD : Color.web("#8b6614",0.4);
        Color fg = on ? Color.web("#4a2c0a") : Color.web("#8b6614",0.5);
        g.setFill(Color.web("#000",0.14)); g.fillRoundRect(x+2, y+2, s, s, 8, 8);
        g.setFill(bg); g.fillRoundRect(x, y, s, s, 8, 8);
        g.setStroke(bd); g.setLineWidth(1.8); g.strokeRoundRect(x, y, s, s, 8, 8);
        g.setFill(fg); g.setFont(Font.font("Georgia", FontWeight.BOLD, s*0.38));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
        g.fillText(lbl, x+s/2, y+s/2);
        return new double[]{x, y, s, s};
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NAV BAR
    // ════════════════════════════════════════════════════════════════════════
    private void drawNavBar(GraphicsContext g, double W, double H) {
        double bW = W*0.14, bH = H*0.056, bY = H*0.916;
        double pX = W/2 - bW - W*0.05, nX = W/2 + W*0.05;
        navBtn(g, pX, bY, bW, bH, "← PRÉCÉDENTE", currentRoom > 0);
        navBtn(g, nX, bY, bW, bH, "SUIVANTE →",   currentRoom < ROOM_NAMES.length-1);
        g.setFill(Color.web("#f5e6c8",0.95));
        g.setFont(Font.font("Georgia", FontWeight.BOLD, H*0.018));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
        g.fillText((currentRoom+1) + " / " + ROOM_NAMES.length, W/2, bY+bH/2);
        g.setFill(Color.web("#d4a96a"));
        g.setFont(Font.font("Georgia", FontWeight.BOLD, H*0.021));
        g.fillText(ROOM_NAMES[currentRoom], W/2, bY - bH*0.62);
        double ds = W*0.015, dx0 = W/2 - (ROOM_NAMES.length-1)*ds/2;
        for (int i = 0; i < ROOM_NAMES.length; i++) {
            double dx = dx0+i*ds, dy = bY+bH+H*0.016;
            g.setFill(i == currentRoom ? GOLD : Color.web("#8b6614",0.5));
            g.fillOval(dx-5, dy-5, 10, 10);
            if (i == currentRoom) { g.setStroke(GOLD.brighter()); g.setLineWidth(1.5); g.strokeOval(dx-6,dy-6,12,12); }
        }
    }

    private void navBtn(GraphicsContext g, double x, double y, double w, double h, String t, boolean on) {
        Color bg = on ? Color.web("#f5e6c8",0.97) : Color.web("#8b6614",0.3);
        Color bd = on ? GOLD : Color.web("#8b6614",0.4);
        Color fg = on ? Color.web("#4a2c0a") : Color.web("#8b6614",0.5);
        g.setFill(Color.web("#000",0.16)); g.fillRoundRect(x+2, y+2, w, h, 10, 10);
        g.setFill(bg); g.fillRoundRect(x, y, w, h, 10, 10);
        g.setStroke(bd); g.setLineWidth(2); g.strokeRoundRect(x, y, w, h, 10, 10);
        g.setFill(fg); g.setFont(Font.font("Georgia", FontWeight.BOLD, h*0.37));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
        g.fillText(t, x+w/2, y+h/2);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HUD
    // ════════════════════════════════════════════════════════════════════════
    private void drawHUD(GraphicsContext g, double W, double H) {
        double bx = W*0.01, by = H*0.01, bw = W*0.24, bh = H*0.068;
        g.setFill(Color.web("#f5e6c8",0.93)); g.fillRoundRect(bx, by, bw, bh, 10, 10);
        g.setStroke(GOLD); g.setLineWidth(1.5); g.strokeRoundRect(bx, by, bw, bh, 10, 10);
        g.setFill(Color.web("#4a2c0a"));
        g.setFont(Font.font("Georgia", FontWeight.BOLD, H*0.02));
        g.setTextAlign(TextAlignment.LEFT); g.setTextBaseline(VPos.TOP);
        g.fillText("🏛 Espace 3D — Événements", bx+10, by+6);
        g.setFont(Font.font("Georgia", H*0.015)); g.setFill(Color.web("#8b6614"));
        g.fillText(ROOM_NAMES[currentRoom], bx+10, by+bh*0.52);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CLICK HANDLER
    // ════════════════════════════════════════════════════════════════════════
    private void onClick(MouseEvent e) {
        double mx = e.getX(), my = e.getY(), W = canvas3D.getWidth(), H = canvas3D.getHeight();
        // Boutons de déplacement caméra
        if (btnL != null && hit(mx,my,btnL)) { camPan = Math.max(-1, camPan-0.22); return; }
        if (btnR != null && hit(mx,my,btnR)) { camPan = Math.min( 1, camPan+0.22); return; }
        if (btnC != null && hit(mx,my,btnC)) { camPan = 0; return; } // reset vue centrale
        // Boutons nav
        double bW = W*0.14, bH = H*0.056, bY = H*0.916;
        double pX = W/2-bW-W*0.05, nX = W/2+W*0.05;
        if (mx>=pX && mx<=pX+bW && my>=bY && my<=bY+bH) { prevRoom(); return; }
        if (mx>=nX && mx<=nX+bW && my>=bY && my<=bY+bH) { nextRoom(); return; }
        // Portes
        for (double[] d : doorHits)
            if (mx>=d[0] && mx<=d[0]+d[2] && my>=d[1] && my<=d[1]+d[3]) { goRoom((int)d[4]); return; }
        // Tables
        for (double[] t : tableHits)
            if (mx>=t[0] && mx<=t[0]+t[2] && my>=t[1] && my<=t[1]+t[3]) {
                int si=(int)t[4], tn=(int)t[5], key=(int)t[6];
                if ("occupied".equals(getStatus(key))) { setStatus("Table "+(tn+1)+" — occupée","#ef4444"); return; }
                pendingSalleIdx = si; pendingTableNum = tn;
                setStatus("Table "+(tn+1)+" sélectionnée — cliquez Réserver","#22c55e");
                if (btnReserver != null) btnReserver.setDisable(equipe == null);
                return;
            }
        canvas3D.requestFocus();
    }

    private boolean hit(double mx, double my, double[] b) {
        return mx>=b[0] && mx<=b[0]+b[2] && my>=b[1] && my<=b[1]+b[3];
    }

    private void goRoom(int i)  { currentRoom=i; camPan=0; pendingSalleIdx=-1; redraw(); }
    private void nextRoom()     { if (currentRoom < ROOM_NAMES.length-1) goRoom(currentRoom+1); }
    private void prevRoom()     { if (currentRoom > 0) goRoom(currentRoom-1); }

    private void setStatus(String m, String c) {
        if (labelStatus != null) {
            labelStatus.setText(m);
            labelStatus.setStyle("-fx-font-size:12;-fx-font-weight:700;-fx-text-fill:"+c+";");
        }
    }

    @FXML private void onGoCorridor() { goRoom(0); }
    @FXML private void onGoSalleA()   { goRoom(1); }
    @FXML private void onGoSalleB()   { goRoom(2); }
    @FXML private void onGoSalleC()   { goRoom(3); }

    @FXML private void onReserver() {
        if (pendingSalleIdx < 0 || equipe == null) return;
        int key = pendingSalleIdx * 100 + pendingTableNum;
        if (participationService.reserverTable(evenement.getId(), equipe.getId(), key)) {
            reservations.put(key, equipe.getId());
            setStatus("Table "+(pendingTableNum+1)+" réservée !","#3b82f6");
            if (btnReserver != null) btnReserver.setDisable(true);
            if (btnLiberer  != null) btnLiberer.setDisable(false);
            pendingSalleIdx = -1;
            showReservationConfirmation();
            redraw();
        } else setStatus("Table déjà prise !","#ef4444");
    }

    private void showReservationConfirmation() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Confirmation de Réservation");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(300);
        
        dialog.getDialogPane().setStyle(
            "-fx-background-color: #2C1A0E; " +
            "-fx-text-fill: #F5E6C8; " +
            "-fx-font-family: 'Arial'; " +
            "-fx-font-size: 12;"
        );
        
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20; -fx-background-color: #2C1A0E;");
        
        Label titleLabel = new Label("✓ Réservation Confirmée");
        titleLabel.setStyle(
            "-fx-font-size: 18; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #22c55e;"
        );
        
        VBox detailsBox = new VBox(10);
        detailsBox.setStyle(
            "-fx-background-color: rgba(139,102,20,0.25); " +
            "-fx-border-color: #8B6614; " +
            "-fx-border-radius: 10; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 15;"
        );
        
        HBox nameBox = new HBox(10);
        nameBox.setStyle("-fx-alignment: CENTER_LEFT;");
        Label nameLabel = new Label("👤 Nom :");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #D4A96A; -fx-min-width: 100;");
        Label nameValue = new Label(equipe != null ? equipe.getNom() : "N/A");
        nameValue.setStyle("-fx-text-fill: #F5E6C8; -fx-font-size: 13;");
        nameBox.getChildren().addAll(nameLabel, nameValue);
        
        HBox roomBox = new HBox(10);
        roomBox.setStyle("-fx-alignment: CENTER_LEFT;");
        Label roomLabel = new Label("🏛 Salle :");
        roomLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #D4A96A; -fx-min-width: 100;");
        Label roomValue = new Label(ROOM_NAMES[pendingSalleIdx]);
        roomValue.setStyle("-fx-text-fill: #F5E6C8; -fx-font-size: 13;");
        roomBox.getChildren().addAll(roomLabel, roomValue);
        
        HBox tableBox = new HBox(10);
        tableBox.setStyle("-fx-alignment: CENTER_LEFT;");
        Label tableLabel = new Label("🪑 Table :");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #D4A96A; -fx-min-width: 100;");
        Label tableValue = new Label("Table " + (pendingTableNum + 1));
        tableValue.setStyle("-fx-text-fill: #F5E6C8; -fx-font-size: 13;");
        tableBox.getChildren().addAll(tableLabel, tableValue);
        
        HBox eventBox = new HBox(10);
        eventBox.setStyle("-fx-alignment: CENTER_LEFT;");
        Label eventLabel = new Label("📅 Événement :");
        eventLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #D4A96A; -fx-min-width: 100;");
        Label eventValue = new Label(evenement != null ? evenement.getTitre() : "N/A");
        eventValue.setStyle("-fx-text-fill: #F5E6C8; -fx-font-size: 13;");
        eventBox.getChildren().addAll(eventLabel, eventValue);
        
        detailsBox.getChildren().addAll(nameBox, roomBox, tableBox, eventBox);
        
        Label confirmLabel = new Label("Votre réservation a été enregistrée avec succès.");
        confirmLabel.setStyle(
            "-fx-text-fill: #D4A96A; " +
            "-fx-font-style: italic; " +
            "-fx-font-size: 11;"
        );
        confirmLabel.setWrapText(true);
        
        content.getChildren().addAll(titleLabel, detailsBox, confirmLabel);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle(
            "-fx-background-color: #8B6614; " +
            "-fx-text-fill: #F5E6C8; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 30; " +
            "-fx-font-size: 12;"
        );
        
        dialog.showAndWait();
    }

    @FXML private void onLiberer() {
        if (equipe == null) return;
        participationService.libererTable(evenement.getId(), equipe.getId());
        reservations.entrySet().removeIf(en -> en.getValue() == myEquipeId);
        setStatus("Réservation libérée.","#888888");
        if (btnLiberer  != null) btnLiberer.setDisable(true);
        if (btnReserver != null) btnReserver.setDisable(true);
        pendingSalleIdx = -1; redraw();
    }

    private void updateButtons() {
        if (equipe == null) {
            if (btnReserver != null) btnReserver.setDisable(true);
            if (btnLiberer  != null) btnLiberer.setDisable(true);
            return;
        }
        int t = participationService.getTableByEquipe(evenement.getId(), equipe.getId());
        if (btnLiberer != null) btnLiberer.setDisable(t < 0);
        if (t >= 0) {
            int s = t/100, tn = t%100;
            String[] n = {"Entrée","Salle A","Salle B","Salle C"};
            setStatus("Réservé : Table "+(tn+1)+" — "+(s<n.length?n[s]:"Salle "+s),"#3b82f6");
        }
    }

    @FXML private void onRetour()            { if(timer!=null)timer.stop(); FrontNavHelper.goMesParticipations(null); }
    @FXML private void onHome()              { if(timer!=null)timer.stop(); FrontNavHelper.goHome(); }
    @FXML private void onProfile()           { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout()            { if(timer!=null)timer.stop(); FrontNavHelper.goLogout(); }
}
