# Palette de Couleurs Complète — Quiz AutoLearn
# À utiliser dans JavaFX (CSS) et Web (CSS/Twig)

---

## 1. FOND PRINCIPAL — Le violet avec les barres diagonales

C'est la combinaison de 2 propriétés CSS appliquées sur `body` :

```css
/* ① La couleur de fond violet */
background: #46178F;

/* ② Le motif des barres diagonales semi-transparentes par-dessus */
background-image: repeating-linear-gradient(
    45deg,                              /* angle des barres */
    transparent,                        /* bande transparente */
    transparent        35px,            /* largeur bande transparente */
    rgba(255,255,255,.05) 35px,         /* début bande blanche 5% opacité */
    rgba(255,255,255,.05) 70px          /* fin bande blanche = 35px de large */
);
```

### Résultat visuel
```
fond violet #46178F
  + bandes blanches à 5% d'opacité tous les 35px à 45°
  = effet "rayures diagonales subtiles" comme dans la capture
```

### En JavaFX CSS (quiz-styles.css)
```css
/* JavaFX ne supporte pas repeating-linear-gradient nativement */
/* Solution : utiliser une image PNG de texture en overlay */

.quiz-background {
    -fx-background-color: #46178F;
    /* Pour les barres : utiliser une image PNG transparente en overlay */
    /* OU dessiner avec Canvas en Java */
}
```

### En JavaFX Java (dessiner les barres avec Canvas)
```java
// Créer un Canvas par-dessus le fond violet
Canvas canvas = new Canvas(1024, 768);
GraphicsContext gc = canvas.getGraphicsContext2D();

// Fond violet
gc.setFill(Color.web("#46178F"));
gc.fillRect(0, 0, 1024, 768);

// Barres diagonales blanches à 5% d'opacité
gc.setStroke(Color.rgb(255, 255, 255, 0.05));
gc.setLineWidth(35);
for (int i = -768; i < 1024 + 768; i += 70) {
    gc.strokeLine(i, 0, i + 768, 768);
}
```

---

## 2. TOUTES LES COULEURS PAR ÉLÉMENT

### Fond et structure
| Élément | Couleur | Code |
|---------|---------|------|
| Fond principal | Violet foncé | `#46178F` |
| Barres diagonales | Blanc 5% | `rgba(255,255,255,0.05)` |
| Header transparent | Noir 30% | `rgba(0,0,0,0.3)` |
| Barre progression fond | Blanc 20% | `rgba(255,255,255,0.2)` |

---

### Options de réponse (les 4 couleurs Kahoot)
| Position | Couleur | Dégradé complet |
|----------|---------|-----------------|
| Haut-gauche ▲ | Rouge | `linear-gradient(135deg, #e74c3c 0%, #c0392b 100%)` |
| Haut-droite ◆ | Bleu | `linear-gradient(135deg, #3498db 0%, #2980b9 100%)` |
| Bas-gauche ● | Orange | `linear-gradient(135deg, #f39c12 0%, #e67e22 100%)` |
| Bas-droite ■ | Vert | `linear-gradient(135deg, #2ecc71 0%, #27ae60 100%)` |

---

### Boutons d'action
| Bouton | Couleur | Dégradé |
|--------|---------|---------|
| Commencer le quiz | Vert | `linear-gradient(135deg, #2ecc71 0%, #27ae60 100%)` |
| Soumettre le quiz | Vert | `linear-gradient(135deg, #2ecc71 0%, #27ae60 100%)` |
| Refaire le quiz | Bleu | `linear-gradient(135deg, #3498db 0%, #2980b9 100%)` |
| Nouvelle tentative | Orange | `linear-gradient(135deg, #f39c12 0%, #e67e22 100%)` |
| Retour / Autres quiz | Blanc | `background: white; color: #46178F; border: 3px solid #46178F` |
| Accueil | Vert | `linear-gradient(135deg, #2ecc71 0%, #27ae60 100%)` |
| Désactivé | Gris | `linear-gradient(135deg, #95a5a6 0%, #7f8c8d 100%)` |

---

### Badges de performance (résultats)
| Score | Couleur | Dégradé |
|-------|---------|---------|
| ≥ 80% Excellent | Vert | `linear-gradient(135deg, #2ecc71 0%, #27ae60 100%)` |
| ≥ 60% Bien joué | Bleu | `linear-gradient(135deg, #3498db 0%, #2980b9 100%)` |
| ≥ 40% Peut mieux | Orange | `linear-gradient(135deg, #f39c12 0%, #e67e22 100%)` |
| < 40% À revoir | Rouge | `linear-gradient(135deg, #e74c3c 0%, #c0392b 100%)` |

---

### Icônes de résultat
| Résultat | Couleur | Dégradé |
|----------|---------|---------|
| Excellent (🏆) | Vert | `linear-gradient(135deg, #2ecc71 0%, #27ae60 100%)` |
| Bien (👍) | Bleu | `linear-gradient(135deg, #3498db 0%, #2980b9 100%)` |
| Moyen (📈) | Orange | `linear-gradient(135deg, #f39c12 0%, #e67e22 100%)` |
| Faible (📚) | Rouge | `linear-gradient(135deg, #e74c3c 0%, #c0392b 100%)` |

---

### Textes
| Élément | Couleur | Code |
|---------|---------|------|
| Texte sur fond violet | Blanc | `#ffffff` |
| Sous-titre sur fond violet | Blanc 80% | `rgba(255,255,255,0.8)` |
| Titre principal (carte blanche) | Noir | `#1f2937` |
| Texte secondaire | Gris | `#6b7280` |
| Accent violet (labels) | Violet | `#46178F` |
| Score en dégradé | Violet→Mauve | `linear-gradient(135deg, #46178f 0%, #764ba2 100%)` |

---

### Cartes blanches (quiz card, question card)
| Élément | Valeur |
|---------|--------|
| Fond | `white` |
| Border-radius | `20px` |
| Ombre | `0 10px 40px rgba(0,0,0,0.3)` |
| Barre colorée en haut | `linear-gradient(90deg, #e74c3c 0%, #3498db 25%, #f39c12 50%, #2ecc71 75%, #e74c3c 100%)` |
| Hauteur barre | `6px` (liste) / `8px` (résultats) |

---

### Chronomètre
| État | Fond | Texte |
|------|------|-------|
| Normal | `white` | `#46178F` |
| Attention (≤ 30s) | `#f39c12` | `white` |
| Danger (≤ 10s) | `#e74c3c` | `white` |

---

### Messages flash (alertes)
| Type | Fond | Bordure gauche |
|------|------|----------------|
| Warning (⚠️) | `#fffbeb` | `#f59e0b` |
| Error (❌) | `#fef2f2` | `#ef4444` |
| Success (✅) | `#f0fdf4` | `#10b981` |

---

### IA / Tuteur
| Élément | Couleur | Dégradé |
|---------|---------|---------|
| Bouton flottant IA | Violet clair | `linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)` |
| Bordure carte IA | Violet clair | `#8b5cf6` |
| Texte IA | Violet clair | `#8b5cf6` |
| Fond section IA | Violet 5% | `rgba(139,92,246,0.05)` |

---

## 3. FICHIER CSS COMPLET POUR JAVAFX

```css
/* ════════════════════════════════════════════════
   quiz-styles.css — Palette complète JavaFX
   ════════════════════════════════════════════════ */

/* ── Fond principal ── */
.quiz-root {
    -fx-background-color: #46178F;
}

/* ── Options de réponse ── */
.option-rouge {
    -fx-background-color: linear-gradient(to bottom right, #e74c3c, #c0392b);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 15;
    -fx-font-size: 14px;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);
}
.option-bleu {
    -fx-background-color: linear-gradient(to bottom right, #3498db, #2980b9);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 15;
    -fx-font-size: 14px;
    -fx-cursor: hand;
}
.option-orange {
    -fx-background-color: linear-gradient(to bottom right, #f39c12, #e67e22);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 15;
    -fx-font-size: 14px;
    -fx-cursor: hand;
}
.option-vert {
    -fx-background-color: linear-gradient(to bottom right, #2ecc71, #27ae60);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-background-radius: 15;
    -fx-font-size: 14px;
    -fx-cursor: hand;
}

/* Option sélectionnée */
.option-selected {
    -fx-border-color: white;
    -fx-border-width: 4;
    -fx-border-radius: 15;
    -fx-scale-x: 1.05;
    -fx-scale-y: 1.05;
    -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.4), 15, 0, 0, 0);
}

/* ── Carte question (fond blanc) ── */
.question-card {
    -fx-background-color: white;
    -fx-background-radius: 20;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 10);
    -fx-padding: 40 60 40 60;
}
.question-text {
    -fx-font-size: 24px;
    -fx-font-weight: bold;
    -fx-text-fill: #1f2937;
    -fx-wrap-text: true;
    -fx-text-alignment: center;
}
.question-number {
    -fx-background-color: white;
    -fx-text-fill: #46178F;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-background-radius: 50;
    -fx-padding: 8 25 8 25;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 3);
}
.question-points {
    -fx-text-fill: white;
    -fx-font-size: 16px;
    -fx-font-weight: 600;
}

/* ── Chronomètre ── */
.timer-normal {
    -fx-background-color: white;
    -fx-background-radius: 50;
    -fx-text-fill: #46178F;
    -fx-font-weight: bold;
    -fx-font-size: 20px;
    -fx-padding: 10 25 10 25;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);
}
.timer-warning {
    -fx-background-color: #f39c12;
    -fx-text-fill: white;
    -fx-background-radius: 50;
    -fx-font-weight: bold;
    -fx-font-size: 20px;
    -fx-padding: 10 25 10 25;
}
.timer-danger {
    -fx-background-color: #e74c3c;
    -fx-text-fill: white;
    -fx-background-radius: 50;
    -fx-font-weight: bold;
    -fx-font-size: 20px;
    -fx-padding: 10 25 10 25;
}

/* ── Boutons ── */
.btn-commencer {
    -fx-background-color: linear-gradient(to bottom right, #2ecc71, #27ae60);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-background-radius: 50;
    -fx-padding: 15 35 15 35;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(46,204,113,0.4), 15, 0, 0, 5);
}
.btn-soumettre {
    -fx-background-color: linear-gradient(to bottom right, #2ecc71, #27ae60);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-background-radius: 50;
    -fx-padding: 15 35 15 35;
    -fx-cursor: hand;
}
.btn-refaire {
    -fx-background-color: linear-gradient(to bottom right, #3498db, #2980b9);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-background-radius: 50;
    -fx-padding: 15 35 15 35;
    -fx-cursor: hand;
}
.btn-retour {
    -fx-background-color: white;
    -fx-text-fill: #46178F;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-background-radius: 50;
    -fx-border-color: #46178F;
    -fx-border-width: 3;
    -fx-border-radius: 50;
    -fx-padding: 15 35 15 35;
    -fx-cursor: hand;
}
.btn-accueil {
    -fx-background-color: linear-gradient(to bottom right, #2ecc71, #27ae60);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-background-radius: 50;
    -fx-padding: 15 35 15 35;
    -fx-cursor: hand;
}

/* ── Barre de progression ── */
.progress-bar .track {
    -fx-background-color: rgba(255,255,255,0.2);
    -fx-background-radius: 10;
}
.progress-bar .bar {
    -fx-background-color: linear-gradient(to right, #2ecc71, #27ae60);
    -fx-background-radius: 10;
}

/* ── Textes généraux ── */
.text-blanc       { -fx-text-fill: white; }
.text-blanc-80    { -fx-text-fill: rgba(255,255,255,0.8); }
.text-titre       { -fx-text-fill: #1f2937; -fx-font-weight: bold; }
.text-secondaire  { -fx-text-fill: #6b7280; }
.text-violet      { -fx-text-fill: #46178F; }

/* ── Badges performance résultats ── */
.badge-excellent {
    -fx-background-color: linear-gradient(to right, #2ecc71, #27ae60);
    -fx-text-fill: white; -fx-background-radius: 50; -fx-padding: 12 30 12 30;
}
.badge-good {
    -fx-background-color: linear-gradient(to right, #3498db, #2980b9);
    -fx-text-fill: white; -fx-background-radius: 50; -fx-padding: 12 30 12 30;
}
.badge-average {
    -fx-background-color: linear-gradient(to right, #f39c12, #e67e22);
    -fx-text-fill: white; -fx-background-radius: 50; -fx-padding: 12 30 12 30;
}
.badge-poor {
    -fx-background-color: linear-gradient(to right, #e74c3c, #c0392b);
    -fx-text-fill: white; -fx-background-radius: 50; -fx-padding: 12 30 12 30;
}
```

---

## 4. RÉSUMÉ RAPIDE — Les codes à retenir

```
FOND VIOLET          →  #46178F
BARRES DIAGONALES    →  repeating-linear-gradient(45deg, transparent 35px, rgba(255,255,255,.05) 70px)

ROUGE  (option 1)    →  #e74c3c → #c0392b
BLEU   (option 2)    →  #3498db → #2980b9
ORANGE (option 3)    →  #f39c12 → #e67e22
VERT   (option 4)    →  #2ecc71 → #27ae60

VIOLET ACCENT        →  #46178F
VIOLET CLAIR (IA)    →  #8b5cf6 → #7c3aed
VIOLET DÉGRADÉ       →  #46178f → #764ba2

TEXTE BLANC          →  #ffffff
TEXTE GRIS           →  #6b7280
TEXTE SOMBRE         →  #1f2937
```
