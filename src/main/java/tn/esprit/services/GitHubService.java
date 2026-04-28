package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class GitHubService {
    private static final String GITHUB_API_URL = "https://api.github.com";
    private String apiToken;
    private final Gson gson;

    public GitHubService() {
        this.gson = new Gson();
        // Token optionnel - GitHub permet 60 requêtes/heure sans token
        this.apiToken = null;
    }

    public GitHubService(String apiToken) {
        this.gson = new Gson();
        this.apiToken = apiToken;
    }

    /**
     * Rechercher des repositories par langage et mot-clé
     */
    public List<GitHubRepository> searchRepositories(String language, String keyword, int maxResults) {
        List<GitHubRepository> repositories = new ArrayList<>();
        try {
            // Encoder correctement les paramètres de l'URL
            String encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8");
            String encodedLanguage = java.net.URLEncoder.encode(language, "UTF-8");
            String query = encodedKeyword + "+language:" + encodedLanguage;
            String urlString = GITHUB_API_URL + "/search/repositories?q=" + query + "&sort=stars&per_page=" + maxResults;
            
            System.out.println("Searching repositories: " + urlString);
            
            JsonObject response = makeRequest(urlString);
            
            if (!response.has("items")) {
                System.err.println("No 'items' field in response");
                return repositories;
            }
            
            JsonArray items = response.getAsJsonArray("items");
            System.out.println("Found " + items.size() + " repositories");
            
            for (int i = 0; i < items.size(); i++) {
                JsonObject repo = items.get(i).getAsJsonObject();
                String description = repo.has("description") && !repo.get("description").isJsonNull() 
                    ? repo.get("description").getAsString() 
                    : "Pas de description";
                String repoLanguage = repo.has("language") && !repo.get("language").isJsonNull()
                    ? repo.get("language").getAsString()
                    : language;
                    
                repositories.add(new GitHubRepository(
                    repo.get("name").getAsString(),
                    repo.get("full_name").getAsString(),
                    description,
                    repo.get("html_url").getAsString(),
                    repo.get("stargazers_count").getAsInt(),
                    repoLanguage
                ));
            }
        } catch (Exception e) {
            System.err.println("Error searching repositories: " + e.getMessage());
            e.printStackTrace();
        }
        return repositories;
    }

    /**
     * Récupérer le contenu d'un fichier depuis GitHub
     */
    public String getFileContent(String owner, String repo, String filePath) {
        try {
            String urlString = GITHUB_API_URL + "/repos/" + owner + "/" + repo + "/contents/" + filePath;
            JsonObject response = makeRequest(urlString);
            
            String encodedContent = response.get("content").getAsString();
            byte[] decodedBytes = Base64.getMimeDecoder().decode(encodedContent);
            return new String(decodedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la récupération du fichier";
        }
    }

    /**
     * Lister les fichiers d'un repository
     */
    public List<String> listFiles(String owner, String repo, String path) {
        List<String> files = new ArrayList<>();
        try {
            String urlString = GITHUB_API_URL + "/repos/" + owner + "/" + repo + "/contents/" + path;
            JsonArray response = makeRequest(urlString).getAsJsonArray();
            
            for (int i = 0; i < response.size(); i++) {
                JsonObject file = response.get(i).getAsJsonObject();
                if (file.get("type").getAsString().equals("file")) {
                    files.add(file.get("name").getAsString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    /**
     * Rechercher des exemples de code
     */
    public List<CodeExample> searchCode(String language, String query, int maxResults) {
        List<CodeExample> examples = new ArrayList<>();
        try {
            String searchQuery = query.replace(" ", "+") + "+language:" + language;
            String urlString = GITHUB_API_URL + "/search/code?q=" + searchQuery + "&per_page=" + maxResults;
            
            System.out.println("Searching GitHub: " + urlString);
            
            JsonObject response = makeRequest(urlString);
            
            if (!response.has("items")) {
                System.err.println("No 'items' field in response");
                return examples;
            }
            
            JsonArray items = response.getAsJsonArray("items");
            System.out.println("Found " + items.size() + " code examples");
            
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                JsonObject repository = item.getAsJsonObject("repository");
                
                examples.add(new CodeExample(
                    item.get("name").getAsString(),
                    item.get("path").getAsString(),
                    item.get("html_url").getAsString(),
                    repository.get("full_name").getAsString()
                ));
            }
        } catch (Exception e) {
            System.err.println("Error searching code: " + e.getMessage());
            e.printStackTrace();
        }
        return examples;
    }

    private JsonObject makeRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("User-Agent", "AutoLearn-JavaFX-App");
        
        if (apiToken != null && !apiToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "token " + apiToken);
        }
        
        int responseCode = conn.getResponseCode();
        System.out.println("GitHub API Response Code: " + responseCode);
        
        if (responseCode != 200) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            System.err.println("GitHub API Error: " + errorResponse.toString());
            throw new Exception("GitHub API returned code " + responseCode + ": " + errorResponse.toString());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return gson.fromJson(response.toString(), JsonObject.class);
    }

    // Classes internes pour représenter les données
    public static class GitHubRepository {
        private String name;
        private String fullName;
        private String description;
        private String url;
        private int stars;
        private String language;

        public GitHubRepository(String name, String fullName, String description, String url, int stars, String language) {
            this.name = name;
            this.fullName = fullName;
            this.description = description;
            this.url = url;
            this.stars = stars;
            this.language = language;
        }

        public String getName() { return name; }
        public String getFullName() { return fullName; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
        public int getStars() { return stars; }
        public String getLanguage() { return language; }
    }

    public static class CodeExample {
        private String fileName;
        private String path;
        private String url;
        private String repository;

        public CodeExample(String fileName, String path, String url, String repository) {
            this.fileName = fileName;
            this.path = path;
            this.url = url;
            this.repository = repository;
        }

        public String getFileName() { return fileName; }
        public String getPath() { return path; }
        public String getUrl() { return url; }
        public String getRepository() { return repository; }
    }
}
