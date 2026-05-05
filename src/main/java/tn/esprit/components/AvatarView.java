package tn.esprit.components;

import javafx.animation.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import tn.esprit.models.AvatarCustomization;

/**
 * 2D Animated Avatar Component
 * Cute character with customizable appearance and animations
 */
public class AvatarView extends StackPane {
    
    private Canvas canvas;
    private AvatarCustomization customization;
    private AnimationType currentAnimation = AnimationType.IDLE;
    private Timeline animationTimeline;
    private double animationFrame = 0;
    
    public enum AnimationType {
        IDLE,       // Breathing, blinking
        TALKING,    // Mouth moving
        THINKING,   // Hand on chin
        HAPPY,      // Smiling, jumping
        CELEBRATING // Arms up, confetti
    }
    
    public AvatarView(double size) {
        this.canvas = new Canvas(size, size);
        this.customization = new AvatarCustomization();
        this.getChildren().add(canvas);
        
        // Start idle animation
        startIdleAnimation();
    }
    
    public void setCustomization(AvatarCustomization customization) {
        this.customization = customization;
        redraw();
    }
    
    public AvatarCustomization getCustomization() {
        return customization;
    }
    
    public void playAnimation(AnimationType type) {
        if (animationTimeline != null) {
            animationTimeline.stop();
        }
        currentAnimation = type;
        animationFrame = 0;
        
        switch (type) {
            case TALKING -> startTalkingAnimation();
            case THINKING -> startThinkingAnimation();
            case HAPPY -> startHappyAnimation();
            case CELEBRATING -> startCelebratingAnimation();
            default -> startIdleAnimation();
        }
    }
    
    private void startIdleAnimation() {
        animationTimeline = new Timeline(
            new KeyFrame(Duration.millis(50), e -> {
                animationFrame += 0.1;
                redraw();
            })
        );
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
        animationTimeline.play();
    }
    
    private void startTalkingAnimation() {
        animationTimeline = new Timeline(
            new KeyFrame(Duration.millis(100), e -> {
                animationFrame += 0.3;
                redraw();
            })
        );
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
        animationTimeline.play();
    }
    
    private void startThinkingAnimation() {
        animationTimeline = new Timeline(
            new KeyFrame(Duration.millis(80), e -> {
                animationFrame += 0.15;
                redraw();
            })
        );
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
        animationTimeline.play();
    }
    
    private void startHappyAnimation() {
        animationTimeline = new Timeline(
            new KeyFrame(Duration.millis(60), e -> {
                animationFrame += 0.2;
                redraw();
            })
        );
        animationTimeline.setCycleCount(30); // 1.8 seconds
        animationTimeline.setOnFinished(e -> startIdleAnimation());
        animationTimeline.play();
    }
    
    private void startCelebratingAnimation() {
        animationTimeline = new Timeline(
            new KeyFrame(Duration.millis(50), e -> {
                animationFrame += 0.25;
                redraw();
            })
        );
        animationTimeline.setCycleCount(40); // 2 seconds
        animationTimeline.setOnFinished(e -> startIdleAnimation());
        animationTimeline.play();
    }
    
    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        
        // Clear
        gc.clearRect(0, 0, w, h);
        
        // Background circle
        gc.setFill(Color.web(customization.getBackgroundColor()));
        gc.fillOval(0, 0, w, h);
        
        // Draw character based on animation
        double centerX = w / 2;
        double centerY = h / 2;
        
        // Body bounce for animations
        double bounce = 0;
        if (currentAnimation == AnimationType.HAPPY) {
            bounce = Math.sin(animationFrame * 2) * 3;
        } else if (currentAnimation == AnimationType.CELEBRATING) {
            bounce = Math.sin(animationFrame * 3) * 5;
        } else {
            bounce = Math.sin(animationFrame) * 1.5; // Subtle breathing
        }
        
        // Head (circle)
        Color skinColor = getSkinColor();
        gc.setFill(skinColor);
        double headSize = w * 0.5;
        gc.fillOval(centerX - headSize/2, centerY - headSize/2 + bounce, headSize, headSize);
        
        // Hair
        drawHair(gc, centerX, centerY + bounce, headSize);
        
        // Eyes
        drawEyes(gc, centerX, centerY + bounce, headSize);
        
        // Mouth
        drawMouth(gc, centerX, centerY + bounce, headSize);
        
        // Accessory
        drawAccessory(gc, centerX, centerY + bounce, headSize);
        
        // Outfit indicator (small collar/shirt)
        drawOutfit(gc, centerX, centerY + bounce + headSize/2, headSize);
    }
    
    private Color getSkinColor() {
        return switch (customization.getSkinTone()) {
            case "light" -> Color.web("#fde4cd");
            case "medium" -> Color.web("#f0c89e");
            case "tan" -> Color.web("#d4a574");
            case "dark" -> Color.web("#8d5524");
            default -> Color.web("#f0c89e");
        };
    }
    
    private void drawHair(GraphicsContext gc, double cx, double cy, double headSize) {
        Color hairColor = Color.web(customization.getHairColor());
        gc.setFill(hairColor);
        
        double hairY = cy - headSize * 0.35;
        
        switch (customization.getHairStyle()) {
            case "short" -> {
                // Simple cap
                gc.fillOval(cx - headSize/2, hairY, headSize, headSize * 0.4);
            }
            case "long" -> {
                // Top + sides
                gc.fillOval(cx - headSize/2, hairY, headSize, headSize * 0.4);
                gc.fillRect(cx - headSize/2, cy - headSize/4, headSize * 0.15, headSize * 0.6);
                gc.fillRect(cx + headSize/2 - headSize * 0.15, cy - headSize/4, headSize * 0.15, headSize * 0.6);
            }
            case "curly" -> {
                // Multiple circles
                for (int i = 0; i < 5; i++) {
                    double angle = Math.PI + (i * Math.PI / 4);
                    double x = cx + Math.cos(angle) * headSize * 0.35;
                    double y = hairY + Math.sin(angle) * headSize * 0.2;
                    gc.fillOval(x - headSize * 0.12, y, headSize * 0.24, headSize * 0.24);
                }
            }
            case "ponytail" -> {
                gc.fillOval(cx - headSize/2, hairY, headSize, headSize * 0.4);
                gc.fillOval(cx + headSize * 0.3, hairY - headSize * 0.1, headSize * 0.2, headSize * 0.5);
            }
            case "bun" -> {
                gc.fillOval(cx - headSize/2, hairY, headSize, headSize * 0.3);
                gc.fillOval(cx - headSize * 0.15, hairY - headSize * 0.2, headSize * 0.3, headSize * 0.3);
            }
        }
    }
    
    private void drawEyes(GraphicsContext gc, double cx, double cy, double headSize) {
        double eyeY = cy - headSize * 0.08;
        double eyeSpacing = headSize * 0.15;
        double eyeSize = headSize * 0.08;
        
        // Blink animation
        boolean blinking = (currentAnimation == AnimationType.IDLE && (int)(animationFrame * 10) % 50 == 0);
        
        if (blinking) {
            // Closed eyes (lines)
            gc.setStroke(Color.web("#1e1b4b"));
            gc.setLineWidth(2);
            gc.strokeLine(cx - eyeSpacing - eyeSize/2, eyeY, cx - eyeSpacing + eyeSize/2, eyeY);
            gc.strokeLine(cx + eyeSpacing - eyeSize/2, eyeY, cx + eyeSpacing + eyeSize/2, eyeY);
        } else {
            // Open eyes
            gc.setFill(Color.WHITE);
            gc.fillOval(cx - eyeSpacing - eyeSize/2, eyeY - eyeSize/2, eyeSize, eyeSize);
            gc.fillOval(cx + eyeSpacing - eyeSize/2, eyeY - eyeSize/2, eyeSize, eyeSize);
            
            // Pupils
            gc.setFill(Color.web("#1e1b4b"));
            double pupilSize = eyeSize * 0.5;
            gc.fillOval(cx - eyeSpacing - pupilSize/2, eyeY - pupilSize/2, pupilSize, pupilSize);
            gc.fillOval(cx + eyeSpacing - pupilSize/2, eyeY - pupilSize/2, pupilSize, pupilSize);
            
            // Shine
            gc.setFill(Color.WHITE);
            double shineSize = pupilSize * 0.3;
            gc.fillOval(cx - eyeSpacing - pupilSize/4, eyeY - pupilSize/3, shineSize, shineSize);
            gc.fillOval(cx + eyeSpacing - pupilSize/4, eyeY - pupilSize/3, shineSize, shineSize);
        }
    }
    
    private void drawMouth(GraphicsContext gc, double cx, double cy, double headSize) {
        double mouthY = cy + headSize * 0.15;
        
        gc.setStroke(Color.web("#1e1b4b"));
        gc.setLineWidth(2);
        
        switch (currentAnimation) {
            case TALKING -> {
                // Open mouth (oval)
                double openAmount = Math.abs(Math.sin(animationFrame)) * headSize * 0.08;
                gc.setFill(Color.web("#ff6b9d"));
                gc.fillOval(cx - headSize * 0.08, mouthY - openAmount/2, headSize * 0.16, openAmount);
            }
            case HAPPY, CELEBRATING -> {
                // Big smile
                gc.strokeArc(cx - headSize * 0.12, mouthY - headSize * 0.08, 
                           headSize * 0.24, headSize * 0.16, 180, 180, javafx.scene.shape.ArcType.OPEN);
            }
            case THINKING -> {
                // Small line
                gc.strokeLine(cx - headSize * 0.06, mouthY, cx + headSize * 0.06, mouthY);
            }
            default -> {
                // Gentle smile
                gc.strokeArc(cx - headSize * 0.1, mouthY - headSize * 0.05, 
                           headSize * 0.2, headSize * 0.1, 180, 180, javafx.scene.shape.ArcType.OPEN);
            }
        }
    }
    
    private void drawAccessory(GraphicsContext gc, double cx, double cy, double headSize) {
        switch (customization.getAccessory()) {
            case "glasses" -> {
                gc.setStroke(Color.web("#1e1b4b"));
                gc.setLineWidth(2);
                double glassSize = headSize * 0.12;
                double glassY = cy - headSize * 0.08;
                gc.strokeOval(cx - headSize * 0.2 - glassSize/2, glassY - glassSize/2, glassSize, glassSize);
                gc.strokeOval(cx + headSize * 0.2 - glassSize/2, glassY - glassSize/2, glassSize, glassSize);
                gc.strokeLine(cx - headSize * 0.08, glassY, cx + headSize * 0.08, glassY);
            }
            case "hat" -> {
                gc.setFill(Color.web("#7c3aed"));
                double hatY = cy - headSize * 0.5;
                gc.fillRect(cx - headSize * 0.35, hatY, headSize * 0.7, headSize * 0.08);
                gc.fillRect(cx - headSize * 0.25, hatY - headSize * 0.2, headSize * 0.5, headSize * 0.2);
            }
            case "headphones" -> {
                gc.setStroke(Color.web("#7c3aed"));
                gc.setLineWidth(4);
                gc.strokeArc(cx - headSize * 0.4, cy - headSize * 0.3, 
                           headSize * 0.8, headSize * 0.6, 0, 180, javafx.scene.shape.ArcType.OPEN);
                gc.setFill(Color.web("#7c3aed"));
                gc.fillOval(cx - headSize * 0.45, cy - headSize * 0.05, headSize * 0.15, headSize * 0.2);
                gc.fillOval(cx + headSize * 0.3, cy - headSize * 0.05, headSize * 0.15, headSize * 0.2);
            }
        }
    }
    
    private void drawOutfit(GraphicsContext gc, double cx, double cy, double headSize) {
        Color outfitColor = switch (customization.getOutfit()) {
            case "professional" -> Color.web("#1e40af");
            case "sporty" -> Color.web("#059669");
            case "academic" -> Color.web("#7c3aed");
            default -> Color.web("#f59e0b"); // casual
        };
        
        gc.setFill(outfitColor);
        // Simple collar/shirt indicator
        double collarWidth = headSize * 0.6;
        double collarHeight = headSize * 0.15;
        gc.fillRect(cx - collarWidth/2, cy, collarWidth, collarHeight);
        
        // V-neck for professional
        if ("professional".equals(customization.getOutfit())) {
            gc.setFill(Color.WHITE);
            double[] xPoints = {cx, cx - collarWidth * 0.2, cx + collarWidth * 0.2};
            double[] yPoints = {cy + collarHeight * 0.8, cy, cy};
            gc.fillPolygon(xPoints, yPoints, 3);
        }
    }
    
    public void stopAnimation() {
        if (animationTimeline != null) {
            animationTimeline.stop();
        }
    }
}
