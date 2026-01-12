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

public class WikidataMuseumFetcher {

    private static final int BATCH_SIZE = 20;
    private static final String INPUT_FILE = "artworks_arp_int.ttl";
    private static final String OUTPUT_FILE = "museums_wikidata_getty_int.ttl";

    // Endpoint oficial Getty
    private static final String GETTY_SPARQL_ENDPOINT = "http://vocab.getty.edu/sparql";

    private Property wikidataUriProp;
    private Property wikidataNameProp;
    private Property imageLinkProp;
    private Property gettyUriProp;
    private Property gettyIdProp;

    public static void main(String[] args) throws Exception {
        new WikidataMuseumFetcher().run();
    }

    public void run() throws Exception {
        System.out.println("--- START PROCESARE (Wikidata + Fallback Getty [ULAN/TGN]) ---");

        File inFile = new File(INPUT_FILE);
        if (!inFile.exists()) {
            System.err.println("EROARE: Fisierul " + INPUT_FILE + " nu exista!");
            return;
        }

        Model model = RDFDataMgr.loadModel(INPUT_FILE);

        String ARP_NS = "http://arp.ro/schema#";
        model.setNsPrefix("arp", ARP_NS);
        // Adaugam prefixe pentru ambele tipuri Getty
        model.setNsPrefix("ulan", "http://vocab.getty.edu/ulan/");
        model.setNsPrefix("tgn", "http://vocab.getty.edu/tgn/");

        wikidataUriProp = model.createProperty(ARP_NS, "wikidataUri");
        wikidataNameProp = model.createProperty(ARP_NS, "wikidataName");
        imageLinkProp = model.createProperty(ARP_NS, "imageLink");
        Property nameProp = model.createProperty(ARP_NS, "name");
        gettyUriProp = model.createProperty(ARP_NS, "gettyUri");
        gettyIdProp = model.createProperty(ARP_NS, "gettyId");
        Resource museumType = model.createResource(ARP_NS + "Museum");

        List<Resource> museums = model.listResourcesWithProperty(RDF.type, museumType).toList();
        System.out.println("Total muzee de procesat: " + museums.size());

        Model batchModel = ModelFactory.createDefaultModel();
        batchModel.setNsPrefixes(model.getNsPrefixMap());

        File outFile = new File(OUTPUT_FILE);
        if (outFile.exists()) outFile.delete();

        String wdSparqlTemplate = loadSparql("/sparql/wikidata_museum_query.sparql");

        // Template Getty Universal (ULAN + TGN) ---
        String gettySparqlTemplate =
                "PREFIX luc: <http://www.ontotext.com/owlim/lucene#> " +
                        "PREFIX gvp: <http://vocab.getty.edu/ontology#> " +
                        "SELECT ?subject ?shortId WHERE { " +
                        "  ?subject luc:term \"%s\" . " +
                        "  ?subject a gvp:Subject . " +

                        // 1. FILTRU: Acceptam doar ULAN (Institutii) sau TGN (Locatii/Cladiri)
                        "  FILTER (REGEX(STR(?subject), \"/(ulan|tgn)/\", \"i\")) ." +

                        // 2. CURATARE: Stergem prefixul HTTP indiferent care este el (ulan sau tgn)
                        // Rezultatul in ?shortId va fi doar numarul (ex: 500305940 sau 2615185)
                        "  BIND(REPLACE(STR(?subject), \"http://vocab.getty.edu/(ulan|tgn)/\", \"\") AS ?shortId) " +
                        "} LIMIT 1";

        int count = 0;
        boolean firstBatch = true;

        for (Resource museumRes : museums) {
            if (!museumRes.hasProperty(nameProp)) continue;

            String originalName = museumRes.getProperty(nameProp).getLiteral().getString();
            String searchName = cleanMuseumName(originalName);
            String safeSearchName = searchName.replace("\"", "\\\"");

            Resource resCopy = batchModel.createResource(museumRes.getURI());
            resCopy.addProperty(RDF.type, museumType);
            resCopy.addProperty(nameProp, originalName);

            // 1. CAUTARE WIKIDATA
            QuerySolution wdSol = executeSearch(wdSparqlTemplate, safeSearchName, "https://query.wikidata.org/sparql");
            boolean foundGettyInWiki = false;

            if (wdSol != null) {
                processWikidataSolution(wdSol, resCopy, batchModel);
                if (wdSol.contains("ulan")) {
                    foundGettyInWiki = true;
                    System.out.println("✔ Wikidata + Getty gasit: " + searchName);
                } else {
                    System.out.println("⚠ Wikidata gasit, dar FARA Getty: " + searchName);
                }
            } else {
                System.out.println("✖ Wikidata nu a gasit nimic: " + searchName);
            }

            // 2. CAUTARE DIRECTA IN GETTY (Fallback)
            if (!foundGettyInWiki) {
                QuerySolution gettySol = executeSearch(gettySparqlTemplate, safeSearchName, GETTY_SPARQL_ENDPOINT);

                if (gettySol != null) {
                    // Acum luam ID-ul curat (shortId) care poate fi ULAN sau TGN
                    String cleanId = gettySol.getLiteral("shortId").getString();
                    String fullUri = gettySol.getResource("subject").getURI();

                    resCopy.addProperty(gettyIdProp, cleanId);
                    resCopy.addProperty(gettyUriProp, batchModel.createResource(fullUri));

                    // Determinam tipul doar pentru afisare in consola
                    String type = fullUri.contains("/tgn/") ? "TGN" : "ULAN";
                    System.out.println("   ★ RECUPERAT DIN GETTY (" + type + "): " + cleanId);
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
        System.out.println("fisierul: " + OUTPUT_FILE);
    }

    private QuerySolution executeSearch(String template, String name, String endpoint) {
        try {
            String query = template.replace("%s", name);
            try (QueryExecution qexec = QueryExecutionHTTP.service(endpoint, query)) {
                ResultSet results = qexec.execSelect();
                if (results.hasNext()) {
                    return results.nextSolution();
                }
            }
        } catch (Exception e) {
            // Ignoram erorile temporare
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

    private String cleanMuseumName(String rawName) {
        if (rawName == null) return "";
        String clean = rawName.replaceAll("\\s*-\\s*[A-ZȘȚÂÎĂ]{1,2}\\s*$", "");
        if (clean.equals(rawName) && rawName.contains(" - ")) {
            clean = rawName.split(" - ")[0].trim();
        }
        return clean.replace("\"", "").trim();
    }

    private String loadSparql(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("SPARQL file missing: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}