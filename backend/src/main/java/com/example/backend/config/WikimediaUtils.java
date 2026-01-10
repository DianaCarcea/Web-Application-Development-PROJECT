package com.example.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public class WikimediaUtils {

    private static final String WIKI_API_URL = "https://en.wikipedia.org/w/api.php";
    private static final String COMMONS_API_URL = "https://commons.wikimedia.org/w/api.php";
    // User-Agent obligatoriu pentru Wikimedia
    private static final String USER_AGENT = "Java:ArtCatalogApp/1.0 (contact@example.com)";

    // --- METODA 1: Wikipedia PageImages (De obicei returnează link bun) ---
    public static String searchWikipediaPageImage(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = WIKI_API_URL + "?action=query"
                    + "&generator=search"
                    + "&gsrsearch=" + encodedQuery
                    + "&gsrnamespace=0"
                    + "&gsrlimit=1"
                    + "&prop=pageimages"
                    + "&pithumbsize=1000"
                    + "&format=json";

            JsonNode rootNode = fetchJson(searchUrl);
            if (rootNode == null) return null;

            JsonNode pagesNode = rootNode.path("query").path("pages");
            if (pagesNode.isMissingNode() || pagesNode.isEmpty()) return null;

            Iterator<Map.Entry<String, JsonNode>> fields = pagesNode.fields();
            while (fields.hasNext()) {
                JsonNode pageNode = fields.next().getValue();
                if (pageNode.has("thumbnail")) {
                    return pageNode.path("thumbnail").path("source").asText();
                }
            }
        } catch (Exception e) {
            System.err.println("Eroare Wiki PageImages: " + e.getMessage());
        }
        return null;
    }

    // --- METODA 2: Commons Search (REPARATĂ) ---
    // Aceasta era sursa problemei. Acum returnează FORȚAT link-ul de redirect.
    public static String searchCommonsFile(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query + " filetype:jpg|png", StandardCharsets.UTF_8);

            // Căutăm doar titlul fișierului
            String searchUrl = COMMONS_API_URL + "?action=query"
                    + "&list=search"
                    + "&srsearch=" + encodedQuery
                    + "&srnamespace=6" // Namespace File
                    + "&srlimit=1"
                    + "&format=json";

            JsonNode rootNode = fetchJson(searchUrl);
            if (rootNode == null) return null;

            JsonNode searchResults = rootNode.path("query").path("search");
            if (!searchResults.isArray() || searchResults.isEmpty()) return null;

            // 1. Luăm titlul exact (ex: "File:Interchange-colour-img_0526.jpg")
            String rawTitle = searchResults.get(0).path("title").asText();

            // 2. Eliminăm "File:" din față
            String cleanFilename = rawTitle.replace("File:", "").trim();

            // 3. Codificăm numele pentru URL (spațiile devin %20)
            String encodedFilename = URLEncoder.encode(cleanFilename, StandardCharsets.UTF_8);

            // 4. Returnăm URL-ul magic de Redirect
            // Browserul va primi acest link și va fi trimis automat la poza .jpg
            String redirectUrl = "https://commons.wikimedia.org/w/index.php?title=Special:Redirect/file/" + encodedFilename + "&width=800";

            System.out.println("✅ Commons Search: Generat link redirect: " + redirectUrl);
            return redirectUrl;

        } catch (Exception e) {
            System.err.println("Eroare Commons Search: " + e.getMessage());
        }
        return null;
    }



    // Helper intern pentru request HTTP
    private static JsonNode fetchJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);

        if (conn.getResponseCode() != 200) return null;

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            content.append(line);
        }
        in.close();
        conn.disconnect();

        return new ObjectMapper().readTree(content.toString());
    }
}