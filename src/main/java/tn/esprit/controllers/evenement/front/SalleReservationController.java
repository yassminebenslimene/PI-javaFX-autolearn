package tn.esprit.controllers.evenement.front;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
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
 * Caméra : position (camX, camY, camZ) + direction (camAngle)
 * Éléments : tables, étagères, portes ont des coordonnées fixes dans l'espace monde
 * Projection : perspective 3D réelle selon la position/direction de la caméra
 * Déplacement : flèches déplacent la caméra, pas les éléments
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

    // Vraie caméra 3D
    private double camX = 0, camY = 0, camZ = 1.6; // position caméra (hauteur yeux)
    private double camAngle = 0; // direction (radians)

    // Selection
    private int pendingSalleIdx = -1, pendingTableNum = -1;

    private final Map<Integer,Integer> reservations = new HashMap<>();
    private int myEquipeId = -1;

    private final List<double[]> tableHits = new ArrayList<>(); // {x,y,w,h,salleIdx,tableNum,key}
    private final List<double[]> doorHits  = new ArrayList<>(); // {x,y,w,h,target}
    private double[] btnL, btnR, btnZI, btnZO; // move button bounds

    private AnimationTimer timer;

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color WALL      = Color.web("#f0ebe0");
    private static final Color WALL_S    = Color.web("#ddd5c5");
    private static final Color WALL_D    = Color.web("#c8c0b0");
    private static final Color FLOOR_L   = Color.web("#d4b888");
    private static final Color FLOOR_D   = Color.web("#a07840");
    private static final Color CEIL_L    = Color.web("#e8e0d0");
    private static final Color CEIL_D    = Color.web("#c8c0b0");
    private static final Color GOLD      = Color.web("#c8a040");
    private static final Color GOLD2     = Color.web("#a07820");
    private static final Color FRAME     = Color.web("#8b6828");
    private static final Color FRAME2    = Color.web("#5a3810");
    private static final Color ART_BG    = Color.web("#f8f4ee");
    private static final Color INK       = Color.web("#1a1008");
    private static final Color TBL_L     = Color.web("#d4b880");
    private static final Color TBL_D     = Color.web("#a88050");
    private static final Color TBL_LEG   = Color.web("#8b6040");
    private static final Color CHR       = Color.web("#c4a870");
    private static final Color FREE      = Color.web("#22c55e");
    private static final Color OCC       = Color.web("#ef4444");
    private static final Color MINE      = Color.web("#3b82f6");
    private static final Color PLT_G     = Color.web("#5a8040");
    private static final Color PLT_G2    = Color.web("#3a5828");
    private static final Color PLT_POT   = Color.web("#b07850");
    private static final Color ROPE      = Color.web("#c8a050");
    private static final Color POST      = Color.web("#b89040");
    private static final Color SHELF_C   = Color.web("#b89060");
    private static final Color DOOR_C    = Color.web("#8b6040");
    private static final Color DOOR_C2   = Color.web("#5a3820");
    private static final Color BG        = Color.web("#1a0e06");
    private static final Color VM_BODY   = Color.web("#e8e0d8");
    private static final Color VM_ACCENT = Color.web("#d04030");
    private static final Color COFFEE_C  = Color.web("#6b4020");

    @FXML public void initialize() {
        if (btnReserver!=null) btnReserver.setDisable(true);
        if (btnLiberer !=null) btnLiberer.setDisable(true);
    }

    public void setData(Evenement ev, Equipe eq) {
        this.evenement=ev; this.equipe=eq;
        this.myEquipeId=(eq!=null)?eq.getId():-1;
        if (labelEventName!=null) labelEventName.setText(ev.getTitre()+"  —  "+ev.getLieu());
        try { reservations.putAll(participationService.getReservationsTable(ev.getId())); }
        catch(Exception e){ System.err.println("Réservations: "+e.getMessage()); }
        setupCanvas(); startRender(); updateButtons();
    }

    private void setupCanvas() {
        canvas3D.widthProperty().bind(scene3DContainer.widthProperty());
        canvas3D.heightProperty().bind(scene3DContainer.heightProperty());
        canvas3D.widthProperty().addListener(o->redraw());
        canvas3D.heightProperty().addListener(o->redraw());
        canvas3D.setOnMouseClicked(this::onClick);
        canvas3D.setFocusTraversable(true);
        canvas3D.setOnKeyPressed(e->{
            switch(e.getCode()){
                case LEFT  -> { camPan=Math.max(-1,camPan-0.18); e.consume(); }
                case RIGHT -> { camPan=Math.min( 1,camPan+0.18); e.consume(); }
                default -> {}
            }
        });
        canvas3D.requestFocus();
    }

    private void startRender() {
        timer=new AnimationTimer(){ @Override public void handle(long n){ redraw(); } };
        timer.start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  REDRAW
    // ════════════════════════════════════════════════════════════════════════
    private void redraw() {
        double W=canvas3D.getWidth(), H=canvas3D.getHeight();
        if(W<=0||H<=0) return;
        GraphicsContext g=canvas3D.getGraphicsContext2D();
        tableHits.clear(); doorHits.clear();
        g.setFill(BG); g.fillRect(0,0,W,H);
        if(currentRoom==0) drawCorridor(g,W,H);
        else               drawSalle(g,W,H);
        drawMoveBtns(g,W,H);
        drawNavBar(g,W,H);
        drawHUD(g,W,H);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GEOMETRY — horizon very low so floor dominates
    //  horizon = 26% from top  →  floor = 74% of screen height
    // ════════════════════════════════════════════════════════════════════════
    /** Returns {rL,rT,rR,rB, bwL,bwT,bwR,bwB} */
    private double[] geo(double W, double H) {
        double rL=W*0.01, rR=W*0.99, rT=H*0.02, rB=H*0.88;
        // horizon line at 26% — back wall top/bottom
        double hz = H*0.26;
        // back wall width = 55% of screen, centered + pan shift
        double bwHalf = W*0.275 + camPan*W*0.0;
        double panShift = camPan * W * 0.14;
        double bwL = W/2 - bwHalf + panShift;
        double bwR = W/2 + bwHalf + panShift;
        double bwT = hz - H*0.01;
        double bwB = hz + H*0.01; // back wall is a thin strip at horizon
        // For geometry we need back-wall bottom = where floor meets back wall
        // Use a proper perspective: back wall spans from bwT to bwT+wallH
        double wallH = H*0.22; // wall height above horizon
        bwT = hz - wallH;
        bwB = hz + H*0.005; // floor starts just below horizon
        return new double[]{rL,rT,rR,rB, bwL,bwT,bwR,hz};
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DRAW SALLE (rooms A/B/C)
    // ════════════════════════════════════════════════════════════════════════
    private void drawSalle(GraphicsContext g, double W, double H) {
        double[] d=geo(W,H);
        double rL=d[0],rT=d[1],rR=d[2],rB=d[3];
        double bwL=d[4],bwT=d[5],bwR=d[6],hz=d[7];

        drawCeil(g,rL,rT,rR,bwL,bwT,bwR,hz);
        drawFloor(g,rL,rB,rR,bwL,hz,bwR);
        drawLeftWall(g,rL,rT,rB,bwL,bwT,hz);
        drawRightWall(g,rR,rT,rB,bwR,bwT,hz);
        drawBackWall(g,bwL,bwT,bwR,hz);
        drawMoldings(g,rL,rT,rR,rB,bwL,bwT,bwR,hz);
        drawChandelier(g,W,H,W/2+camPan*W*0.06,bwT+H*0.04);
        drawBackArtworks(g,bwL,bwT,bwR,hz);
        drawSideArtworks(g,rL,rT,rB,bwL,bwT,hz,rR,bwR);
        drawShelves(g,W,H,rL,rT,rB,bwL,bwT,hz,rR,bwR);
        drawFloorPlants(g,W,H,rL,rR,rB);
        if(currentRoom==2) drawCoffeeCorner(g,W,H,rL,rB);
        drawTablesSpacious(g,W,H,rL,rR,rB,hz);
        drawRopeBarrier(g,W,H,rL,rR,rB,hz);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DRAW CORRIDOR — side view: player in middle, doors on left+right walls
    // ════════════════════════════════════════════════════════════════════════
    private void drawCorridor(GraphicsContext g, double W, double H) {
        double[] d=geo(W,H);
        double rL=d[0],rT=d[1],rR=d[2],rB=d[3];
        double bwL=d[4],bwT=d[5],bwR=d[6],hz=d[7];

        drawCeil(g,rL,rT,rR,bwL,bwT,bwR,hz);
        drawFloor(g,rL,rB,rR,bwL,hz,bwR);
        drawLeftWall(g,rL,rT,rB,bwL,bwT,hz);
        drawRightWall(g,rR,rT,rB,bwR,bwT,hz);
        drawBackWall(g,bwL,bwT,bwR,hz);
        drawMoldings(g,rL,rT,rR,rB,bwL,bwT,bwR,hz);
        drawChandelier(g,W,H,W/2,bwT+H*0.04);

        // Doors on LEFT wall (Salle A) and RIGHT wall (Salle B, C)
        drawCorridorDoorLeft(g,W,H,rL,rT,rB,bwL,bwT,hz);
        drawCorridorDoorRight(g,W,H,rR,rT,rB,bwR,bwT,hz);
        // Back wall door (Salle C — end of corridor)
        drawCorridorDoorBack(g,bwL,bwT,bwR,hz);

        // Vending machine in corridor
        drawVendingMachine(g,W,H,rL,rT,rB,bwL,hz);
        drawFloorPlants(g,W,H,rL,rR,rB);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CEILING
    // ════════════════════════════════════════════════════════════════════════
    private void drawCeil(GraphicsContext g,
                           double rL,double rT,double rR,
                           double bwL,double bwT,double bwR,double hz) {
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,CEIL_D),new Stop(1,CEIL_L)));
        g.fillPolygon(new double[]{rL,rR,bwR,bwL},new double[]{rT,rT,bwT,bwT},4);
        // Grid
        g.setStroke(Color.web("#a09880",0.4)); g.setLineWidth(0.7);
        for(int i=0;i<=12;i++){
            double t=(double)i/12;
            g.strokeLine(rL+t*(rR-rL),rT, bwL+t*(bwR-bwL),bwT);
        }
        for(int j=0;j<=7;j++){
            double t=(double)j/7;
            g.strokeLine(rL+t*(bwL-rL),rT+t*(bwT-rT), rR-t*(rR-bwR),rT+t*(bwT-rT));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FLOOR — takes 74% of screen height
    // ════════════════════════════════════════════════════════════════════════
    private void drawFloor(GraphicsContext g,
                            double rL,double rB,double rR,
                            double bwL,double hz,double bwR) {
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,FLOOR_L),new Stop(1,FLOOR_D)));
        g.fillPolygon(new double[]{rL,rR,bwR,bwL},new double[]{rB,rB,hz,hz},4);
        // Plank lines
        g.setStroke(Color.web("#7a5020",0.2)); g.setLineWidth(0.8);
        for(int i=0;i<=12;i++){
            double t=(double)i/12;
            g.strokeLine(bwL+t*(bwR-bwL),hz, rL+t*(rR-rL),rB);
        }
        for(int j=1;j<=9;j++){
            double t=(double)j/10;
            double y=rB-t*(rB-hz);
            double lx=rL+t*(bwL-rL), rx=rR-t*(rR-bwR);
            g.strokeLine(lx,y,rx,y);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  WALLS
    // ════════════════════════════════════════════════════════════════════════
    private void drawLeftWall(GraphicsContext g,
                               double rL,double rT,double rB,
                               double bwL,double bwT,double hz) {
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,WALL_D),new Stop(1,WALL)));
        g.fillPolygon(new double[]{rL,bwL,bwL,rL},new double[]{rT,bwT,hz,rB},4);
    }
    private void drawRightWall(GraphicsContext g,
                                double rR,double rT,double rB,
                                double bwR,double bwT,double hz) {
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,WALL),new Stop(1,WALL_D)));
        g.fillPolygon(new double[]{bwR,rR,rR,bwR},new double[]{bwT,rT,rB,hz},4);
    }
    private void drawBackWall(GraphicsContext g,
                               double bwL,double bwT,double bwR,double hz) {
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,WALL),new Stop(1,WALL_S)));
        g.fillRect(bwL,bwT,bwR-bwL,hz-bwT);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MOLDINGS
    // ════════════════════════════════════════════════════════════════════════
    private void drawMoldings(GraphicsContext g,
                               double rL,double rT,double rR,double rB,
                               double bwL,double bwT,double bwR,double hz) {
        LinearGradient gld=new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,GOLD),new Stop(1,GOLD2));
        g.setFill(gld);
        double bh=(rB-hz)*0.06;
        // Baseboard left
        g.fillPolygon(new double[]{rL,bwL,bwL,rL},new double[]{rB-bh,hz,hz+bh*0.3,rB},4);
        // Baseboard right
        g.fillPolygon(new double[]{bwR,rR,rR,bwR},new double[]{hz,rB-bh,rB,hz+bh*0.3},4);
        // Baseboard back
        g.fillRect(bwL,hz-bh*0.25,bwR-bwL,bh*0.25);
        // Crown left
        double ch=(hz-bwT)*0.12;
        g.fillPolygon(new double[]{rL,bwL,bwL,rL},new double[]{rT,bwT,bwT+ch*0.3,rT+ch},4);
        // Crown right
        g.fillPolygon(new double[]{bwR,rR,rR,bwR},new double[]{bwT,rT,rT+ch,bwT+ch*0.3},4);
        g.fillRect(bwL,bwT,bwR-bwL,ch*0.3);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CHANDELIER
    // ════════════════════════════════════════════════════════════════════════
    private void drawChandelier(GraphicsContext g,double W,double H,double cx,double topY) {
        double bot=topY+H*0.1, rr=W*0.035;
        g.setFill(new RadialGradient(0,0,cx,bot,rr*3,false,CycleMethod.NO_CYCLE,
            new Stop(0,Color.web("#ffd080",0.45)),new Stop(1,Color.TRANSPARENT)));
        g.fillOval(cx-rr*3,bot-rr*3,rr*6,rr*6);
        g.setStroke(GOLD); g.setLineWidth(2.5);
        g.strokeLine(cx,topY,cx,bot-rr);
        g.setStroke(GOLD); g.setLineWidth(3);
        g.strokeOval(cx-rr,bot-rr,rr*2,rr*2);
        g.setStroke(GOLD2); g.setLineWidth(1.5);
        g.strokeOval(cx-rr*0.5,bot-rr*0.5,rr,rr);
        for(int i=0;i<6;i++){
            double a=i*Math.PI/3;
            double ex=cx+Math.cos(a)*rr*1.1, ey=bot+Math.sin(a)*rr*0.5;
            g.setStroke(GOLD2); g.setLineWidth(1.8);
            g.strokeLine(cx,bot,ex,ey);
            double br=W*0.006;
            g.setFill(new RadialGradient(0,0,ex,ey,br*3,false,CycleMethod.NO_CYCLE,
                new Stop(0,Color.web("#fff8c0",0.95)),new Stop(1,Color.TRANSPARENT)));
            g.fillOval(ex-br*3,ey-br*3,br*6,br*6);
            g.setFill(Color.web("#ffd060")); g.fillOval(ex-br,ey-br,br*2,br*2);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ARTWORKS — minimalist line art inspired by your reference images
    // ════════════════════════════════════════════════════════════════════════
    private void drawBackArtworks(GraphicsContext g,double bwL,double bwT,double bwR,double hz) {
        double wW=bwR-bwL, wH=hz-bwT;
        String[][] arts=switch(currentRoom){
            case 1->new String[][]{{"Femme","woman"},{"Lignes","lines"},{"Fleurs","flowerface"}};
            case 2->new String[][]{{"Profil","profile"},{"Arcs","arcs"},{"Duo","duo"}};
            case 3->new String[][]{{"Visage","womanside"},{"Vagues","waves"},{"Fleur","flowerface"}};
            default->new String[][]{{"Femme","woman"},{"Lignes","lines"},{"Profil","profile"}};
        };
        double aW=wW*0.22, aH=wH*0.72;
        double aY=bwT+wH*0.08;
        double[] aXs={bwL+wW*0.06, bwL+wW*0.39, bwL+wW*0.70};
        for(int i=0;i<3;i++) artFrame(g,aXs[i],aY,aW,aH,arts[i][0],arts[i][1]);
    }

    private void drawSideArtworks(GraphicsContext g,
                                   double rL,double rT,double rB,
                                   double bwL,double bwT,double hz,
                                   double rR,double bwR) {
        double lwW=(bwL-rL)*0.48, lwH=(rB-rT)*0.22;
        double lwX=(rL+bwL)/2-lwW/2, lwY=(rT+hz)/2-lwH/2;
        artFrame(g,lwX,lwY,lwW,lwH,"","arcs");
        double rwW=(rR-bwR)*0.48, rwH=(rB-rT)*0.22;
        double rwX=(rR+bwR)/2-rwW/2, rwY=(rT+hz)/2-rwH/2;
        artFrame(g,rwX,rwY,rwW,rwH,"","waves");
    }

    private void artFrame(GraphicsContext g,double x,double y,double w,double h,
                           String label,String type) {
        g.setFill(Color.web("#000",0.18)); g.fillRect(x+3,y+3,w,h);
        g.setFill(FRAME2); g.fillRect(x-5,y-5,w+10,h+10);
        g.setFill(FRAME);  g.fillRect(x-3,y-3,w+6,h+6);
        g.setFill(Color.web("#e8e0d0")); g.fillRect(x,y,w,h);
        g.setFill(ART_BG); g.fillRect(x+6,y+6,w-12,h-12);
        lineArt(g,x+6,y+6,w-12,h-12,type);
        if(!label.isEmpty()){
            g.setFill(FRAME); g.setFont(Font.font("Georgia",9));
            g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.TOP);
            g.fillText(label,x+w/2,y+h+4);
        }
    }

    /** Minimalist line art — inspired by the reference images you shared */
    private void lineArt(GraphicsContext g,double x,double y,double w,double h,String type) {
        double cx=x+w/2, cy=y+h/2;
        g.setStroke(INK); g.setLineWidth(1.6);
        g.setLineCap(StrokeLineCap.ROUND); g.setLineJoin(StrokeLineJoin.ROUND);
        switch(type) {
            // Woman face — one continuous line (like your reference)
            case "woman" -> {
                g.beginPath();
                g.moveTo(cx-w*0.05,cy-h*0.42);
                g.bezierCurveTo(cx+w*0.28,cy-h*0.42, cx+w*0.32,cy-h*0.1, cx+w*0.18,cy+h*0.05);
                g.bezierCurveTo(cx+w*0.28,cy+h*0.18, cx+w*0.22,cy+h*0.38, cx,cy+h*0.42);
                g.bezierCurveTo(cx-w*0.22,cy+h*0.38, cx-w*0.28,cy+h*0.18, cx-w*0.18,cy+h*0.05);
                g.bezierCurveTo(cx-w*0.32,cy-h*0.1, cx-w*0.28,cy-h*0.42, cx-w*0.05,cy-h*0.42);
                g.stroke();
                // Eye
                g.beginPath(); g.moveTo(cx-w*0.12,cy-h*0.08);
                g.bezierCurveTo(cx-w*0.06,cy-h*0.14,cx+w*0.06,cy-h*0.14,cx+w*0.12,cy-h*0.08); g.stroke();
                // Nose
                g.beginPath(); g.moveTo(cx,cy-h*0.04); g.lineTo(cx-w*0.06,cy+h*0.06); g.stroke();
                // Lips
                g.beginPath(); g.moveTo(cx-w*0.1,cy+h*0.14);
                g.quadraticCurveTo(cx,cy+h*0.22,cx+w*0.1,cy+h*0.14); g.stroke();
                // Hair flowing
                g.beginPath(); g.moveTo(cx-w*0.05,cy-h*0.42);
                g.bezierCurveTo(cx-w*0.35,cy-h*0.35,cx-w*0.42,cy,cx-w*0.3,cy+h*0.3); g.stroke();
                // Leaf/flower detail
                g.beginPath(); g.moveTo(cx+w*0.18,cy-h*0.2);
                g.bezierCurveTo(cx+w*0.35,cy-h*0.35,cx+w*0.4,cy-h*0.15,cx+w*0.22,cy-h*0.05); g.stroke();
            }
            // Parallel curved lines (like your "lignes" reference)
            case "lines" -> {
                for(int i=0;i<5;i++){
                    double off=i*h*0.12-h*0.24;
                    g.beginPath();
                    g.moveTo(x+w*0.1,cy+off);
                    g.bezierCurveTo(cx-w*0.1,cy+off-h*0.15, cx+w*0.1,cy+off+h*0.15, x+w*0.9,cy+off);
                    g.stroke();
                }
            }
            // Woman with flowers (profile + flowers)
            case "flowerface" -> {
                // Profile
                g.beginPath();
                g.moveTo(cx+w*0.1,cy-h*0.42);
                g.bezierCurveTo(cx+w*0.3,cy-h*0.3,cx+w*0.28,cy-h*0.05,cx+w*0.15,cy+h*0.1);
                g.bezierCurveTo(cx+w*0.25,cy+h*0.25,cx+w*0.1,cy+h*0.42,cx-w*0.1,cy+h*0.42);
                g.stroke();
                // Flowers on head
                for(int i=0;i<3;i++){
                    double fx=cx-w*0.15+i*w*0.15, fy=cy-h*0.38+i*h*0.04;
                    for(int p=0;p<5;p++){
                        double a=p*Math.PI*2/5;
                        g.strokeOval(fx+Math.cos(a)*w*0.06-w*0.04,fy+Math.sin(a)*h*0.05-h*0.04,w*0.08,h*0.07);
                    }
                    g.strokeOval(fx-w*0.03,fy-h*0.03,w*0.06,h*0.05);
                }
            }
            // Side profile
            case "profile" -> {
                g.beginPath();
                g.moveTo(cx+w*0.05,cy-h*0.42);
                g.bezierCurveTo(cx+w*0.28,cy-h*0.38,cx+w*0.3,cy-h*0.2,cx+w*0.22,cy-h*0.05);
                g.bezierCurveTo(cx+w*0.32,cy+h*0.05,cx+w*0.28,cy+h*0.2,cx+w*0.1,cy+h*0.3);
                g.lineTo(cx-w*0.15,cy+h*0.42); g.stroke();
                // Eye
                g.strokeOval(cx+w*0.08,cy-h*0.18,w*0.1,h*0.07);
                // Neck
                g.beginPath(); g.moveTo(cx+w*0.1,cy+h*0.3); g.lineTo(cx+w*0.05,cy+h*0.42); g.stroke();
                // Abstract shape left
                g.setFill(Color.web("#c8a090",0.35));
                g.fillOval(x,cy,w*0.35,h*0.42);
                g.setFill(Color.TRANSPARENT);
            }
            // Concentric arcs (geometric)
            case "arcs" -> {
                for(int i=1;i<=5;i++){
                    double r=i*Math.min(w,h)*0.08;
                    g.strokeArc(cx-r,cy-r,r*2,r*2,0,180,javafx.scene.shape.ArcType.OPEN);
                }
            }
            // Two faces / duo
            case "duo" -> {
                // Face 1
                g.beginPath();
                g.moveTo(cx-w*0.05,cy-h*0.38);
                g.bezierCurveTo(cx+w*0.2,cy-h*0.38,cx+w*0.22,cy+h*0.1,cx,cy+h*0.38);
                g.bezierCurveTo(cx-w*0.22,cy+h*0.1,cx-w*0.2,cy-h*0.38,cx-w*0.05,cy-h*0.38);
                g.stroke();
                // Face 2 (mirrored, offset)
                g.beginPath();
                g.moveTo(cx+w*0.12,cy-h*0.28);
                g.bezierCurveTo(cx+w*0.38,cy-h*0.28,cx+w*0.4,cy+h*0.15,cx+w*0.18,cy+h*0.38);
                g.stroke();
            }
            // Woman side view
            case "womanside" -> {
                g.beginPath();
                g.moveTo(cx,cy-h*0.42);
                g.bezierCurveTo(cx+w*0.25,cy-h*0.35,cx+w*0.28,cy-h*0.1,cx+w*0.18,cy+h*0.05);
                g.bezierCurveTo(cx+w*0.28,cy+h*0.2,cx+w*0.2,cy+h*0.42,cx,cy+h*0.42);
                g.stroke();
                g.strokeOval(cx+w*0.06,cy-h*0.2,w*0.1,h*0.07);
                g.beginPath(); g.moveTo(cx+w*0.08,cy-h*0.06); g.lineTo(cx+w*0.02,cy+h*0.04); g.stroke();
                g.beginPath(); g.moveTo(cx-w*0.04,cy+h*0.12);
                g.quadraticCurveTo(cx+w*0.08,cy+h*0.2,cx+w*0.14,cy+h*0.12); g.stroke();
                // Hair
                g.beginPath(); g.moveTo(cx,cy-h*0.42);
                g.bezierCurveTo(cx-w*0.3,cy-h*0.3,cx-w*0.35,cy+h*0.1,cx-w*0.2,cy+h*0.35); g.stroke();
            }
            // Wave lines
            case "waves" -> {
                for(int i=0;i<6;i++){
                    double yy=y+h*0.15+i*h*0.13;
                    g.beginPath(); g.moveTo(x+w*0.05,yy);
                    for(int t=0;t<=20;t++){
                        double tx=x+w*0.05+t*(w*0.9/20);
                        double ty=yy+Math.sin(t*Math.PI/3.5)*h*0.04;
                        if(t==0) g.moveTo(tx,ty); else g.lineTo(tx,ty);
                    }
                    g.stroke();
                }
            }
            default -> g.strokeOval(cx-w*0.28,cy-h*0.32,w*0.56,h*0.52);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SHELVES
    // ════════════════════════════════════════════════════════════════════════
    private void drawShelves(GraphicsContext g,double W,double H,
                              double rL,double rT,double rB,
                              double bwL,double bwT,double hz,
                              double rR,double bwR) {
        double lwMx=(rL+bwL)/2, rwMx=(rR+bwR)/2;
        double sW=(bwL-rL)*0.7, sH=H*0.011;
        double sY=hz+(rB-hz)*0.35;
        shelf(g,lwMx-sW/2,sY,sW,sH,W,H);
        shelf(g,rwMx-sW/2,sY,sW,sH,W,H);
    }

    private void shelf(GraphicsContext g,double sx,double sy,double sw,double sh,double W,double H) {
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,SHELF_C),new Stop(1,SHELF_C.darker())));
        g.fillRect(sx,sy,sw,sh);
        g.setFill(SHELF_C.brighter()); g.fillRect(sx,sy,sw,sh*0.3);
        // Books
        double bx=sx+sw*0.04, bH=H*0.04, bW=sw*0.09;
        Color[] bc={Color.web("#8b4040"),Color.web("#4060a0"),Color.web("#406040"),
                    Color.web("#a07030"),Color.web("#604080")};
        for(int i=0;i<5;i++){
            double bh=bH*(0.8+i%3*0.1);
            g.setFill(bc[i]); g.fillRect(bx,sy-bh,bW*0.88,bh);
            g.setFill(bc[i].brighter()); g.fillRect(bx,sy-bh,bW*0.15,bh);
            g.setStroke(bc[i].darker()); g.setLineWidth(0.5); g.strokeRect(bx,sy-bh,bW*0.88,bh);
            bx+=bW;
        }
        // Small plant
        double px=sx+sw*0.68, potW=sw*0.1, potH=H*0.028;
        g.setFill(PLT_POT);
        g.fillPolygon(new double[]{px-potW*0.5,px+potW*0.5,px+potW*0.4,px-potW*0.4},
                      new double[]{sy-potH,sy-potH,sy,sy},4);
        g.setFill(PLT_G);
        g.save(); g.translate(px,sy-potH); g.rotate(-22);
        g.fillOval(-potW*0.3,-potH*1.1,potW*0.6,potH*0.9); g.restore();
        g.save(); g.translate(px,sy-potH); g.rotate(22);
        g.fillOval(-potW*0.3,-potH*1.1,potW*0.6,potH*0.9); g.restore();
        // Small frame
        double fW=sw*0.13,fH=H*0.038,fX=sx+sw*0.83,fY=sy-fH;
        g.setFill(FRAME2); g.fillRect(fX-2,fY-2,fW+4,fH+4);
        g.setFill(ART_BG); g.fillRect(fX,fY,fW,fH);
        g.setStroke(INK); g.setLineWidth(1);
        g.strokeOval(fX+fW*0.28,fY+fH*0.18,fW*0.44,fH*0.52);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FLOOR PLANTS
    // ════════════════════════════════════════════════════════════════════════
    private void drawFloorPlants(GraphicsContext g,double W,double H,
                                  double rL,double rR,double rB) {
        for(double px:new double[]{rL+W*0.055,rR-W*0.055}){
            double py=rB, pW=W*0.03, pH=H*0.07;
            g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
                new Stop(0,PLT_POT.darker()),new Stop(1,PLT_POT)));
            g.fillPolygon(new double[]{px-pW*0.6,px+pW*0.6,px+pW*0.5,px-pW*0.5},
                          new double[]{py-pH,py-pH,py,py},4);
            g.setFill(PLT_POT.brighter()); g.fillRect(px-pW*0.6,py-pH,pW*1.2,pH*0.12);
            g.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
                new Stop(0,PLT_G),new Stop(1,PLT_G2)));
            g.save(); g.translate(px,py-pH); g.rotate(-32);
            g.fillOval(-pW*0.35,-pH*1.35,pW*0.78,pH*1.2); g.restore();
            g.save(); g.translate(px,py-pH); g.rotate(32);
            g.fillOval(-pW*0.38,-pH*1.35,pW*0.78,pH*1.2); g.restore();
            g.setFill(PLT_G2); g.fillOval(px-pW*0.22,py-pH-pH*1.55,pW*0.44,pH*1.35);
            g.setStroke(PLT_G2); g.setLineWidth(1.5);
            g.strokeLine(px,py-pH,px,py-pH-pH*0.55);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COFFEE CORNER (Salle B)
    // ════════════════════════════════════════════════════════════════════════
    private void drawCoffeeCorner(GraphicsContext g,double W,double H,
                                   double rL,double rB) {
        double cx=rL+W*0.12, cy=rB-H*0.18;
        double cW=W*0.09, cH=H*0.14;
        // Counter
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,SHELF_C),new Stop(1,SHELF_C.darker())));
        g.fillRoundRect(cx-cW/2,cy,cW,cH*0.35,6,6);
        // Coffee cup
        double cupX=cx-W*0.012, cupY=cy-H*0.04;
        double cupW=W*0.024, cupH=H*0.032;
        g.setFill(Color.web("#6b9060")); g.fillOval(cupX-cupW*0.1,cupY+cupH*0.8,cupW*1.2,cupH*0.25);
        g.setFill(Color.web("#7a9870")); g.fillRoundRect(cupX,cupY,cupW,cupH,4,4);
        g.setFill(Color.web("#3a2010")); g.fillOval(cupX+cupW*0.1,cupY+cupH*0.15,cupW*0.8,cupH*0.55);
        // Steam
        g.setStroke(Color.web("#ffffff",0.5)); g.setLineWidth(1.2);
        for(int i=0;i<2;i++){
            double sx=cupX+cupW*0.3+i*cupW*0.35;
            g.beginPath(); g.moveTo(sx,cupY);
            g.bezierCurveTo(sx-cupW*0.1,cupY-cupH*0.4,sx+cupW*0.1,cupY-cupH*0.7,sx,cupY-cupH); g.stroke();
        }
        // Label
        g.setFill(GOLD); g.setFont(Font.font("Georgia",8));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.TOP);
        g.fillText("☕ Café",cx,cy+cH*0.38);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VENDING MACHINE (Corridor)
    // ════════════════════════════════════════════════════════════════════════
    private void drawVendingMachine(GraphicsContext g,double W,double H,
                                     double rL,double rT,double rB,
                                     double bwL,double hz) {
        double vmX=rL+W*0.08, vmY=rB-H*0.32;
        double vmW=W*0.065, vmH=H*0.28;
        // Body
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,VM_BODY.darker()),new Stop(0.5,VM_BODY),new Stop(1,VM_BODY.darker())));
        g.fillRoundRect(vmX,vmY,vmW,vmH,8,8);
        // Glass window
        g.setFill(Color.web("#b0d8e8",0.7));
        g.fillRoundRect(vmX+vmW*0.1,vmY+vmH*0.08,vmW*0.8,vmH*0.45,4,4);
        // Bottles inside (3 rows)
        Color[] bottleC={Color.web("#e06030"),Color.web("#30a060"),Color.web("#3060c0"),
                         Color.web("#c0a030"),Color.web("#a03060"),Color.web("#30a0a0")};
        for(int row=0;row<3;row++) for(int col=0;col<2;col++){
            double bx=vmX+vmW*0.18+col*vmW*0.35, by=vmY+vmH*0.12+row*vmH*0.13;
            double bW=vmW*0.22, bH=vmH*0.1;
            g.setFill(bottleC[row*2+col]);
            g.fillRoundRect(bx,by,bW,bH,3,3);
        }
        // Accent stripe
        g.setFill(VM_ACCENT);
        g.fillRect(vmX,vmY+vmH*0.55,vmW,vmH*0.04);
        // Slot
        g.setFill(Color.web("#888")); g.fillRoundRect(vmX+vmW*0.3,vmY+vmH*0.62,vmW*0.4,vmH*0.06,3,3);
        // Tray
        g.setFill(Color.web("#aaa")); g.fillRect(vmX+vmW*0.1,vmY+vmH*0.88,vmW*0.8,vmH*0.04);
        // Label
        g.setFill(Color.web("#4a2c0a")); g.setFont(Font.font("Georgia",FontWeight.BOLD,7));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.TOP);
        g.fillText("Boissons",vmX+vmW/2,vmY+vmH+3);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TABLES — spacious, spread across entire floor
    // ════════════════════════════════════════════════════════════════════════
    private void drawTablesSpacious(GraphicsContext g,double W,double H,
                                     double rL,double rR,double rB,double hz) {
        double floorH=rB-hz;
        // 6 tables in 2 rows × 3 cols, spread across full floor width
        // Row 1 (back): smaller due to perspective, y near horizon
        // Row 2 (front): larger, y near bottom
        double[][] positions = {
            // {relX, relY, scale}  relX=0..1 across floor, relY=0..1 down floor
            {0.18, 0.28, 0.62}, {0.50, 0.25, 0.62}, {0.82, 0.28, 0.62},
            {0.18, 0.68, 1.00}, {0.50, 0.65, 1.00}, {0.82, 0.68, 1.00},
        };
        double baseW=W*0.18, baseH=H*0.05, baseLeg=H*0.06;
        for(int i=0;i<positions.length;i++){
            double relX=positions[i][0], relY=positions[i][1], sc=positions[i][2];
            // Map relX with perspective (wider at bottom)
            double floorW_at_y = (rR-rL) - (rR-rL-bwR_approx(W))*relY;
            double floorL_at_y = rL + (bwL_approx(W)-rL)*relY;
            // Simpler: interpolate between back wall width and full width
            double bwL2=W*0.225, bwR2=W*0.775;
            double lx=bwL2+relX*(bwR2-bwL2)+(rL-bwL2)*(1-relY);
            double rx=bwR2-relX*(bwR2-bwL2)+(rR-bwR2)*(1-relY);
            double tx=rL+relX*(rR-rL);
            double ty=hz+relY*floorH;
            double tw=baseW*sc, th=baseH*sc, tl=baseLeg*sc;
            int key=currentRoom*100+i;
            drawTable(g,tx-tw/2,ty-th,tw,th,tl,getStatus(key),i,key,sc);
        }
    }

    private double bwL_approx(double W){ return W*0.225+camPan*W*0.14; }
    private double bwR_approx(double W){ return W*0.775+camPan*W*0.14; }

    private void drawTable(GraphicsContext g,double x,double y,double w,double h,double lH,
                            String status,int num,int key,double sc) {
        Color top=switch(status){case"mine"->MINE;case"occupied"->OCC;default->TBL_L;};
        Color top2=switch(status){case"mine"->MINE.darker();case"occupied"->OCC.darker();default->TBL_D;};
        // Shadow
        g.setFill(Color.web("#000",0.12));
        g.fillOval(x+w*0.08,y+h+lH-3,w*0.84,6*sc);
        // Legs
        double lw=w*0.035;
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,TBL_LEG.darker()),new Stop(1,TBL_LEG)));
        g.fillRect(x+w*0.08,y+h,lw,lH);
        g.fillRect(x+w*0.88-lw,y+h,lw,lH);
        g.setFill(TBL_LEG.darker());
        g.fillRect(x+w*0.2,y+h*0.3,lw*0.7,lH*0.55);
        g.fillRect(x+w*0.76-lw,y+h*0.3,lw*0.7,lH*0.55);
        // Front face
        g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0,top),new Stop(1,top2)));
        g.fillPolygon(new double[]{x,x+w,x+w-w*0.03,x+w*0.03},
                      new double[]{y+h*0.28,y+h*0.28,y+h,y+h},4);
        // Top surface
        g.setFill(top.brighter());
        g.fillPolygon(new double[]{x+w*0.03,x+w-w*0.03,x+w,x},
                      new double[]{y+h*0.28,y+h*0.28,y,y},4);
        g.setStroke(top2.darker()); g.setLineWidth(0.8);
        g.strokePolygon(new double[]{x,x+w,x+w-w*0.03,x+w*0.03},
                        new double[]{y+h*0.28,y+h*0.28,y+h,y+h},4);
        // Chairs
        Color cc=CHR;
        g.setFill(cc);
        g.fillRoundRect(x+w*0.12,y+h+lH*0.85,w*0.14,h*0.45,3,3);
        g.fillRoundRect(x+w*0.72,y+h+lH*0.85,w*0.14,h*0.45,3,3);
        g.setFill(cc.darker());
        g.fillRoundRect(x+w*0.16,y-h*0.38,w*0.12,h*0.3,2,2);
        g.fillRoundRect(x+w*0.68,y-h*0.38,w*0.12,h*0.3,2,2);
        // Status dot
        double dr=Math.min(w,h)*0.15;
        Color dc=switch(status){case"mine"->MINE;case"occupied"->OCC;default->FREE;};
        g.setFill(dc); g.fillOval(x+w/2-dr,y+h*0.04,dr*2,dr*2);
        g.setStroke(Color.WHITE); g.setLineWidth(1);
        g.strokeOval(x+w/2-dr,y+h*0.04,dr*2,dr*2);
        // Number
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial",FontWeight.BOLD,Math.max(9,h*0.5)));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
        g.fillText(String.valueOf(num+1),x+w/2,y+h*0.62);
        tableHits.add(new double[]{x,y,w,h+lH,currentRoom,num,key});
    }

    private String getStatus(int key){
        if(!reservations.containsKey(key)) return "free";
        return reservations.get(key)==myEquipeId?"mine":"occupied";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ROPE BARRIER
    // ════════════════════════════════════════════════════════════════════════
    private void drawRopeBarrier(GraphicsContext g,double W,double H,
                                  double rL,double rR,double rB,double hz) {
        double by=hz+(rB-hz)*0.15;
        double bL=rL+W*0.04, bR=rR-W*0.04;
        int n=5; double[] pxs=new double[n];
        for(int i=0;i<n;i++) pxs[i]=bL+i*(bR-bL)/(n-1);
        double pH=H*0.06, pW=W*0.006;
        for(double px:pxs){
            g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
                new Stop(0,POST.darker()),new Stop(0.5,GOLD),new Stop(1,POST.darker())));
            g.fillRect(px-pW/2,by-pH,pW,pH);
            g.setFill(new RadialGradient(-0.3,-0.3,px-pW*0.3,by-pH-pW,pW*1.2,false,CycleMethod.NO_CYCLE,
                new Stop(0,GOLD.brighter()),new Stop(1,GOLD2)));
            g.fillOval(px-pW,by-pH-pW*2,pW*2,pW*2);
            g.setFill(POST); g.fillRect(px-pW*1.5,by-pW*0.5,pW*3,pW*0.5);
        }
        g.setStroke(ROPE); g.setLineWidth(2.2);
        for(int i=0;i<n-1;i++){
            double x1=pxs[i],x2=pxs[i+1],ry=by-pH*0.72;
            g.beginPath(); g.moveTo(x1,ry);
            g.quadraticCurveTo((x1+x2)/2,ry+pH*0.18,x2,ry); g.stroke();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CORRIDOR DOORS — on side walls (left/right), not front wall
    // ════════════════════════════════════════════════════════════════════════
    private void drawCorridorDoorLeft(GraphicsContext g,double W,double H,
                                       double rL,double rT,double rB,
                                       double bwL,double bwT,double hz) {
        // Door on LEFT wall — perspective trapezoid
        double wallMidY=(rT+rB)/2;
        double dH=(rB-rT)*0.45, dW=(bwL-rL)*0.38;
        double dY=wallMidY-dH/2;
        // Door shape on left wall (trapezoid)
        double topW=dW*0.55, botW=dW;
        double topX=bwL-(bwL-rL)*0.35-topW/2;
        double botX=bwL-(bwL-rL)*0.35-botW/2;
        double topY=dY+dH*0.1, botY=dY+dH;
        // Door frame
        g.setFill(FRAME2);
        g.fillPolygon(new double[]{topX-4,topX+topW+4,botX+botW+5,botX-5},
                      new double[]{topY-4,topY-4,botY+4,botY+4},4);
        // Door fill
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,DOOR_C2),new Stop(0.5,DOOR_C),new Stop(1,DOOR_C2)));
        g.fillPolygon(new double[]{topX,topX+topW,botX+botW,botX},
                      new double[]{topY,topY,botY,botY},4);
        // Arch
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,DOOR_C2),new Stop(0.5,DOOR_C),new Stop(1,DOOR_C2)));
        g.fillArc(topX,topY-topW*0.5,topW,topW,0,180,javafx.scene.shape.ArcType.CHORD);
        // Knob
        g.setFill(GOLD); g.fillOval(topX+topW*0.72,topY+dH*0.35,topW*0.08,topW*0.08);
        // Label
        g.setFill(Color.web("#f5e6c8")); g.setFont(Font.font("Georgia",FontWeight.BOLD,Math.max(8,topW*0.2)));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.BOTTOM);
        g.fillText("Salle A",topX+topW/2,topY-topW*0.55);
        g.setFill(GOLD); g.setFont(Font.font("Georgia",8));
        g.setTextBaseline(VPos.TOP);
        g.fillText("Entrer →",topX+topW/2,botY+3);
        doorHits.add(new double[]{topX,topY-topW*0.5,topW,botY-topY+topW*0.5,1});
    }

    private void drawCorridorDoorRight(GraphicsContext g,double W,double H,
                                        double rR,double rT,double rB,
                                        double bwR,double bwT,double hz) {
        double wallMidY=(rT+rB)/2;
        double dH=(rB-rT)*0.45, dW=(rR-bwR)*0.38;
        double dY=wallMidY-dH/2;
        double topW=dW*0.55, botW=dW;
        double topX=bwR+(rR-bwR)*0.35-topW/2;
        double botX=bwR+(rR-bwR)*0.35-botW/2;
        double topY=dY+dH*0.1, botY=dY+dH;
        g.setFill(FRAME2);
        g.fillPolygon(new double[]{topX-4,topX+topW+4,botX+botW+5,botX-5},
                      new double[]{topY-4,topY-4,botY+4,botY+4},4);
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,DOOR_C2),new Stop(0.5,DOOR_C),new Stop(1,DOOR_C2)));
        g.fillPolygon(new double[]{topX,topX+topW,botX+botW,botX},
                      new double[]{topY,topY,botY,botY},4);
        g.fillArc(topX,topY-topW*0.5,topW,topW,0,180,javafx.scene.shape.ArcType.CHORD);
        g.setFill(GOLD); g.fillOval(topX+topW*0.2,topY+dH*0.35,topW*0.08,topW*0.08);
        g.setFill(Color.web("#f5e6c8")); g.setFont(Font.font("Georgia",FontWeight.BOLD,Math.max(8,topW*0.2)));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.BOTTOM);
        g.fillText("Salle B",topX+topW/2,topY-topW*0.55);
        g.setFill(GOLD); g.setFont(Font.font("Georgia",8));
        g.setTextBaseline(VPos.TOP);
        g.fillText("← Entrer",topX+topW/2,botY+3);
        doorHits.add(new double[]{topX,topY-topW*0.5,topW,botY-topY+topW*0.5,2});
    }

    private void drawCorridorDoorBack(GraphicsContext g,
                                       double bwL,double bwT,double bwR,double hz) {
        double wW=bwR-bwL, wH=hz-bwT;
        double dW=wW*0.28, dH=wH*0.75;
        double dX=bwL+wW/2-dW/2, dY=bwT+wH*0.18;
        g.setFill(FRAME2); g.fillRect(dX-4,dY-4,dW+8,dH+8);
        g.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
            new Stop(0,DOOR_C2),new Stop(0.5,DOOR_C),new Stop(1,DOOR_C2)));
        g.fillRect(dX,dY,dW,dH);
        g.fillArc(dX,dY-dW*0.5,dW,dW,0,180,javafx.scene.shape.ArcType.CHORD);
        g.setFill(GOLD); g.fillOval(dX+dW*0.72,dY+dH*0.45,dW*0.1,dW*0.1);
        g.setFill(Color.web("#f5e6c8")); g.setFont(Font.font("Georgia",FontWeight.BOLD,Math.max(8,dW*0.18)));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.BOTTOM);
        g.fillText("Salle C",dX+dW/2,dY-dW*0.55);
        g.setFill(GOLD); g.setFont(Font.font("Georgia",8));
        g.setTextBaseline(VPos.TOP);
        g.fillText("Entrer →",dX+dW/2,dY+dH+3);
        doorHits.add(new double[]{dX,dY-dW*0.5,dW,dH+dW*0.5,3});
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MOVE BUTTONS
    // ════════════════════════════════════════════════════════════════════════
    private void drawMoveBtns(GraphicsContext g,double W,double H) {
        double s=H*0.052, bx=W*0.01, by=H*0.44;
        btnL  = moveBtn(g,bx,           by+s*1.1,s,"◀",camPan>-0.9);
        btnR  = moveBtn(g,bx+s*1.1,     by+s*1.1,s,"▶",camPan< 0.9);
        btnZI = moveBtn(g,bx+s*0.55,    by,       s,"＋",true);
        btnZO = moveBtn(g,bx+s*0.55,    by+s*2.2, s,"－",true);
        g.setFill(Color.web("#c9a96e",0.65)); g.setFont(Font.font("Georgia",8));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.TOP);
        g.fillText("Vue",bx+s*0.55+s/2,by+s*3.3);
    }

    private double[] moveBtn(GraphicsContext g,double x,double y,double s,
                              String lbl,boolean on) {
        Color bg=on?Color.web("#f5e6c8",0.93):Color.web("#8b6614",0.3);
        Color bd=on?GOLD:Color.web("#8b6614",0.4);
        Color fg=on?Color.web("#4a2c0a"):Color.web("#8b6614",0.5);
        g.setFill(Color.web("#000",0.15)); g.fillRoundRect(x+2,y+2,s,s,8,8);
        g.setFill(bg); g.fillRoundRect(x,y,s,s,8,8);
        g.setStroke(bd); g.setLineWidth(1.8); g.strokeRoundRect(x,y,s,s,8,8);
        g.setFill(fg); g.setFont(Font.font("Georgia",FontWeight.BOLD,s*0.4));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
        g.fillText(lbl,x+s/2,y+s/2);
        return new double[]{x,y,s,s};
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NAV BAR
    // ════════════════════════════════════════════════════════════════════════
    private void drawNavBar(GraphicsContext g,double W,double H) {
        double bW=W*0.14,bH=H*0.058,bY=H*0.915;
        double pX=W/2-bW-W*0.05, nX=W/2+W*0.05;
        navBtn(g,pX,bY,bW,bH,"← PRÉCÉDENTE",currentRoom>0);
        navBtn(g,nX,bY,bW,bH,"SUIVANTE →",currentRoom<ROOM_NAMES.length-1);
        g.setFill(Color.web("#f5e6c8",0.95));
        g.setFont(Font.font("Georgia",FontWeight.BOLD,H*0.019));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
        g.fillText((currentRoom+1)+" / "+ROOM_NAMES.length,W/2,bY+bH/2);
        g.setFill(Color.web("#d4a96a"));
        g.setFont(Font.font("Georgia",FontWeight.BOLD,H*0.022));
        g.fillText(ROOM_NAMES[currentRoom],W/2,bY-bH*0.65);
        double ds=W*0.016, dx0=W/2-(ROOM_NAMES.length-1)*ds/2;
        for(int i=0;i<ROOM_NAMES.length;i++){
            double dx=dx0+i*ds, dy=bY+bH+H*0.018;
            g.setFill(i==currentRoom?GOLD:Color.web("#8b6614",0.5));
            g.fillOval(dx-5,dy-5,10,10);
            if(i==currentRoom){g.setStroke(GOLD.brighter());g.setLineWidth(1.5);g.strokeOval(dx-6,dy-6,12,12);}
        }
    }

    private void navBtn(GraphicsContext g,double x,double y,double w,double h,
                         String t,boolean on) {
        Color bg=on?Color.web("#f5e6c8",0.97):Color.web("#8b6614",0.3);
        Color bd=on?GOLD:Color.web("#8b6614",0.4);
        Color fg=on?Color.web("#4a2c0a"):Color.web("#8b6614",0.5);
        g.setFill(Color.web("#000",0.18)); g.fillRoundRect(x+2,y+2,w,h,10,10);
        g.setFill(bg); g.fillRoundRect(x,y,w,h,10,10);
        g.setStroke(bd); g.setLineWidth(2); g.strokeRoundRect(x,y,w,h,10,10);
        g.setFill(fg); g.setFont(Font.font("Georgia",FontWeight.BOLD,h*0.38));
        g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(VPos.CENTER);
        g.fillText(t,x+w/2,y+h/2);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HUD
    // ════════════════════════════════════════════════════════════════════════
    private void drawHUD(GraphicsContext g,double W,double H) {
        double bx=W*0.01,by=H*0.01,bw=W*0.24,bh=H*0.072;
        g.setFill(Color.web("#f5e6c8",0.93)); g.fillRoundRect(bx,by,bw,bh,10,10);
        g.setStroke(GOLD); g.setLineWidth(1.5); g.strokeRoundRect(bx,by,bw,bh,10,10);
        g.setFill(Color.web("#4a2c0a"));
        g.setFont(Font.font("Georgia",FontWeight.BOLD,H*0.021));
        g.setTextAlign(TextAlignment.LEFT); g.setTextBaseline(VPos.TOP);
        g.fillText("🏛 Espace 3D — Événements",bx+10,by+6);
        g.setFont(Font.font("Georgia",H*0.016)); g.setFill(Color.web("#8b6614"));
        g.fillText(ROOM_NAMES[currentRoom],bx+10,by+bh*0.52);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CLICK
    // ════════════════════════════════════════════════════════════════════════
    private void onClick(MouseEvent e) {
        double mx=e.getX(),my=e.getY(),W=canvas3D.getWidth(),H=canvas3D.getHeight();
        // Move buttons
        if(btnL !=null&&hit(mx,my,btnL)) {camPan=Math.max(-1,camPan-0.22);return;}
        if(btnR !=null&&hit(mx,my,btnR)) {camPan=Math.min( 1,camPan+0.22);return;}
        if(btnZI!=null&&hit(mx,my,btnZI)){camPan=0;return;} // reset pan
        if(btnZO!=null&&hit(mx,my,btnZO)){camPan=0;return;}
        // Nav buttons
        double bW=W*0.14,bH=H*0.058,bY=H*0.915;
        double pX=W/2-bW-W*0.05,nX=W/2+W*0.05;
        if(mx>=pX&&mx<=pX+bW&&my>=bY&&my<=bY+bH){prevRoom();return;}
        if(mx>=nX&&mx<=nX+bW&&my>=bY&&my<=bY+bH){nextRoom();return;}
        // Doors
        for(double[] d:doorHits) if(mx>=d[0]&&mx<=d[0]+d[2]&&my>=d[1]&&my<=d[1]+d[3]){goRoom((int)d[4]);return;}
        // Tables
        for(double[] t:tableHits) if(mx>=t[0]&&mx<=t[0]+t[2]&&my>=t[1]&&my<=t[1]+t[3]){
            int si=(int)t[4],tn=(int)t[5],key=(int)t[6];
            if("occupied".equals(getStatus(key))){setStatus("Table "+(tn+1)+" — occupée","#ef4444");return;}
            pendingSalleIdx=si;pendingTableNum=tn;
            setStatus("Table "+(tn+1)+" sélectionnée — cliquez Réserver","#22c55e");
            if(btnReserver!=null)btnReserver.setDisable(equipe==null);
            return;
        }
        canvas3D.requestFocus();
    }

    private boolean hit(double mx,double my,double[] b){return mx>=b[0]&&mx<=b[0]+b[2]&&my>=b[1]&&my<=b[1]+b[3];}

    private void goRoom(int i){currentRoom=i;camPan=0;pendingSalleIdx=-1;redraw();}
    private void nextRoom(){if(currentRoom<ROOM_NAMES.length-1)goRoom(currentRoom+1);}
    private void prevRoom(){if(currentRoom>0)goRoom(currentRoom-1);}

    private void setStatus(String m,String c){
        if(labelStatus!=null){labelStatus.setText(m);labelStatus.setStyle("-fx-font-size:12;-fx-font-weight:700;-fx-text-fill:"+c+";");}
    }

    @FXML private void onGoCorridor(){goRoom(0);}
    @FXML private void onGoSalleA()  {goRoom(1);}
    @FXML private void onGoSalleB()  {goRoom(2);}
    @FXML private void onGoSalleC()  {goRoom(3);}

    @FXML private void onReserver() {
        if(pendingSalleIdx<0||equipe==null)return;
        int key=pendingSalleIdx*100+pendingTableNum;
        if(participationService.reserverTable(evenement.getId(),equipe.getId(),key)){
            reservations.put(key,equipe.getId());
            setStatus("Table "+(pendingTableNum+1)+" réservée !","#3b82f6");
            if(btnReserver!=null)btnReserver.setDisable(true);
            if(btnLiberer !=null)btnLiberer.setDisable(false);
            pendingSalleIdx=-1;redraw();
        } else setStatus("Table déjà prise !","#ef4444");
    }

    @FXML private void onLiberer() {
        if(equipe==null)return;
        participationService.libererTable(evenement.getId(),equipe.getId());
        reservations.entrySet().removeIf(e->e.getValue()==myEquipeId);
        setStatus("Réservation libérée.","#888888");
        if(btnLiberer !=null)btnLiberer.setDisable(true);
        if(btnReserver!=null)btnReserver.setDisable(true);
        pendingSalleIdx=-1;redraw();
    }

    private void updateButtons() {
        if(equipe==null){if(btnReserver!=null)btnReserver.setDisable(true);if(btnLiberer!=null)btnLiberer.setDisable(true);return;}
        int t=participationService.getTableByEquipe(evenement.getId(),equipe.getId());
        if(btnLiberer!=null)btnLiberer.setDisable(t<0);
        if(t>=0){int s=t/100,tn=t%100;String[]n={"Entrée","Salle A","Salle B","Salle C"};setStatus("Réservé : Table "+(tn+1)+" — "+(s<n.length?n[s]:"Salle "+s),"#3b82f6");}
    }

    @FXML private void onRetour(){if(timer!=null)timer.stop();FrontNavHelper.goMesParticipations(null);}
    @FXML private void onHome()  {if(timer!=null)timer.stop();FrontNavHelper.goHome();}
    @FXML private void onProfile(){FrontNavHelper.goProfile();}
    @FXML private void onMesParticipations(){FrontNavHelper.goMesParticipations(null);}
    @FXML private void onLogout(){if(timer!=null)timer.stop();FrontNavHelper.goLogout();}
}
