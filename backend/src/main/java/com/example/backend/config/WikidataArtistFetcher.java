package com.example.backend.config;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.vocabulary.RDF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WikidataArtistFetcher {

    private static final int BATCH_SIZE = 50;
    // Asigura-te ca fisierul de intrare este corect
    private static final String INPUT_FILE = "output.ttl";
    private static final String OUTPUT_FILE = "artists_wikidata_getty.ttl";

    // Endpoint oficial Getty
    private static final String GETTY_SPARQL_ENDPOINT = "http://vocab.getty.edu/sparql";

    private Property wikidataUriProp;
    private Property wikidataNameProp;
    private Property imageLinkProp;
    private Property gettyUriProp;
    private Property gettyIdProp;
    private Property nameProp;

    public static void main(String[] args) throws Exception {
        new WikidataArtistFetcher().run();
    }

    public void run() throws Exception {
        System.out.println("--- START PROCESARE ARTISTI (Wikidata + Fallback Getty) ---");

        File inFile = new File(INPUT_FILE);
        if (!inFile.exists()) {
            System.err.println("EROARE: Fisierul " + INPUT_FILE + " nu exista!");
            return;
        }

        Model model = RDFDataMgr.loadModel(INPUT_FILE);

        String ARP_NS = "http://arp.ro/schema#";
        model.setNsPrefix("arp", ARP_NS);
        // Prefixele Getty
        model.setNsPrefix("ulan", "http://vocab.getty.edu/ulan/");
        model.setNsPrefix("tgn", "http://vocab.getty.edu/tgn/");

        // Initializare Proprietati
        wikidataUriProp = model.createProperty(ARP_NS, "wikidataUri");
        wikidataNameProp = model.createProperty(ARP_NS, "wikidataName");
        imageLinkProp = model.createProperty(ARP_NS, "imageLink");
        nameProp = model.createProperty(ARP_NS, "name");
        gettyUriProp = model.createProperty(ARP_NS, "gettyUri");
        gettyIdProp = model.createProperty(ARP_NS, "gettyId");

        Resource artistType = model.createResource(ARP_NS + "Artist");

        List<Resource> artists = model.listResourcesWithProperty(RDF.type, artistType).toList();
        System.out.println("Total artisti de procesat: " + artists.size());

        Model batchModel = ModelFactory.createDefaultModel();
        batchModel.setNsPrefixes(model.getNsPrefixMap());

        File outFile = new File(OUTPUT_FILE);
        if (outFile.exists()) outFile.delete();

        // Incarcam template-ul SPARQL pentru Wikidata (adaptat pentru artisti)
        String wdSparqlTemplate = loadSparql("/sparql/wikidata_artist_query.sparql");

        // Template Getty Universal (ULAN pt artisti)
        String gettySparqlTemplate =
                "PREFIX luc: <http://www.ontotext.com/owlim/lucene#> " +
                        "PREFIX gvp: <http://vocab.getty.edu/ontology#> " +
                        "SELECT ?subject ?shortId WHERE { " +
                        "  ?subject luc:term \"%s\" . " +
                        "  ?subject a gvp:Subject . " +
                        "  FILTER (REGEX(STR(?subject), \"/(ulan|tgn)/\", \"i\")) ." +
                        "  BIND(REPLACE(STR(?subject), \"http://vocab.getty.edu/(ulan|tgn)/\", \"\") AS ?shortId) " +
                        "} LIMIT 1";

        int count = 0;
        boolean firstBatch = true;

        for (Resource artistRes : artists) {
            if (!artistRes.hasProperty(nameProp)) continue;

            String originalName = artistRes.getProperty(nameProp).getLiteral().getString();

            // PRELUCRARE NUME: "Nume, Prenume" -> "Prenume Nume"
            String searchName = formatArtistName(originalName);

            // Escapare ghilimele pentru query
            String safeSearchName = searchName.replace("\"", "\\\"");

            Resource resCopy = batchModel.createResource(artistRes.getURI());
            resCopy.addProperty(RDF.type, artistType);
            resCopy.addProperty(nameProp, originalName);

            // 1. CAUTARE WIKIDATA
            QuerySolution wdSol = executeSearch(wdSparqlTemplate, safeSearchName, "https://query.wikidata.org/sparql");
            boolean foundGettyInWiki = false;

            if (wdSol != null) {
                processWikidataSolution(wdSol, resCopy, batchModel);
                if (wdSol.contains("ulan")) {
                    foundGettyInWiki = true;
                    System.out.println("✔ Wikidata + Getty: " + searchName);
                } else {
                    System.out.println("⚠ Wikidata gasit (FARA Getty): " + searchName);
                }
            } else {
                System.out.println("✖ Wikidata nu a gasit nimic: " + searchName);
            }

            // 2. CAUTARE DIRECTA IN GETTY (Fallback)
            if (!foundGettyInWiki) {
                QuerySolution gettySol = executeSearch(gettySparqlTemplate, safeSearchName, GETTY_SPARQL_ENDPOINT);

                if (gettySol != null) {
                    String cleanId = gettySol.getLiteral("shortId").getString();
                    String fullUri = gettySol.getResource("subject").getURI();

                    resCopy.addProperty(gettyIdProp, cleanId);
                    resCopy.addProperty(gettyUriProp, batchModel.createResource(fullUri));

                    System.out.println("   ★ RECUPERAT DIN GETTY: " + cleanId);
                }
            }

            count++;
            if (count % BATCH_SIZE == 0) {
                appendBatchToFile(batchModel, firstBatch);
                batchModel.removeAll();
                firstBatch = false;
            }
        }

        if (!batchModel.isEmpty()) {
            appendBatchToFile(batchModel, firstBatch);
        }
        System.out.println("✅ Gata! Vezi fisierul: " + OUTPUT_FILE);
    }

    private QuerySolution executeSearch(String template, String name, String endpoint) {
        try {
            // Folosim replace simplu, nu String.format, pentru siguranta
            String query = template.replace("%s", name);
            try (QueryExecution qexec = QueryExecutionHTTP.service(endpoint, query)) {
                ResultSet results = qexec.execSelect();
                if (results.hasNext()) {
                    return results.nextSolution();
                }
            }
        } catch (Exception e) {
            // Silent catch
        }
        return null;
    }

    private void processWikidataSolution(QuerySolution sol, Resource res, Model model) {
        Resource wdItem = sol.getResource("item");
        res.addProperty(wikidataUriProp, wdItem);

        if (sol.contains("itemLabel")) {
            res.addProperty(wikidataNameProp, sol.getLiteral("itemLabel"));
        }

        if (sol.contains("image")) {
            String imgUrl = sol.getResource("image").getURI();
            if (imgUrl.contains("/wiki/File:")) {
                imgUrl = imgUrl.replace("/wiki/File:", "/wiki/Special:FilePath/");
            }
            res.addProperty(imageLinkProp, model.createResource(imgUrl));
        }

        if (sol.contains("ulan")) {
            String ulanId = sol.getLiteral("ulan").getString();
            res.addProperty(gettyIdProp, ulanId);
            res.addProperty(gettyUriProp, model.createResource("http://vocab.getty.edu/ulan/" + ulanId));
        }
    }

    private void appendBatchToFile(Model batchModel, boolean firstBatch) throws Exception {
        try (FileOutputStream out = new FileOutputStream(OUTPUT_FILE, true)) {
            RDFDataMgr.write(out, batchModel, RDFFormat.TURTLE_PRETTY);
        }
    }

    // Functie specifica pentru artisti: "Grigorescu, Nicolae" -> "Nicolae Grigorescu"
    private String formatArtistName(String rawName) {
        if (rawName == null) return "";
        String clean = rawName.trim();

        // Daca contine virgula, inversam partile
        if (clean.contains(",")) {
            String[] parts = clean.split(",");
            if (parts.length >= 2) {
                // parts[1] (Prenume) + parts[0] (Nume)
                clean = parts[1].trim() + " " + parts[0].trim();
            }
        }
        return clean.replace("\"", "");
    }

    private String loadSparql(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("SPARQL file not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}