package tn.esprit.models;

/**
 * Avatar customization model - stores user's avatar preferences
 */
public class AvatarCustomization {
    private int userId;
    private String hairStyle;      // "short", "long", "curly", "ponytail", "bun"
    private String hairColor;      // hex color
    private String skinTone;       // "light", "medium", "tan", "dark"
    private String outfit;         // "casual", "professional", "sporty", "academic"
    private String accessory;      // "none", "glasses", "hat", "headphones"
    private String backgroundColor; // hex color

    public AvatarCustomization() {
        // Default values
        this.hairStyle = "short";
        this.hairColor = "#7c3aed"; // purple
        this.skinTone = "medium";
        this.outfit = "casual";
        this.accessory = "none";
        this.backgroundColor = "#f5f3ff";
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getHairStyle() { return hairStyle; }
    public void setHairStyle(String hairStyle) { this.hairStyle = hairStyle; }

    public String getHairColor() { return hairColor; }
    public void setHairColor(String hairColor) { this.hairColor = hairColor; }

    public String getSkinTone() { return skinTone; }
    public void setSkinTone(String skinTone) { this.skinTone = skinTone; }

    public String getOutfit() { return outfit; }
    public void setOutfit(String outfit) { this.outfit = outfit; }

    public String getAccessory() { return accessory; }
    public void setAccessory(String accessory) { this.accessory = accessory; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    @Override
    public String toString() {
        return "AvatarCustomization{" +
                "userId=" + userId +
                ", hairStyle='" + hairStyle + '\'' +
                ", hairColor='" + hairColor + '\'' +
                ", skinTone='" + skinTone + '\'' +
                ", outfit='" + outfit + '\'' +
                ", accessory='" + accessory + '\'' +
                ", backgroundColor='" + backgroundColor + '\'' +
                '}';
    }
}
