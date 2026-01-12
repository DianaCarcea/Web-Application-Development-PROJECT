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
    // User-Agent Wikimedia
    private static final String USER_AGENT = "Java:ArtCatalogApp/1.0 (contact@example.com)";
    private static final String WIKIDATA_API_URL = "https://www.wikidata.org/w/api.php";

    // --- METODA 1: Wikipedia PageImages ---
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

    // --- METODA 2: Commons Search ---
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

            String rawTitle = searchResults.get(0).path("title").asText();

            String cleanFilename = rawTitle.replace("File:", "").trim();

            String encodedFilename = URLEncoder.encode(cleanFilename, StandardCharsets.UTF_8);

            String redirectUrl = "https://commons.wikimedia.org/w/index.php?title=Special:Redirect/file/" + encodedFilename + "&width=800";

            System.out.println("link redirect: " + redirectUrl);
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


    private static String cleanQuery(String query) {
        if (query == null) return "";

        if (query.contains(" - ")) {
            query = query.split(" - ")[0];
        }
        if (query.contains(",")) {
            query = query.split(",")[0];
        }
        if (query.contains("(")) {
            query = query.split("\\(")[0];
        }

        return query.trim();
    }

    public static String searchWikidataId(String query) {
        try {
            if (query == null || query.trim().isEmpty()) return null;

            // STEP 1: Smart Cleaning
            String cleanedQuery = cleanQuery(query);

            String encodedQuery = URLEncoder.encode(cleanedQuery, StandardCharsets.UTF_8);

            // STEP 2: Construct API URL
            String urlString = WIKIDATA_API_URL + "?action=wbsearchentities"
                    + "&search=" + encodedQuery
                    + "&language=en"
                    + "&limit=1" // Take the first result
                    + "&format=json";

            JsonNode rootNode = fetchJson(urlString);

            if (rootNode != null) {
                JsonNode searchArray = rootNode.path("search");
                if (searchArray.isArray() && searchArray.size() > 0) {
                    // Return the ID of the first match
                    return searchArray.get(0).path("id").asText();
                }
            }

            if (!cleanedQuery.equals(query)) {
                return searchWikidataId(query);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String searchWikidataUrl(String query) {
        String id = searchWikidataId(query);
        if (id != null) {
            return "http://www.wikidata.org/entity/" + id;
        }
        return null;
    }

    // Test rapid
    public static void main(String[] args) {
        System.out.println(searchWikidataId("Muzeul Național de Artă al României"));

        System.out.println(searchWikidataUrl("Muzeul Național de Artă al României - BUCUREȘTI"));

        System.out.println(searchWikidataUrl("Louvre Museum, Paris"));
    }

}