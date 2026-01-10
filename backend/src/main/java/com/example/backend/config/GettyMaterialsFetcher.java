package com.example.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GettyMaterialsFetcher {

    private static final String WIKIDATA_SPARQL = "https://query.wikidata.org/sparql";

    public static void main(String[] args) throws Exception {

        // SINGURA sursă de adevăr
        Map<String, String> translations = getTranslations();

        try (PrintWriter out = new PrintWriter(new FileOutputStream("getty-materials.ttl"))) {

            out.println("@prefix aat:  <http://vocab.getty.edu/aat/> .");
            out.println("@prefix arp:  <http://arp.ro/schema#> .");
            out.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
            out.println("@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n");

            for (Map.Entry<String, String> entry : translations.entrySet()) {

                String ro = entry.getKey();
                String en = entry.getValue();

                String uriId = toUriSafe(ro);
                String aatCode = queryGettyCode(en);

                if (aatCode != null) {
                    out.printf("<http://arp.ro/material/%s>\n", uriId);
                    out.printf("    rdfs:label \"%s\"@ro;\n", ro);
                    out.printf("    rdfs:label \"%s\"@en;\n", en);
                    out.printf("    skos:exactMatch aat:%s .\n\n", aatCode);

                    System.out.printf("✔ %s → %s%n", ro, aatCode);
                } else {
                    System.out.printf("✖ Nu am găsit AAT pentru: %s%n", ro);
                }
            }
        }

        System.out.println("✔ getty-materials.ttl generat cu succes!");
    }

    // URI safe (FĂRĂ spații / diacritice)
    private static String toUriSafe(String input) {
        return input
                .toLowerCase()
                .replace("ă", "a")
                .replace("â", "a")
                .replace("î", "i")
                .replace("ș", "s")
                .replace("ț", "t")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }

    private static Map<String, String> getTranslations() {
        return Map.ofEntries(
                Map.entry("ulei","oil painting"),
                Map.entry("creion","pencil"),
                Map.entry("acuarel","watercolor"),
                Map.entry("sanguină","red chalk"),
                Map.entry("peniță","pen"),
                Map.entry("tuș","ink"),
                Map.entry("lemn","wood"),
                Map.entry("bronz","bronze"),
                Map.entry("marmură","marble"),
                Map.entry("teracotă","terracotta"),
                Map.entry("sticlă","glass"),
                Map.entry("pastel","pastel"),
                Map.entry("tempera","tempera"),
                Map.entry("grafit","graphite"),
                Map.entry("piatră prețioasă", "gemstone"),
                Map.entry("oglindă", "mirror"),
                Map.entry("chihlimbar", "amber"),
                Map.entry("argint", "silver"),
                Map.entry("gazar", "gazar"),
                Map.entry("organza", "organza"),
                Map.entry("tul", "tulle"),
                Map.entry("dantelă", "lace"),
                Map.entry("satin", "satin"),
                Map.entry("pergament", "parchment"),
                Map.entry("beton", "concrete"),
                Map.entry("hârtie", "paper"),
                Map.entry("lut", "clay"),
                Map.entry("gresie", "sandstone"),
                Map.entry("cuarț", "quartz")
        );
    }

    private static String queryGettyCode(String term) throws Exception {

        String sparql =
                "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "SELECT ?aatID WHERE {\n" +
                        "  ?item wdt:P1014 ?aatID.\n" +
                        "  ?item rdfs:label \"" + term + "\"@en.\n" +
                        "} LIMIT 1";

        HttpClient client = HttpClient.newHttpClient();
        String url = WIKIDATA_SPARQL +
                "?query=" + URLEncoder.encode(sparql, StandardCharsets.UTF_8) +
                "&format=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/sparql-results+json")
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode bindings = new ObjectMapper()
                .readTree(response.body())
                .path("results")
                .path("bindings");

        if (bindings.isArray() && bindings.size() > 0) {
            return bindings.get(0).path("aatID").path("value").asText();
        }
        return null;
    }
}
