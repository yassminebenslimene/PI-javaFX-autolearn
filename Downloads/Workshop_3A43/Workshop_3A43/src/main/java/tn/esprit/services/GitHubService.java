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
            String query = keyword + "+language:" + language;
            String urlString = GITHUB_API_URL + "/search/repositories?q=" + query + "&sort=stars&per_page=" + maxResults;
            
            JsonObject response = makeRequest(urlString);
            JsonArray items = response.getAsJsonArray("items");
            
            for (int i = 0; i < items.size(); i++) {
                JsonObject repo = items.get(i).getAsJsonObject();
                repositories.add(new GitHubRepository(
                    repo.get("name").getAsString(),
                    repo.get("full_name").getAsString(),
                    repo.get("description").getAsString(),
                    repo.get("html_url").getAsString(),
                    repo.get("stargazers_count").getAsInt(),
                    repo.get("language").getAsString()
                ));
            }
        } catch (Exception e) {
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
            String searchQuery = query + "+language:" + language;
            String urlString = GITHUB_API_URL + "/search/code?q=" + searchQuery + "&per_page=" + maxResults;
            
            JsonObject response = makeRequest(urlString);
            JsonArray items = response.getAsJsonArray("items");
            
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
            e.printStackTrace();
        }
        return examples;
    }

    private JsonObject makeRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        
        if (apiToken != null && !apiToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "token " + apiToken);
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
