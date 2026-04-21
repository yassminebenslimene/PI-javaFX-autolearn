# OAuth Configuration Guide for AutoLearn

This guide explains how to configure Google, Facebook, and GitHub OAuth for the AutoLearn application.

## Overview

The OAuth buttons are now functional on both **Login** and **Register** pages:
- **Google** (red "G" button)
- **Facebook** (blue "f" button)  
- **GitHub** (dark icon button)

## How It Works

### Login Flow
1. User clicks an OAuth button (Google/Facebook/GitHub)
2. Browser opens to the provider's authorization page
3. User authorizes the app
4. Provider redirects back to localhost with authorization code
5. App exchanges code for access token
6. App fetches user info (email, name)
7. If user exists in database → logs in automatically
8. If user doesn't exist → shows error "Please register first"

### Register Flow
1. User clicks an OAuth button
2. Same authorization flow as login
3. App fetches user info from provider
4. **Pre-fills the registration form** with:
   - Email
   - First name (Prénom)
   - Last name (Nom)
5. User completes remaining fields (password, role, niveau)
6. User clicks "Créer mon compte" to finalize registration

---

## Configuration Steps

### 1. Google OAuth Setup

#### Create Google OAuth Credentials
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable **Google+ API**
4. Go to **Credentials** → **Create Credentials** → **OAuth 2.0 Client ID**
5. Application type: **Web application**
6. Authorized redirect URIs: `http://localhost:8080/callback`
7. Copy your **Client ID** and **Client Secret**

#### Update Code
Edit `src/main/java/tn/esprit/services/GoogleOAuthService.java`:

```java
private static final String CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com";
private static final String CLIENT_SECRET = "YOUR_GOOGLE_CLIENT_SECRET";
```

---

### 2. Facebook OAuth Setup

#### Create Facebook App
1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create a new app → **Consumer** type
3. Add **Facebook Login** product
4. Settings → Basic:
   - Copy **App ID** and **App Secret**
5. Facebook Login → Settings:
   - Valid OAuth Redirect URIs: `http://localhost:8081/callback`
6. Make app **Live** (toggle in top bar)

#### Update Code
Edit `src/main/java/tn/esprit/services/FacebookOAuthService.java`:

```java
private static final String APP_ID = "YOUR_FACEBOOK_APP_ID";
private static final String APP_SECRET = "YOUR_FACEBOOK_APP_SECRET";
```

---

### 3. GitHub OAuth Setup

#### Create GitHub OAuth App
1. Go to [GitHub Settings → Developer settings](https://github.com/settings/developers)
2. **OAuth Apps** → **New OAuth App**
3. Fill in:
   - Application name: `AutoLearn`
   - Homepage URL: `http://localhost:8080`
   - Authorization callback URL: `http://localhost:8082/callback`
4. Click **Register application**
5. Copy **Client ID**
6. Generate a new **Client Secret** and copy it

#### Update Code
Edit `src/main/java/tn/esprit/services/GitHubOAuthService.java`:

**Note**: GitHub credentials are already configured with your provided values:
```java
private static final String CLIENT_ID = "Ov23liaGRyNv6Q340ANg";
private static final String CLIENT_SECRET = "83cf8926b7e97be668ec646ef08ad7d226c81684";
```

If you need to change them, update these values.

---

## Testing

### Test Login with OAuth
1. Run the app: `mvn javafx:run`
2. Go to Login page
3. Click Google/Facebook/GitHub button
4. Authorize in browser
5. Should log in automatically if account exists

### Test Register with OAuth
1. Go to Register page
2. Click Google/Facebook/GitHub button
3. Authorize in browser
4. Form should pre-fill with your email and name
5. Complete password, role, and niveau
6. Click "Créer mon compte"

---

## Troubleshooting

### "No email received from provider"
- **Google**: Make sure you requested `email` scope
- **Facebook**: Check that `email` permission is approved
- **GitHub**: User must have a public email or primary email set

### "User denied access"
- User clicked "Cancel" on authorization page
- Try again and click "Allow"

### Browser doesn't open
- Check if `Desktop.isDesktopSupported()` returns true
- Manually copy the URL from console and paste in browser

### Port already in use
- Each provider uses a different port:
  - Google: 8080
  - Facebook: 8081
  - GitHub: 8082
- Make sure these ports are not blocked by firewall

---

## Security Notes

⚠️ **IMPORTANT**: 
- Never commit OAuth secrets to Git
- Add `*OAuthService.java` to `.gitignore` after configuration
- Use environment variables in production:
  ```java
  private static final String CLIENT_ID = System.getenv("GOOGLE_CLIENT_ID");
  ```

---

## Files Modified

### New Files Created
- `src/main/java/tn/esprit/services/GoogleOAuthService.java`
- `src/main/java/tn/esprit/services/FacebookOAuthService.java`
- `src/main/java/tn/esprit/services/GitHubOAuthService.java`

### Modified Files
- `src/main/resources/views/auth/login.fxml` - Added clickable OAuth buttons
- `src/main/resources/views/auth/register.fxml` - Added OAuth buttons with separator
- `src/main/java/tn/esprit/controllers/LoginController.java` - Added OAuth handlers
- `src/main/java/tn/esprit/controllers/RegisterController.java` - Added OAuth pre-fill logic
- `pom.xml` - Added `org.json` dependency

---

## Next Steps

1. Configure your OAuth credentials in the service files
2. Test each provider (Google, Facebook, GitHub)
3. Verify email pre-filling works correctly
4. Test both login and register flows
5. Deploy to production with environment variables

---

**Questions?** Contact: autolearn66@gmail.com
