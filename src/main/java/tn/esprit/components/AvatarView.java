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
        
        // Clear with anti-aliasing
        gc.clearRect(0, 0, w, h);
        
        // Enable smooth rendering
        gc.setImageSmoothing(true);
        
        // Background circle with gradient
        javafx.scene.paint.RadialGradient bgGradient = new javafx.scene.paint.RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.web(customization.getBackgroundColor())),
            new javafx.scene.paint.Stop(1, Color.web(customization.getBackgroundColor()).darker())
        );
        gc.setFill(bgGradient);
        gc.fillOval(0, 0, w, h);
        
        // Draw character based on animation
        double centerX = w / 2;
        double centerY = h / 2;
        
        // Body bounce for animations with easing
        double bounce = 0;
        if (currentAnimation == AnimationType.HAPPY) {
            bounce = Math.sin(animationFrame * 2) * 4;
        } else if (currentAnimation == AnimationType.CELEBRATING) {
            bounce = Math.sin(animationFrame * 3) * 6;
        } else {
            bounce = Math.sin(animationFrame) * 2; // Subtle breathing
        }
        
        // Slight rotation for celebrating
        double rotation = 0;
        if (currentAnimation == AnimationType.CELEBRATING) {
            rotation = Math.sin(animationFrame * 2) * 5;
        }
        
        gc.save();
        gc.translate(centerX, centerY + bounce);
        gc.rotate(rotation);
        gc.translate(-centerX, -(centerY + bounce));
        
        // Head (circle) with shadow
        Color skinColor = getSkinColor();
        double headSize = w * 0.5;
        
        // Shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.1));
        gc.fillOval(centerX - headSize/2 + 2, centerY - headSize/2 + bounce + 3, headSize, headSize);
        
        // Head
        gc.setFill(skinColor);
        gc.fillOval(centerX - headSize/2, centerY - headSize/2 + bounce, headSize, headSize);
        
        // Cheeks (rosy)
        if (currentAnimation == AnimationType.HAPPY || currentAnimation == AnimationType.CELEBRATING) {
            gc.setFill(Color.rgb(255, 182, 193, 0.5));
            gc.fillOval(centerX - headSize * 0.35, centerY + bounce, headSize * 0.15, headSize * 0.12);
            gc.fillOval(centerX + headSize * 0.2, centerY + bounce, headSize * 0.15, headSize * 0.12);
        }
        
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
        
        gc.restore();
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
        Color hairShadow = hairColor.darker();
        
        // Position hair ABOVE the head, not inside the face
        // Using cy - headSize * 0.6 to ensure hair is completely above the head circle
        double hairY = cy - headSize * 0.6;
        
        switch (customization.getHairStyle()) {
            case "short" -> {
                // Simple cap with highlights - covers top of head
                gc.setFill(hairShadow);
                gc.fillOval(cx - headSize/2 + 1, hairY + 1, headSize, headSize * 0.55);
                gc.setFill(hairColor);
                gc.fillOval(cx - headSize/2, hairY, headSize, headSize * 0.55);
                // Highlight
                gc.setFill(Color.rgb(255, 255, 255, 0.3));
                gc.fillOval(cx - headSize * 0.2, hairY + headSize * 0.15, headSize * 0.3, headSize * 0.15);
            }
            case "long" -> {
                // Top + sides with volume
                gc.setFill(hairShadow);
                gc.fillOval(cx - headSize/2 + 1, hairY + 1, headSize, headSize * 0.55);
                gc.setFill(hairColor);
                gc.fillOval(cx - headSize/2, hairY, headSize, headSize * 0.55);
                // Left side - starts from hair top
                gc.fillRoundRect(cx - headSize/2, cy - headSize/3, headSize * 0.18, headSize * 0.7, 10, 10);
                // Right side - starts from hair top
                gc.fillRoundRect(cx + headSize/2 - headSize * 0.18, cy - headSize/3, headSize * 0.18, headSize * 0.7, 10, 10);
                // Highlight
                gc.setFill(Color.rgb(255, 255, 255, 0.3));
                gc.fillOval(cx - headSize * 0.2, hairY + headSize * 0.15, headSize * 0.3, headSize * 0.15);
            }
            case "curly" -> {
                // Multiple circles with depth - positioned around top of head
                for (int i = 0; i < 6; i++) {
                    double angle = Math.PI + (i * Math.PI / 5);
                    double x = cx + Math.cos(angle) * headSize * 0.38;
                    double y = hairY + headSize * 0.2 + Math.sin(angle) * headSize * 0.25;
                    // Shadow
                    gc.setFill(hairShadow);
                    gc.fillOval(x - headSize * 0.11 + 1, y + 1, headSize * 0.24, headSize * 0.24);
                    // Curl
                    gc.setFill(hairColor);
                    gc.fillOval(x - headSize * 0.12, y, headSize * 0.24, headSize * 0.24);
                    // Highlight
                    gc.setFill(Color.rgb(255, 255, 255, 0.25));
                    gc.fillOval(x - headSize * 0.08, y + headSize * 0.03, headSize * 0.1, headSize * 0.1);
                }
            }
            case "ponytail" -> {
                // Base - covers top of head
                gc.setFill(hairShadow);
                gc.fillOval(cx - headSize/2 + 1, hairY + 1, headSize, headSize * 0.55);
                gc.setFill(hairColor);
                gc.fillOval(cx - headSize/2, hairY, headSize, headSize * 0.55);
                // Ponytail with volume - positioned at back
                gc.setFill(hairShadow);
                gc.fillOval(cx + headSize * 0.28 + 1, hairY + headSize * 0.15 + 1, headSize * 0.24, headSize * 0.52);
                gc.setFill(hairColor);
                gc.fillOval(cx + headSize * 0.28, hairY + headSize * 0.15, headSize * 0.24, headSize * 0.52);
                // Highlight
                gc.setFill(Color.rgb(255, 255, 255, 0.3));
                gc.fillOval(cx - headSize * 0.2, hairY + headSize * 0.15, headSize * 0.3, headSize * 0.15);
            }
            case "bun" -> {
                // Base - covers top of head
                gc.setFill(hairShadow);
                gc.fillOval(cx - headSize/2 + 1, hairY + 1, headSize, headSize * 0.45);
                gc.setFill(hairColor);
                gc.fillOval(cx - headSize/2, hairY, headSize, headSize * 0.45);
                // Bun with depth - positioned on top
                gc.setFill(hairShadow);
                gc.fillOval(cx - headSize * 0.14 + 1, hairY - headSize * 0.02 + 1, headSize * 0.32, headSize * 0.32);
                gc.setFill(hairColor);
                gc.fillOval(cx - headSize * 0.15, hairY - headSize * 0.02, headSize * 0.32, headSize * 0.32);
                // Highlight on bun
                gc.setFill(Color.rgb(255, 255, 255, 0.35));
                gc.fillOval(cx - headSize * 0.08, hairY + headSize * 0.03, headSize * 0.15, headSize * 0.15);
            }
        }
    }
    
    private void drawEyes(GraphicsContext gc, double cx, double cy, double headSize) {
        double eyeY = cy - headSize * 0.08;
        double eyeSpacing = headSize * 0.15;
        double eyeSize = headSize * 0.09;
        
        // Blink animation
        boolean blinking = (currentAnimation == AnimationType.IDLE && (int)(animationFrame * 10) % 50 == 0);
        
        // Eye expression based on animation
        double eyeOpenness = 1.0;
        if (currentAnimation == AnimationType.HAPPY || currentAnimation == AnimationType.CELEBRATING) {
            eyeOpenness = 0.7; // Squinted happy eyes
        }
        
        if (blinking) {
            // Closed eyes (curved lines)
            gc.setStroke(Color.web("#1e1b4b"));
            gc.setLineWidth(2.5);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.strokeArc(cx - eyeSpacing - eyeSize/2, eyeY - eyeSize/4, eyeSize, eyeSize/2, 
                        180, 180, javafx.scene.shape.ArcType.OPEN);
            gc.strokeArc(cx + eyeSpacing - eyeSize/2, eyeY - eyeSize/4, eyeSize, eyeSize/2, 
                        180, 180, javafx.scene.shape.ArcType.OPEN);
        } else {
            // Open eyes with depth
            // Eye whites
            gc.setFill(Color.WHITE);
            gc.fillOval(cx - eyeSpacing - eyeSize/2, eyeY - eyeSize/2 * eyeOpenness, 
                       eyeSize, eyeSize * eyeOpenness);
            gc.fillOval(cx + eyeSpacing - eyeSize/2, eyeY - eyeSize/2 * eyeOpenness, 
                       eyeSize, eyeSize * eyeOpenness);
            
            // Eye outline
            gc.setStroke(Color.web("#1e1b4b"));
            gc.setLineWidth(1.5);
            gc.strokeOval(cx - eyeSpacing - eyeSize/2, eyeY - eyeSize/2 * eyeOpenness, 
                         eyeSize, eyeSize * eyeOpenness);
            gc.strokeOval(cx + eyeSpacing - eyeSize/2, eyeY - eyeSize/2 * eyeOpenness, 
                         eyeSize, eyeSize * eyeOpenness);
            
            // Pupils with gradient
            double pupilSize = eyeSize * 0.55;
            javafx.scene.paint.RadialGradient pupilGradient = new javafx.scene.paint.RadialGradient(
                0, 0, 0.3, 0.3, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#2d2a5e")),
                new javafx.scene.paint.Stop(1, Color.web("#1e1b4b"))
            );
            gc.setFill(pupilGradient);
            gc.fillOval(cx - eyeSpacing - pupilSize/2, eyeY - pupilSize/2, pupilSize, pupilSize);
            gc.fillOval(cx + eyeSpacing - pupilSize/2, eyeY - pupilSize/2, pupilSize, pupilSize);
            
            // Double shine for depth
            gc.setFill(Color.rgb(255, 255, 255, 0.9));
            double shineSize = pupilSize * 0.35;
            gc.fillOval(cx - eyeSpacing - pupilSize/3, eyeY - pupilSize/3, shineSize, shineSize);
            gc.fillOval(cx + eyeSpacing - pupilSize/3, eyeY - pupilSize/3, shineSize, shineSize);
            
            gc.setFill(Color.rgb(255, 255, 255, 0.5));
            double shineSize2 = pupilSize * 0.2;
            gc.fillOval(cx - eyeSpacing + pupilSize/4, eyeY + pupilSize/6, shineSize2, shineSize2);
            gc.fillOval(cx + eyeSpacing + pupilSize/4, eyeY + pupilSize/6, shineSize2, shineSize2);
            
            // Eyelashes for feminine touch
            gc.setStroke(Color.web("#1e1b4b"));
            gc.setLineWidth(1.2);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            // Left eye lashes
            gc.strokeLine(cx - eyeSpacing + eyeSize/2.5, eyeY - eyeSize/2, 
                         cx - eyeSpacing + eyeSize/2.2, eyeY - eyeSize/1.7);
            // Right eye lashes
            gc.strokeLine(cx + eyeSpacing + eyeSize/2.5, eyeY - eyeSize/2, 
                         cx + eyeSpacing + eyeSize/2.2, eyeY - eyeSize/1.7);
        }
    }
    
    private void drawMouth(GraphicsContext gc, double cx, double cy, double headSize) {
        double mouthY = cy + headSize * 0.15;
        
        gc.setStroke(Color.web("#1e1b4b"));
        gc.setLineWidth(2.5);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        switch (currentAnimation) {
            case TALKING -> {
                // Open mouth (oval) with tongue
                double openAmount = Math.abs(Math.sin(animationFrame)) * headSize * 0.1;
                // Mouth interior
                gc.setFill(Color.web("#8b4049"));
                gc.fillOval(cx - headSize * 0.09, mouthY - openAmount/2, headSize * 0.18, openAmount + headSize * 0.02);
                // Tongue
                if (openAmount > headSize * 0.05) {
                    gc.setFill(Color.web("#ff6b9d"));
                    gc.fillOval(cx - headSize * 0.05, mouthY + openAmount * 0.2, headSize * 0.1, headSize * 0.04);
                }
                // Mouth outline
                gc.setStroke(Color.web("#1e1b4b"));
                gc.strokeOval(cx - headSize * 0.09, mouthY - openAmount/2, headSize * 0.18, openAmount + headSize * 0.02);
            }
            case HAPPY, CELEBRATING -> {
                // Big smile with teeth
                gc.setLineWidth(3);
                gc.strokeArc(cx - headSize * 0.14, mouthY - headSize * 0.1, 
                           headSize * 0.28, headSize * 0.18, 180, 180, javafx.scene.shape.ArcType.OPEN);
                // Teeth
                gc.setFill(Color.WHITE);
                gc.fillRect(cx - headSize * 0.08, mouthY - headSize * 0.02, headSize * 0.16, headSize * 0.04);
                gc.setStroke(Color.web("#1e1b4b"));
                gc.setLineWidth(1);
                gc.strokeLine(cx, mouthY - headSize * 0.02, cx, mouthY + headSize * 0.02);
            }
            case THINKING -> {
                // Thoughtful expression (slight curve)
                gc.strokeArc(cx - headSize * 0.08, mouthY - headSize * 0.02, 
                           headSize * 0.16, headSize * 0.06, 200, 140, javafx.scene.shape.ArcType.OPEN);
            }
            default -> {
                // Gentle smile with slight curve
                gc.setLineWidth(2.5);
                gc.strokeArc(cx - headSize * 0.12, mouthY - headSize * 0.06, 
                           headSize * 0.24, headSize * 0.12, 190, 160, javafx.scene.shape.ArcType.OPEN);
            }
        }
    }
    
    private void drawAccessory(GraphicsContext gc, double cx, double cy, double headSize) {
        switch (customization.getAccessory()) {
            case "glasses" -> {
                // Modern rounded glasses with reflection
                gc.setStroke(Color.web("#1e1b4b"));
                gc.setLineWidth(2.5);
                double glassSize = headSize * 0.13;
                double glassY = cy - headSize * 0.08;
                
                // Frames
                gc.strokeOval(cx - headSize * 0.22 - glassSize/2, glassY - glassSize/2, glassSize, glassSize);
                gc.strokeOval(cx + headSize * 0.22 - glassSize/2, glassY - glassSize/2, glassSize, glassSize);
                
                // Bridge
                gc.setLineWidth(2);
                gc.strokeLine(cx - headSize * 0.09, glassY, cx + headSize * 0.09, glassY);
                
                // Temples
                gc.strokeLine(cx - headSize * 0.22 - glassSize/2, glassY, cx - headSize * 0.4, glassY - headSize * 0.05);
                gc.strokeLine(cx + headSize * 0.22 + glassSize/2, glassY, cx + headSize * 0.4, glassY - headSize * 0.05);
                
                // Lens reflection
                gc.setFill(Color.rgb(255, 255, 255, 0.3));
                gc.fillOval(cx - headSize * 0.25, glassY - headSize * 0.08, glassSize * 0.4, glassSize * 0.5);
                gc.fillOval(cx + headSize * 0.19, glassY - headSize * 0.08, glassSize * 0.4, glassSize * 0.5);
            }
            case "hat" -> {
                // Stylish cap with depth
                gc.setFill(Color.web("#7c3aed"));
                double hatY = cy - headSize * 0.52;
                
                // Shadow
                gc.setFill(Color.rgb(0, 0, 0, 0.2));
                gc.fillRect(cx - headSize * 0.36, hatY + 2, headSize * 0.72, headSize * 0.1);
                gc.fillRoundRect(cx - headSize * 0.26, hatY - headSize * 0.18 + 2, headSize * 0.52, headSize * 0.2, 8, 8);
                
                // Brim
                gc.setFill(Color.web("#7c3aed"));
                gc.fillRect(cx - headSize * 0.36, hatY, headSize * 0.72, headSize * 0.1);
                
                // Crown
                gc.fillRoundRect(cx - headSize * 0.26, hatY - headSize * 0.2, headSize * 0.52, headSize * 0.2, 8, 8);
                
                // Highlight
                gc.setFill(Color.rgb(255, 255, 255, 0.3));
                gc.fillRoundRect(cx - headSize * 0.22, hatY - headSize * 0.18, headSize * 0.3, headSize * 0.08, 4, 4);
                
                // Button on top
                gc.setFill(Color.web("#6d28d9"));
                gc.fillOval(cx - headSize * 0.05, hatY - headSize * 0.22, headSize * 0.1, headSize * 0.05);
            }
            case "headphones" -> {
                // Modern headphones with padding
                gc.setStroke(Color.web("#7c3aed"));
                gc.setLineWidth(5);
                gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                
                // Headband
                gc.strokeArc(cx - headSize * 0.42, cy - headSize * 0.35, 
                           headSize * 0.84, headSize * 0.7, 0, 180, javafx.scene.shape.ArcType.OPEN);
                
                // Ear cups with depth
                // Left cup shadow
                gc.setFill(Color.rgb(0, 0, 0, 0.2));
                gc.fillRoundRect(cx - headSize * 0.48, cy - headSize * 0.08, headSize * 0.18, headSize * 0.24, 8, 8);
                // Left cup
                gc.setFill(Color.web("#7c3aed"));
                gc.fillRoundRect(cx - headSize * 0.48, cy - headSize * 0.1, headSize * 0.18, headSize * 0.24, 8, 8);
                // Left padding
                gc.setFill(Color.web("#9f7aea"));
                gc.fillRoundRect(cx - headSize * 0.45, cy - headSize * 0.07, headSize * 0.12, headSize * 0.18, 6, 6);
                
                // Right cup shadow
                gc.setFill(Color.rgb(0, 0, 0, 0.2));
                gc.fillRoundRect(cx + headSize * 0.3, cy - headSize * 0.08, headSize * 0.18, headSize * 0.24, 8, 8);
                // Right cup
                gc.setFill(Color.web("#7c3aed"));
                gc.fillRoundRect(cx + headSize * 0.3, cy - headSize * 0.1, headSize * 0.18, headSize * 0.24, 8, 8);
                // Right padding
                gc.setFill(Color.web("#9f7aea"));
                gc.fillRoundRect(cx + headSize * 0.33, cy - headSize * 0.07, headSize * 0.12, headSize * 0.18, 6, 6);
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
        
        Color outfitShadow = outfitColor.darker();
        
        // Collar/shirt with depth
        double collarWidth = headSize * 0.65;
        double collarHeight = headSize * 0.18;
        
        // Shadow
        gc.setFill(outfitShadow);
        gc.fillRoundRect(cx - collarWidth/2 + 1, cy + 1, collarWidth, collarHeight, 8, 8);
        
        // Main outfit
        gc.setFill(outfitColor);
        gc.fillRoundRect(cx - collarWidth/2, cy, collarWidth, collarHeight, 8, 8);
        
        // Outfit-specific details
        switch (customization.getOutfit()) {
            case "professional" -> {
                // Suit with tie
                gc.setFill(Color.WHITE);
                double[] xPoints = {cx, cx - collarWidth * 0.15, cx + collarWidth * 0.15};
                double[] yPoints = {cy + collarHeight * 0.9, cy, cy};
                gc.fillPolygon(xPoints, yPoints, 3);
                
                // Tie
                gc.setFill(Color.web("#dc2626"));
                gc.fillRect(cx - headSize * 0.04, cy + collarHeight * 0.2, headSize * 0.08, collarHeight * 0.7);
                // Tie knot
                double[] tieX = {cx, cx - headSize * 0.06, cx + headSize * 0.06};
                double[] tieY = {cy + collarHeight * 0.2, cy, cy};
                gc.fillPolygon(tieX, tieY, 3);
            }
            case "sporty" -> {
                // Athletic shirt with stripes
                gc.setFill(Color.WHITE);
                gc.fillRect(cx - collarWidth * 0.35, cy + collarHeight * 0.3, collarWidth * 0.15, collarHeight * 0.1);
                gc.fillRect(cx + collarWidth * 0.2, cy + collarHeight * 0.3, collarWidth * 0.15, collarHeight * 0.1);
                
                // Number
                gc.setFill(Color.WHITE);
                gc.setFont(new javafx.scene.text.Font("Arial Bold", headSize * 0.15));
                gc.fillText("7", cx - headSize * 0.04, cy + collarHeight * 0.75);
            }
            case "academic" -> {
                // Academic robe with buttons
                gc.setFill(Color.web("#fbbf24"));
                for (int i = 0; i < 3; i++) {
                    double btnY = cy + collarHeight * (0.25 + i * 0.25);
                    gc.fillOval(cx - headSize * 0.03, btnY, headSize * 0.06, headSize * 0.06);
                }
            }
            default -> {
                // Casual with pocket
                gc.setFill(outfitShadow);
                gc.fillRoundRect(cx - collarWidth * 0.25, cy + collarHeight * 0.3, 
                               collarWidth * 0.2, collarHeight * 0.4, 4, 4);
            }
        }
        
        // Highlight
        gc.setFill(Color.rgb(255, 255, 255, 0.2));
        gc.fillRoundRect(cx - collarWidth * 0.4, cy + collarHeight * 0.1, 
                        collarWidth * 0.3, collarHeight * 0.15, 4, 4);
    }
    
    public void stopAnimation() {
        if (animationTimeline != null) {
            animationTimeline.stop();
        }
    }
}
