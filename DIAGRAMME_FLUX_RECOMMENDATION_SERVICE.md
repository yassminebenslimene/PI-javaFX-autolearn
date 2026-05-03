# DIAGRAMME DE FLUX - RecommendationService.java

## 🔄 FLUX COMPLET D'EXÉCUTION

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         UTILISATEUR CONNECTÉ                                │
│                            (userId = 42)                                    │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    buildUserProfile(userId)                                 │
│                                                                             │
│  Construit un profil utilisateur détaillé basé sur les données BD          │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
                ▼                ▼                ▼
        ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
        │  Requête     │  │  Requête     │  │  Requête     │
        │  SQL #1      │  │  SQL #2      │  │  SQL #3      │
        │              │  │              │  │              │
        │ Participations│  │ Participations│  │ Total        │
        │ avec         │  │ sans         │  │ participations│
        │ feedbacks    │  │ feedbacks    │  │              │
        └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
               │                 │                 │
               │ Résultat :      │ Résultat :      │ Résultat :
               │ - Hackathon     │ - Workshop      │ - 5
               │   4.5/5         │   (0/5)         │
               │ - Conference    │                 │
               │   3.8/5         │                 │
               │                 │                 │
               └────────────────┬┴────────────────┘
                                │
                                ▼
                    ┌──────────────────────────┐
                    │  Requête SQL #4          │
                    │                          │
                    │  Événements participés   │
                    │  (IDs à exclure)         │
                    │                          │
                    │  Résultat : [1, 5, 8]   │
                    └──────────────┬───────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────────────┐
                    │  UserProfile créé :                  │
                    │                                      │
                    │  userId: 42                          │
                    │  totalParticipations: 5              │
                    │  typePreferences: [                  │
                    │    {                                 │
                    │      type: "Hackathon",              │
                    │      participationCount: 3,          │
                    │      averageRating: 4.5,             │
                    │      feedback: "Très intéressant..." │
                    │    },                                │
                    │    {                                 │
                    │      type: "Conference",             │
                    │      participationCount: 2,          │
                    │      averageRating: 3.8,             │
                    │      feedback: "Bon contenu..."      │
                    │    }                                 │
                    │  ]                                   │
                    │  participatedEventIds: [1, 5, 8]     │
                    └──────────────┬───────────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
                    ▼                             ▼
    ┌──────────────────────────────┐  ┌──────────────────────────────┐
    │ generateEventRecommendations │  │ generateCourseRecommendations│
    │        (profile, 5)          │  │        (profile, 5)         │
    └──────────────┬───────────────┘  └──────────────┬───────────────┘
                   │                                 │
                   ▼                                 ▼
    ┌──────────────────────────────┐  ┌──────────────────────────────┐
    │ buildRecommendationPrompt()  │  │buildCourseRecommendationPr()│
    │                              │  │                              │
    │ Crée un prompt avec :        │  │ Crée un prompt avec :        │
    │ - Total participations       │  │ - Types d'événements préférés│
    │ - Historique par type        │  │ - Notes moyennes             │
    │ - Notes moyennes             │  │                              │
    │ - Commentaires               │  │ Prompt :                     │
    │                              │  │ "Profil utilisateur :        │
    │ Prompt :                     │  │  - Types préférés :          │
    │ "Profil utilisateur :        │  │    Hackathon (4.5/5),        │
    │  - Total participations : 5  │  │    Conference (3.8/5)        │
    │  - Historique par type :     │  │                              │
    │    * Hackathon : 3 part.,    │  │  Recommande les 3 matières   │
    │      4.5/5 moyenne           │  │  les plus pertinentes"       │
    │    * Conference : 2 part.,   │  │                              │
    │      3.8/5 moyenne           │  │                              │
    │                              │  │                              │
    │  Recommande les 3 types      │  │                              │
    │  d'événements les plus       │  │                              │
    │  pertinents"                 │  │                              │
    └──────────────┬───────────────┘  └──────────────┬───────────────┘
                   │                                 │
                   ▼                                 ▼
    ┌──────────────────────────────┐  ┌──────────────────────────────┐
    │  groqService.ask(prompt)     │  │  groqService.ask(prompt)     │
    │                              │  │                              │
    │  Appel à l'IA Groq           │  │  Appel à l'IA Groq           │
    │  avec le profil utilisateur  │  │  avec le profil utilisateur  │
    └──────────────┬───────────────┘  └──────────────┬───────────────┘
                   │                                 │
                   ▼                                 ▼
    ┌──────────────────────────────┐  ┌──────────────────────────────┐
    │  Réponse IA (JSON) :         │  │  Réponse IA (JSON) :         │
    │                              │  │                              │
    │  ["Hackathon",               │  │  ["Informatique",            │
    │   "Workshop",                │  │   "Développement",           │
    │   "Conference"]              │  │   "Gestion"]                 │
    └──────────────┬───────────────┘  └──────────────┬───────────────┘
                   │                                 │
                   ▼                                 ▼
    ┌──────────────────────────────┐  ┌──────────────────────────────┐
    │ parseIARecommendations()     │  │ parseCourseRecommendations() │
    │                              │  │                              │
    │ Parse la réponse JSON        │  │ Parse la réponse JSON        │
    │ Extrait les types            │  │ Extrait les matières         │
    │                              │  │                              │
    │ Résultat :                   │  │ Résultat :                   │
    │ ["Hackathon",                │  │ ["Informatique",             │
    │  "Workshop",                 │  │  "Développement",            │
    │  "Conference"]               │  │  "Gestion"]                  │
    └──────────────┬───────────────┘  └──────────────┬───────────────┘
                   │                                 │
                   ▼                                 ▼
    ┌──────────────────────────────┐  ┌──────────────────────────────┐
    │ Récupère événements futurs   │  │ Récupère tous les cours      │
    │                              │  │                              │
    │ evenementService.getAll()    │  │ coursService.getAll()        │
    │ .filter(!isCanceled)         │  │                              │
    │ .filter(dateDebut > now)     │  │ Résultat :                   │
    │ .filter(!participatedIds)    │  │ [Cours1, Cours2, Cours3, ...]│
    │                              │  │                              │
    │ Résultat :                   │  │                              │
    │ [Ev20, Ev21, Ev22, Ev23, ...]│  │                              │
    └──────────────┬───────────────┘  └──────────────┬───────────────┘
                   │                                 │
                   ▼                                 ▼
    ┌──────────────────────────────┐  ┌──────────────────────────────┐
    │ Tri par pertinence           │  │ Tri par pertinence           │
    │                              │  │                              │
    │ for (String type :           │  │ for (String subject :        │
    │      recommendedTypes) {     │  │      recommendedSubjects) {  │
    │   filter(ev.type == type)    │  │   filter(c.matiere contains  │
    │   add to recommendations     │  │          subject)            │
    │ }                            │  │   add to recommendations     │
    │                              │  │ }                            │
    │ Résultat (trié) :            │  │                              │
    │ 1. Ev20 (Hackathon)          │  │ Résultat (trié) :            │
    │ 2. Ev21 (Workshop)           │  │ 1. Cours1 (Informatique)     │
    │ 3. Ev22 (Conference)         │  │ 2. Cours2 (Développement)    │
    │ 4. Ev23 (Hackathon)          │  │ 3. Cours3 (Gestion)          │
    │ 5. Ev24 (Workshop)           │  │ 4. Cours4 (Informatique)     │
    │                              │  │ 5. Cours5 (Développement)    │
    └──────────────┬───────────────┘  └──────────────┬───────────────┘
                   │                                 │
                   └────────────────┬────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────────┐
                    │  RECOMMANDATIONS FINALES          │
                    │                                   │
                    │  Événements recommandés :         │
                    │  1. Hackathon 2024 (2024-05-15)   │
                    │  2. Workshop Python (2024-05-20)  │
                    │  3. Conference IA (2024-05-25)    │
                    │  4. Hackathon Web (2024-06-01)    │
                    │  5. Workshop React (2024-06-05)   │
                    │                                   │
                    │  Cours recommandés :              │
                    │  1. Python Avancé (Informatique)  │
                    │  2. IA et ML (Développement)      │
                    │  3. Gestion de Projet (Gestion)   │
                    │  4. Web Dev (Informatique)        │
                    │  5. DevOps (Développement)        │
                    │                                   │
                    │  Affichage à l'utilisateur        │
                    └───────────────────────────────────┘
```

---

## 🗄️ STRUCTURE DE LA BASE DE DONNÉES

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BASE DE DONNÉES                                     │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐      ┌──────────────────────┐      ┌──────────────────────┐
│   participation      │      │  equipe_etudiant     │      │    evenement         │
├──────────────────────┤      ├──────────────────────┤      ├──────────────────────┤
│ id (PK)              │      │ equipe_id (FK)       │      │ id (PK)              │
│ equipe_id (FK) ──────┼──────┤ etudiant_id (FK) ────┼──────┤ titre                │
│ evenement_id (FK) ───┼──────┤                      │      │ type                 │
│ statut               │      │                      │      │ date_debut           │
│ feedbacks (JSON) ◄───┼──────┤                      │      │ date_fin             │
│ table_numero         │      │                      │      │ is_canceled          │
└──────────────────────┘      └──────────────────────┘      │ lieu                 │
                                                             │ description          │
                                                             └──────────────────────┘

Feedbacks JSON Format :
{
  "rating_global": 4.5,
  "comment": "Très intéressant et bien organisé",
  "created_at": "2024-04-27T10:30:00"
}

┌──────────────────────┐
│      cours           │
├──────────────────────┤
│ id (PK)              │
│ titre                │
│ matiere              │
│ description          │
│ niveau               │
│ duree                │
└──────────────────────┘
```

---

## 🔗 RELATIONS ENTRE LES TABLES

```
                    ┌─────────────────────────────────────┐
                    │         etudiant (user)             │
                    │                                     │
                    │ id = 42                             │
                    └────────────────┬────────────────────┘
                                     │
                                     │ 1:N
                                     │
                    ┌────────────────▼────────────────┐
                    │      equipe_etudiant            │
                    │                                 │
                    │ etudiant_id = 42                │
                    │ equipe_id = 5                   │
                    └────────────────┬────────────────┘
                                     │
                                     │ 1:N
                                     │
                    ┌────────────────▼────────────────┐
                    │       participation             │
                    │                                 │
                    │ equipe_id = 5                   │
                    │ evenement_id = 20               │
                    │ feedbacks = {...}               │
                    └────────────────┬────────────────┘
                                     │
                                     │ N:1
                                     │
                    ┌────────────────▼────────────────┐
                    │       evenement                 │
                    │                                 │
                    │ id = 20                         │
                    │ type = "Hackathon"              │
                    │ date_debut = 2024-04-15         │
                    └─────────────────────────────────┘
```

---

## 📊 EXEMPLE DE DONNÉES

### Données SQL extraites pour userId = 42

```
Requête SQL #1 - Participations avec feedbacks :
┌──────────┬──────────────┬────────────┬──────────────────────────────────┐
│ type     │ nb_particip. │ avg_rating │ comments                         │
├──────────┼──────────────┼────────────┼──────────────────────────────────┤
│ Hackathon│ 3            │ 4.5        │ Très intéressant | Bien organisé │
│ Conference│ 2           │ 3.8        │ Bon contenu | Pourrait être plus │
└──────────┴──────────────┴────────────┴──────────────────────────────────┘

Requête SQL #3 - Total participations :
┌───────┐
│ total │
├───────┤
│ 5     │
└───────┘

Requête SQL #4 - Événements participés :
┌──────────────┐
│ evenement_id │
├──────────────┤
│ 1            │
│ 5            │
│ 8            │
│ 12           │
│ 15           │
└──────────────┘
```

### Événements futurs disponibles

```
┌────┬──────────────────┬──────────┬────────────────┐
│ id │ titre            │ type     │ date_debut     │
├────┼──────────────────┼──────────┼────────────────┤
│ 20 │ Hackathon 2024   │ Hackathon│ 2024-05-15     │
│ 21 │ Workshop Python  │ Workshop │ 2024-05-20     │
│ 22 │ Conference IA    │ Conference│ 2024-05-25    │
│ 23 │ Hackathon Web    │ Hackathon│ 2024-06-01     │
│ 24 │ Workshop React   │ Workshop │ 2024-06-05     │
└────┴──────────────────┴──────────┴────────────────┘
```

### Recommandations finales (triées par pertinence)

```
Ordre de pertinence (types recommandés par l'IA) :
1. Hackathon (note moyenne 4.5/5)
2. Workshop (note moyenne 0/5, mais recommandé par l'IA)
3. Conference (note moyenne 3.8/5)

Événements recommandés (triés) :
┌────┬──────────────────┬──────────┬────────────────┐
│ id │ titre            │ type     │ date_debut     │
├────┼──────────────────┼──────────┼────────────────┤
│ 20 │ Hackathon 2024   │ Hackathon│ 2024-05-15     │ ← Type #1
│ 21 │ Workshop Python  │ Workshop │ 2024-05-20     │ ← Type #2
│ 22 │ Conference IA    │ Conference│ 2024-05-25    │ ← Type #3
│ 23 │ Hackathon Web    │ Hackathon│ 2024-06-01     │ ← Type #1 (2ème)
│ 24 │ Workshop React   │ Workshop │ 2024-06-05     │ ← Type #2 (2ème)
└────┴──────────────────┴──────────┴────────────────┘
```

---

## 🎯 RÉSUMÉ VISUEL

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  UTILISATEUR CONNECTÉ (userId = 42)                                        │
│                                                                             │
│  ↓                                                                          │
│                                                                             │
│  REQUÊTES SQL COMPLEXES                                                    │
│  ├─ Participations avec feedbacks (notes moyennes par type)                │
│  ├─ Participations sans feedbacks (types supplémentaires)                  │
│  ├─ Total participations                                                   │
│  └─ Événements participés (IDs à exclure)                                  │
│                                                                             │
│  ↓                                                                          │
│                                                                             │
│  PROFIL UTILISATEUR CRÉÉ                                                   │
│  ├─ userId: 42                                                             │
│  ├─ totalParticipations: 5                                                 │
│  ├─ typePreferences: [Hackathon (4.5/5), Conference (3.8/5), ...]         │
│  └─ participatedEventIds: [1, 5, 8, 12, 15]                               │
│                                                                             │
│  ↓                                                                          │
│                                                                             │
│  INTÉGRATION IA                                                            │
│  ├─ Prompt construit avec le profil utilisateur                            │
│  ├─ Appel à groqService.ask()                                              │
│  ├─ IA analyse les préférences et génère des recommandations               │
│  └─ Réponse IA : ["Hackathon", "Workshop", "Conference"]                  │
│                                                                             │
│  ↓                                                                          │
│                                                                             │
│  RECOMMANDATIONS FINALES                                                   │
│  ├─ Événements recommandés (triés par pertinence)                          │
│  │  1. Hackathon 2024                                                      │
│  │  2. Workshop Python                                                     │
│  │  3. Conference IA                                                       │
│  │  4. Hackathon Web                                                       │
│  │  5. Workshop React                                                      │
│  │                                                                         │
│  └─ Cours recommandés (triés par pertinence)                               │
│     1. Python Avancé (Informatique)                                        │
│     2. IA et Machine Learning (Développement)                              │
│     3. Gestion de Projet (Gestion)                                         │
│     4. Web Development (Informatique)                                      │
│     5. DevOps (Développement)                                              │
│                                                                             │
│  ↓                                                                          │
│                                                                             │
│  AFFICHAGE À L'UTILISATEUR                                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ✅ CONCLUSION

Le RecommendationService.java implémente un flux complet et professionnel :

1. **Récupération des données** : Requêtes SQL complexes
2. **Construction du profil** : Analyse des participations et feedbacks
3. **Intégration IA** : Utilisation de Groq pour générer des recommandations
4. **Tri et filtrage** : Recommandations triées par pertinence
5. **Affichage** : Recommandations d'événements et de cours

C'est une implémentation **COMPLÈTE**, **ROBUSTE** et **PROFESSIONNELLE**.

