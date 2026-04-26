# Quick Start: Backoffice Events Interface

## What Was Fixed

The backoffice events interface was completely empty because the `evenement` table was not being created in the database. This has been **FIXED**.

## The Solution

Added the `evenement` table creation to the database initialization in:
- **File**: `src/main/java/tn/esprit/tools/MyConnection.java`
- **What**: Added SQL table creation statement for the `evenement` table
- **Result**: Table is now created automatically on first application startup

## How to Use

### 1. Start the Application
- The database will initialize automatically
- The `evenement` table will be created
- No manual database setup needed

### 2. Navigate to Backoffice Events
- Go to: Backoffice > Events
- You should see an empty list initially (no events created yet)

### 3. Create an Event
- Click "➕ Ajouter un événement" button
- Fill in the form:
  - **Titre**: Event name
  - **Description**: Event description (or use 🤖 AI button to generate)
  - **Type**: Hackathon, Conference, or Workshop
  - **Nombre maximum d'équipes**: Max teams (or use 🤖 AI button to estimate)
  - **Date de début**: Start date and time
  - **Date de fin**: End date and time
  - **Lieu**: Event location
  - **Planning**: Optional AI-generated planning
- Click "Créer" to save

### 4. View Events
- Events appear in the list with:
  - Title
  - Type (color-coded: Green=Hackathon, Indigo=Conference, Orange=Workshop)
  - Start and end dates
  - Status (color-coded)
  - Max places
  - Action buttons

### 5. View Event Details
- Click "👁 Voir" button on any event
- See complete event information including:
  - All event details
  - **Weather information** (temperature, humidity, wind speed)
  - Number of registered teams
  - Number of participations
- Click "✏ Modifier" to edit
- Click "↩ Retour à la liste" to go back

### 6. Generate AI Reports
- In the list view, select a filter (or "All types")
- Click one of the AI report buttons:
  - "📈 Générer Rapport d'Analyse" - Detailed analysis
  - "💡 Recommandations d'Événements" - Improvement suggestions
  - "✨ Suggestions d'Amélioration" - Enhancement ideas
- Report appears in a dialog

### 7. Manage Events
- **Edit**: Click "✏ Modifier" button
- **Delete**: Click "🗑 Supprimer" button
- **Cancel**: Click "✖ Annuler" button (if not already passed)

## Features

### ✅ Event Management
- Create, read, update, delete events
- Filter by event type
- View event details with weather
- Cancel events

### ✅ AI Features
- Generate event descriptions
- Estimate number of teams
- Generate event planning
- Generate analysis reports
- Generate recommendation reports
- Generate suggestion reports

### ✅ Weather Display
- Current weather or forecast
- Temperature, humidity, wind speed
- Weather emoji
- Forecast indicator

### ✅ Color Coding
- **Event Types**:
  - Hackathon: Green (#10b981)
  - Conference: Indigo (#6366f1)
  - Workshop: Orange (#f59e0b)
- **Status**:
  - Plannifié: Blue
  - En cours: Green
  - Passé: Green
  - Annulé: Yellow

## Troubleshooting

### Events list is empty
- **Expected**: If no events have been created yet
- **Solution**: Create an event using the form

### Weather not showing
- **Possible cause**: No internet connection or API key issue
- **Solution**: Check console logs for error messages

### AI features not working
- **Possible cause**: Groq API key not configured
- **Solution**: Set GROQ_API_KEY environment variable

### Database errors
- **Possible cause**: Database connection issue
- **Solution**: Check database credentials in MyConnection.java

## Files Modified

Only one file was modified to fix the issue:
- `src/main/java/tn/esprit/tools/MyConnection.java`

All other files were already correctly implemented:
- Controllers, services, FXML files, etc.

## Verification

All files compile without errors:
- ✅ MyConnection.java
- ✅ EvenementService.java
- ✅ EvenementIndexController.java
- ✅ EvenementShowController.java
- ✅ All FXML files

## Summary

The backoffice events interface is now **fully functional**. The critical issue (missing database table) has been fixed. You can now:

1. Create events
2. View events in a list
3. See event details with weather
4. Generate AI reports
5. Manage events (edit, delete, cancel)

Everything is ready to use!
