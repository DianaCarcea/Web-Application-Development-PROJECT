package com.example.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GettyCategoryFetcher {

    private static final String WIKIDATA_SPARQL = "https://query.wikidata.org/sparql";

    static class CategoryData {
        String labelRo;
        String labelEn;
        CategoryData(String ro, String en) {
            this.labelRo = ro;
            this.labelEn = en;
        }
    }

    public static void main(String[] args) throws Exception {
        Set<String> categoriesRo = extractCategoriesFromTTL("output.ttl"); // română
        Set<String> categoriesEn = extractCategoriesFromTTL("artworks_arp.ttl"); // engleză
        String outputTTL = "getty-categories.ttl";

        // Folosim Map cu cheie codul AAT pentru unicitate
        Map<String, CategoryData> categoriesByAat = new HashMap<>();

        // Procesează categoriile în română
        for (String ro : categoriesRo) {
            String roLower = ro.toLowerCase();
            String en = getEnglishLabel(roLower);
            if (en == null) {
                System.out.printf("⚠ Nu am găsit engleză pentru: %s%n", ro);
                continue;
            }
            String aatCode = queryGettyCode(en);
            if (aatCode != null) {
                categoriesByAat.putIfAbsent(aatCode, new CategoryData(ro.toLowerCase(), en));
                System.out.printf("✔ %s → %s (%s)%n", ro, en, aatCode);
            } else {
                System.out.printf("✖ Nu am găsit cod Getty pentru: %s (%s)%n", ro, en);
            }
        }

        // Procesează categoriile în engleză
        for (String en : categoriesEn) {
            String ro = getRomanianLabel(en);
            String aatCode = queryGettyCode(en);
            if (aatCode != null) {
                categoriesByAat.putIfAbsent(aatCode, new CategoryData(
                        ro != null ? ro : en, // dacă nu găsim română, folosim engleza
                        en
                ));
                System.out.printf("✔ %s → %s (%s)%n", en, ro != null ? ro : en, aatCode);
            } else {
                System.out.printf("✖ Nu am găsit cod Getty pentru: %s%n", en);
            }
        }

        // Scriere TTL final
        try (PrintWriter out = new PrintWriter(new FileOutputStream(outputTTL))) {
            out.println("@prefix aat:  <http://vocab.getty.edu/aat/> .");
            out.println("@prefix arp:  <http://arp.ro/schema#> .");
            out.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
            out.println("@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n");

            for (Map.Entry<String, CategoryData> entry : categoriesByAat.entrySet()) {
                String aatCode = entry.getKey();
                CategoryData data = entry.getValue();
                out.printf("<http://arp.ro/category/%s>\n", aatCode);
                out.printf("    rdfs:label \"%s\"@ro;\n", data.labelRo);
                out.printf("    rdfs:label \"%s\"@en;\n", data.labelEn);
                out.printf("    skos:exactMatch aat:%s .\n\n", aatCode);
            }
        }

        System.out.println("✔ TTL final pentru categorii generat cu succes!");
    }

    private static Set<String> extractCategoriesFromTTL(String filename) throws IOException {
        Set<String> categories = new HashSet<>();
        Pattern pattern = Pattern.compile("arp:category\\s+\"([^\"]+)\"");
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    categories.add(m.group(1));
                }
            }
        }
        return categories;
    }

    private static String toUriSafe(String input) {
        return input.toLowerCase()
                .replace("ă", "a")
                .replace("â", "a")
                .replace("î", "i")
                .replace("ș", "s")
                .replace("ț", "t")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }

    private static String getEnglishLabel(String roTermLower) throws Exception {
        String sparql = "SELECT ?enLabel WHERE {\n" +
                "  ?item rdfs:label \"" + roTermLower + "\"@ro .\n" +
                "  ?item rdfs:label ?enLabel .\n" +
                "  FILTER(LANG(?enLabel)=\"en\")\n" +
                "} LIMIT 1";

        HttpClient client = HttpClient.newHttpClient();
        String url = WIKIDATA_SPARQL + "?query=" + URLEncoder.encode(sparql, StandardCharsets.UTF_8) + "&format=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/sparql-results+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body().trim();
        if (!body.startsWith("{")) {
            System.out.println("⚠ Răspuns neașteptat de la Wikidata: " + body);
            return null;
        }

        JsonNode bindings = new ObjectMapper().readTree(body)
                .path("results").path("bindings");

        if (bindings.isArray() && bindings.size() > 0) {
            return bindings.get(0).path("enLabel").path("value").asText();
        }
        return null;
    }

    private static String getRomanianLabel(String enTerm) throws Exception {
        String sparql = "SELECT ?roLabel WHERE {\n" +
                "  ?item rdfs:label \"" + enTerm + "\"@en .\n" +
                "  ?item rdfs:label ?roLabel .\n" +
                "  FILTER(LANG(?roLabel)=\"ro\")\n" +
                "} LIMIT 1";

        HttpClient client = HttpClient.newHttpClient();
        String url = WIKIDATA_SPARQL + "?query=" + URLEncoder.encode(sparql, StandardCharsets.UTF_8) + "&format=json";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/sparql-results+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode bindings = new ObjectMapper().readTree(response.body())
                .path("results").path("bindings");

        if (bindings.isArray() && bindings.size() > 0) {
            return bindings.get(0).path("roLabel").path("value").asText();
        }
        return null;
    }

    private static String queryGettyCode(String enTerm) throws Exception {
        String sparql =
                "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "SELECT ?aatID WHERE {\n" +
                        "  ?item wdt:P1014 ?aatID.\n" +
                        "  ?item rdfs:label \"" + enTerm + "\"@en.\n" +
                        "} LIMIT 1";

        HttpClient client = HttpClient.newHttpClient();
        String url = WIKIDATA_SPARQL + "?query=" + URLEncoder.encode(sparql, StandardCharsets.UTF_8) + "&format=json";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/sparql-results+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode bindings = new ObjectMapper().readTree(response.body())
                .path("results").path("bindings");

        if (bindings.isArray() && bindings.size() > 0) {
            return bindings.get(0).path("aatID").path("value").asText();
        }
        return null;
    }
}
