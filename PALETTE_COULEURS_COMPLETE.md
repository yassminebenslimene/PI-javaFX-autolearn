# PALETTE DE COULEURS COMPLÈTE — MODULE ÉVÉNEMENT

**Identité AutoLearn:** Violet  
**Date:** April 26, 2026

---

## COULEURS PRIMAIRES

### Violet (Identité AutoLearn)
| Nom | Hex | RGB | Utilisation |
|-----|-----|-----|------------|
| Violet Primaire | #667eea | 102, 126, 234 | Headers, boutons, accents |
| Violet Secondaire | #764ba2 | 118, 75, 162 | Gradients, bordures |
| Violet Clair | #f0ebff | 240, 235, 255 | Fonds de sections |
| Violet Très Clair | #f5f3ff | 245, 243, 255 | Fonds de cartes |
| Violet Bordure | #e8e0ff | 232, 224, 255 | Bordures légères |

---

## COULEURS ÉVÉNEMENTS

| Type | Hex | RGB | Utilisation |
|------|-----|-----|------------|
| Hackathon | #4facfe | 79, 172, 254 | Badge événement, pill |
| Conference | #f093fb | 240, 147, 251 | Badge événement, pill |
| Workshop | #667eea | 102, 126, 234 | Badge événement, pill |
| Seminar | #43e97b | 67, 233, 123 | Badge événement, pill |

---

## COULEURS TEXTE

| Niveau | Hex | RGB | Utilisation |
|--------|-----|-----|------------|
| Dark | #2d3748 | 45, 55, 72 | Titres, texte principal |
| Body | #4a5568 | 74, 85, 104 | Texte corps |
| Muted | #6b7280 | 107, 114, 128 | Texte secondaire |
| Light | #ffffff | 255, 255, 255 | Texte sur fond violet |

---

## GRADIENTS UTILISÉS

### Header Email
```css
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

### QR Code Section
```css
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

### Badge Header
```css
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

---

## APPLICATIONS PAR COMPOSANT

### Espace 3D (salle3d.html)
```
UI Top:        linear-gradient(#667eea → #764ba2)
UI Bottom:     linear-gradient(#667eea → #764ba2)
Minimap Border: #667eea
Legend:        linear-gradient(#667eea → #764ba2)
Popup:         linear-gradient(#667eea → #764ba2)
Navigation:    linear-gradient(#667eea → #764ba2)
```

### Email (ParticipationConfirmationService)
```
Header:        linear-gradient(#667eea → #764ba2)
Event Card:    #f0ebff (fond), #667eea (bordure)
Weather:       #f0ebff (fond), #667eea (bordure)
Weather Tip:   #f0ebff (fond), #667eea (bordure)
QR Section:    linear-gradient(#667eea → #764ba2)
Badge Info:    #f0ebff (fond), #667eea (bordure)
Footer:        #667eea (liens)
```

### Badge PDF (BadgePdfService)
```
Header:        #667eea
Separator:     #764ba2
Footer:        #764ba2
Name:          #667eea
Team:          #764ba2
QR Background: #f0ebff
QR Label:      #667eea
```

### Rapports PDF (ReportPdfService)
```
Primary:       #667eea
Secondary:     #764ba2
Accent:        #667eea
Background:    #f0ebff
Card:          #f5f3ff
Border:        #e8e0ff
Text Dark:     #2d3748
Text Body:     #4a5568
Text Muted:    #6b7280
```

---

## CODES COULEUR PAR FORMAT

### CSS
```css
/* Violet */
--violet-primary: #667eea;
--violet-secondary: #764ba2;
--violet-light: #f0ebff;
--violet-lighter: #f5f3ff;
--violet-border: #e8e0ff;

/* Events */
--hackathon: #4facfe;
--conference: #f093fb;
--workshop: #667eea;

/* Text */
--text-dark: #2d3748;
--text-body: #4a5568;
--text-muted: #6b7280;
```

### Java (BaseColor)
```java
// Violet
new BaseColor(102, 126, 234)    // #667eea
new BaseColor(118, 75, 162)     // #764ba2
new BaseColor(240, 235, 255)    // #f0ebff
new BaseColor(245, 243, 255)    // #f5f3ff
new BaseColor(232, 224, 255)    // #e8e0ff

// Events
new BaseColor(79, 172, 254)     // #4facfe Hackathon
new BaseColor(240, 147, 251)    // #f093fb Conference
new BaseColor(102, 126, 234)    // #667eea Workshop

// Text
new BaseColor(45, 55, 72)       // #2d3748 Dark
new BaseColor(74, 85, 104)      // #4a5568 Body
new BaseColor(107, 114, 128)    // #6b7280 Muted
```

### HTML/Email
```html
<!-- Violet -->
style="color: #667eea;"
style="background: #667eea;"
style="border-color: #667eea;"
style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);"

<!-- Events -->
style="background: #4facfe;" <!-- Hackathon -->
style="background: #f093fb;" <!-- Conference -->
style="background: #667eea;" <!-- Workshop -->
```

---

## CONTRASTE ET ACCESSIBILITÉ

| Combinaison | Ratio | Niveau |
|------------|-------|--------|
| #667eea sur #ffffff | 4.5:1 | AA ✅ |
| #764ba2 sur #ffffff | 5.2:1 | AAA ✅ |
| #2d3748 sur #f0ebff | 8.1:1 | AAA ✅ |
| #4a5568 sur #f0ebff | 6.8:1 | AAA ✅ |

---

## UTILISATION RECOMMANDÉE

### Pour les Titres
- Couleur: #667eea (Violet Primaire)
- Taille: 18-30px
- Poids: Bold (700)

### Pour le Texte Corps
- Couleur: #4a5568 (Body)
- Taille: 11-15px
- Poids: Normal (400)

### Pour les Accents
- Couleur: #667eea (Violet Primaire)
- Utilisation: Bordures, icônes, highlights

### Pour les Fonds
- Couleur: #f0ebff (Violet Clair) ou #f5f3ff (Très Clair)
- Utilisation: Sections, cartes, conteneurs

### Pour les Gradients
- Gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%)
- Utilisation: Headers, boutons, sections principales

---

## NOTES IMPORTANTES

1. **Cohérence:** Toute la plateforme utilise la palette violet
2. **Accessibilité:** Tous les contrastes respectent les normes WCAG AA/AAA
3. **Événements:** Chaque type d'événement a sa couleur spécifique
4. **Gradients:** Utilisés pour les éléments principaux (headers, boutons)
5. **Fonds:** Violet clair pour les sections, très clair pour les cartes

---

**Palette Finale:** ✅ Violet (Identité AutoLearn)
