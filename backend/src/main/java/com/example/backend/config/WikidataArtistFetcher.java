package com.example.backend.config;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;

public class WikidataArtistFetcher {

    private static final int BATCH_SIZE = 50;
    private static final String INPUT_FILE = "output.ttl";
    private static final String OUTPUT_FILE = "artists_wikidata.ttl";

    public static void main(String[] args) throws Exception {
        new WikidataArtistFetcher().run();
    }

    public void run() throws Exception {
        // 1️⃣ Încarcă TTL-ul existent
        Model model = RDFDataMgr.loadModel(INPUT_FILE);

        // Prefixe
        String ARP_NS = "http://arp.ro/schema#";
        model.setNsPrefix("arp", ARP_NS);

        Property wikidataUriProp = model.createProperty(ARP_NS, "wikidataUri");
        Property imageLinkProp = model.createProperty(ARP_NS, "imageLink");
        Property wikidataNameProp = model.createProperty(ARP_NS, "wikidataName");
        Property nameProp = model.createProperty(ARP_NS, "name");
        Resource artistType = model.createResource(ARP_NS + "Artist");

        List<Resource> artists = model.listResourcesWithProperty(RDF.type, artistType).toList();

        // Pregătim batch-ul
        Model batchModel = ModelFactory.createDefaultModel();
        batchModel.setNsPrefixes(model.getNsPrefixMap());

        int count = 0;
        boolean firstBatch = true;

        // Ștergem fișierul vechi dacă există
        File outFile = new File(OUTPUT_FILE);
        if (outFile.exists()) outFile.delete();

        for (Resource artistRes : artists) {
            Literal nameLit = artistRes.getProperty(nameProp).getLiteral();
            String name = nameLit.getString();

            String[] parts = name.split(",\\s*");
            String searchName = parts.length == 2 ? parts[1] + " " + parts[0] : name;
            searchName = toWikidataName(searchName);

            // Query Wikidata
            String wdQueryTemplate = loadSparql("/sparql/wikidata_artist_query.sparql");
            String wdQuery = String.format(wdQueryTemplate, searchName);

            try (QueryExecution qexecWd = QueryExecutionHTTP.service(
                    "https://query.wikidata.org/sparql", wdQuery)) {

                ResultSet wdResults = qexecWd.execSelect();
                Resource resCopy = batchModel.createResource(artistRes.getURI());
                resCopy.addProperty(RDF.type, artistType);
                resCopy.addProperty(nameProp, nameLit);
                if (wdResults.hasNext()) {

                    QuerySolution wdSol = wdResults.nextSolution();
                        Resource wdArtist = wdSol.getResource("wikidataArtist");
                        Resource image = wdSol.contains("image") ? wdSol.getResource("image") : null;
                        Literal wdLabel = wdSol.getLiteral("wikidataArtistLabel"); // nou
                        resCopy.addProperty(wikidataUriProp, wdArtist);
                        resCopy.addProperty(wikidataNameProp, wdLabel);
                        if (image != null) {
                            resCopy.addProperty(imageLinkProp, image);
                        }

                    System.out.println("Appended Wikidata info for " + name);
                }
            } catch (Exception e) {
                System.err.println("⚠ Error fetching Wikidata for " + name + ": " + e.getMessage());
            }

            count++;

            // Scriem batch-ul
            if (count % BATCH_SIZE == 0) {
                appendBatchToFile(batchModel, firstBatch);
                batchModel.removeAll(); // reset batch
                firstBatch = false;
            }
        }

        // Scriem restul artiștilor rămași
        if (!batchModel.isEmpty()) {
            appendBatchToFile(batchModel, firstBatch);
        }

        System.out.println("✅ Finished processing all artists!");
    }

    private void appendBatchToFile(Model batchModel, boolean writePrefixes) throws Exception {
        try (FileOutputStream out = new FileOutputStream(OUTPUT_FILE, true)) {
            if (writePrefixes) {
                RDFDataMgr.write(out, batchModel, RDFFormat.TURTLE_PRETTY);
            } else {
                RDFDataMgr.write(out, batchModel, RDFFormat.TURTLE_PRETTY);
            }
        }
    }


    // Helper: normalizează numele pentru căutarea pe Wikidata
    private String toWikidataName(String original) {
        return Normalizer.normalize(original, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private String loadSparql(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("SPARQL file not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
