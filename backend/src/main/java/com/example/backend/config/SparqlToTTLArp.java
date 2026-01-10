package com.example.backend.config;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SparqlToTTLArp {

    // Namespace-uri
    private static final String NS_ARP = "http://arp.ro/schema#";
    private static final String NS_PROV = "http://www.w3.org/ns/prov#";
    private static final String NS_DCT = "http://purl.org/dc/terms/";
    private static final String NS_FOAF = "http://xmlns.com/foaf/0.1/";

    // Prefixele pentru resursele generate
    private static final String NS_RES_ARTWORK = "http://arp.ro/resource/artwork/";
    private static final String NS_RES_AGENT = "http://arp.ro/resource/agent/";
    private static final String NS_RES_ACTIVITY = "http://arp.ro/resource/activity/";

    public static void main(String[] args) throws Exception {
        System.out.println("Se caută ID-urile operelor de artă populare...");

        List<String> qIds = fetchPopularArtworkIds(30, 0);

        System.out.println("S-au găsit " + qIds.size() + " opere: " + qIds);

        if (!qIds.isEmpty()) {
            saveArtworksAsTurtle(qIds, "artworks_arp.ttl");
        }
    }

    public static List<String> fetchPopularArtworkIds(int limit, int offset) {
        String queryStr = """
            PREFIX wd: <http://www.wikidata.org/entity/>
            PREFIX wdt: <http://www.wikidata.org/prop/direct/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX wikibase: <http://wikiba.se/ontology#>
            SELECT ?title ?artwork ?sitelinks
            WHERE {
              ?artwork wdt:P31 wd:Q838948 . 
              ?artwork rdfs:label ?title FILTER(LANG(?title)="en")
              ?artwork wikibase:sitelinks ?sitelinks
            }
            GROUP BY ?title ?artwork ?sitelinks
            ORDER BY DESC(?sitelinks)
            LIMIT {{LIMIT}} OFFSET {{OFFSET}}
            """
                .replace("{{LIMIT}}", String.valueOf(limit))
                .replace("{{OFFSET}}", String.valueOf(offset));

        List<String> ids = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionHTTP.service("https://query.wikidata.org/sparql")
                .query(queryStr).build()) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String uri = sol.getResource("artwork").getURI();
                ids.add(uri.substring(uri.lastIndexOf("/") + 1));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return ids;
    }

    // --- Funcția Helper cerută ---
    private static String getLiteral(QuerySolution sol, String var) {
        return sol.contains(var) && sol.get(var).isLiteral() ? sol.getLiteral(var).getString() : "";
    }

    public static void saveArtworksAsTurtle(List<String> qIds, String filename) throws Exception {
        String sparqlTemplate = loadSparql("/sparql/generate.sparql");
        String idsBlock = String.join("\n", qIds.stream().map(id -> "wd:" + id).toList());
        String sparql = sparqlTemplate.replace("{{ARTWORK_IDS}}", idsBlock);

        System.out.println("Se descarcă detaliile și se generează TTL (Format ARP)...");

        try (QueryExecution qexec = QueryExecutionHTTP.service("https://query.wikidata.org/sparql")
                .query(sparql).build()) {

            ResultSet rs = qexec.execSelect();
            Model model = ModelFactory.createDefaultModel();

            // Setăm Prefixele
            model.setNsPrefix("arp", NS_ARP);
            model.setNsPrefix("prov", NS_PROV);
            model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
            model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
            model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
            model.setNsPrefix("dct", NS_DCT);
            model.setNsPrefix("foaf", NS_FOAF);

            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                if (!sol.contains("artwork")) continue;

                String wikiUri = sol.getResource("artwork").getURI();
                String qId = wikiUri.substring(wikiUri.lastIndexOf("/") + 1);

                // ==========================================
                // 1. ENTITATEA ARTWORK
                // ==========================================
                Resource artRes = model.createResource(NS_RES_ARTWORK + qId);
                artRes.addProperty(RDF.type, model.createResource(NS_ARP + "Artwork"));

                // Title
                String title = getLiteral(sol, "title");
                if (!title.isEmpty()) {
                    artRes.addProperty(model.createProperty(NS_ARP + "title"), title);
                }

                // Description
                String desc = getLiteral(sol, "description");
                if (!desc.isEmpty()) {
                    artRes.addProperty(model.createProperty(NS_ARP + "description"), desc);
                }

                // Image (Aici nu folosim getLiteral pentru că este o Resursă URI)
                if (sol.contains("image")) {
                    String imgUrl = sol.getResource("image").getURI();
                    artRes.addProperty(model.createProperty(NS_ARP + "imageLink"), imgUrl, XSDDatatype.XSDanyURI);
                }

                // Inventory Number (Mandatory)
                String invNums = getLiteral(sol, "inventoryNumbers");
                if (!invNums.isEmpty()) {
                    String inv = invNums.split(",")[0].trim();
                    artRes.addProperty(model.createProperty(NS_ARP + "inventoryNumber"), inv);
                } else {
                    artRes.addProperty(model.createProperty(NS_ARP + "inventoryNumber"), "unknown");
                }

                // Dimensions
                String hVal = getLiteral(sol, "heightValue");
                String wVal = getLiteral(sol, "widthValue");
                String uLabel = getLiteral(sol, "heightUnitLabel");

                if (!hVal.isEmpty() && !wVal.isEmpty()) {
                    String dim = hVal + " x " + wVal + " " + uLabel;
                    artRes.addProperty(model.createProperty(NS_ARP + "dimensions"), dim);
                }

                // Category & Classification
                processClassifications(model, artRes, sol);

                // Current Location
                processLocation(model, artRes, sol);

                // License
                artRes.addProperty(model.createProperty(NS_DCT + "license"),
                        model.createResource("http://www.europeana.eu/rights/rr-f/"));

                // ==========================================
                // 2. ENTITATEA CREATION
                // ==========================================
                Resource creationRes = model.createResource(NS_RES_ACTIVITY + "creation_" + qId);
                creationRes.addProperty(RDF.type, model.createResource(NS_ARP + "Creation"));

                String titleStr = !title.isEmpty() ? title : "Unknown";
                creationRes.addProperty(RDFS.label, "Crearea operei " + titleStr);

                // Links
                artRes.addProperty(model.createProperty(NS_PROV + "wasGeneratedBy"), creationRes);
                creationRes.addProperty(model.createProperty(NS_PROV + "generated"), artRes);

                // Date
                String inception = getLiteral(sol, "inception");
                if (!inception.isEmpty()) {
                    String year = inception.length() >= 4 ? inception.substring(0, 4) : inception;
                    if (inception.startsWith("-")) year = inception.substring(0, 5);
                    creationRes.addProperty(model.createProperty(NS_ARP + "startedAtTime"), year, XSDDatatype.XSDgYear);
                }

                // Materials (AICI ESTE IMPLEMENTAREA CERUTĂ SPECIFIC)
                // Folosim getLiteral si verificam isEmpty()
                if (!getLiteral(sol, "materialLabels").isEmpty()) {
                    String materials = getLiteral(sol, "materialLabels").replace("|", "; ");
                    creationRes.addProperty(model.createProperty(NS_ARP + "materialsUsed"), materials);
                }

                // ==========================================
                // 3. ARTIST
                // ==========================================
                processArtist(model, artRes, creationRes, sol);

                // ==========================================
                // 4. SIGNIFICANT EVENTS
                // ==========================================
                processSignificantEvents(model, artRes, sol, "significantEvents", qId);
            }

            try (FileOutputStream out = new FileOutputStream(filename)) {
                RDFDataMgr.write(out, model, RDFFormat.TURTLE);
                System.out.println("Salvat cu succes în " + filename);
            }
        }
    }

    // --- Helper Methods actualizate cu getLiteral ---

    private static void processClassifications(Model model, Resource artRes, QuerySolution sol) {
        String instanceLabels = getLiteral(sol, "instanceLabels");

        if (!instanceLabels.isEmpty()) {
            String[] cats = instanceLabels.split("\\|");
            if (cats.length > 0) artRes.addProperty(model.createProperty(NS_ARP + "category"), cats[0]);
        } else {
            artRes.addProperty(model.createProperty(NS_ARP + "category"), "Unknown");
        }

        // artRes.addProperty(model.createProperty(NS_ARP + "classification"), "IMAGE");
        // artRes.addProperty(model.createProperty(NS_ARP + "classification"), "artă plastică");

        if (!instanceLabels.isEmpty()) {
            for (String c : instanceLabels.split("\\|")) {
                if (!c.isBlank()) artRes.addProperty(model.createProperty(NS_ARP + "classification"), c);
            }
        }
    }

    private static void processLocation(Model model, Resource artRes, QuerySolution sol) {
        Resource museumRes = null;
        String locName = "";

        String locationNames = getLiteral(sol, "locationNames");

        if (!locationNames.isEmpty()) {
            String[] locs = locationNames.split("\\|");
            if (locs.length > 0 && !locs[0].isBlank()) {
                locName = locs[0];
                String locUri = NS_RES_AGENT + sanitizeURI(locName);
                museumRes = model.createResource(locUri);
            }
        }

        // Fallback la Collection
        if (museumRes == null) {
            String collectionsData = getLiteral(sol, "collectionsData");
            if (!collectionsData.isEmpty()) {
                String[] entries = collectionsData.split("\\|");
                if (entries.length > 0) {
                    String colName = entries[0].split("::")[0];
                    if (!colName.isBlank()) {
                        locName = colName;
                        String colUri = NS_RES_AGENT + sanitizeURI(colName);
                        museumRes = model.createResource(colUri);
                    }
                }
            }
        }

        if (museumRes == null) {
            locName = "Unknown Location";
            museumRes = model.createResource(NS_RES_AGENT + "Unknown_Location");
        }

        if (!model.contains(museumRes, RDF.type)) {
            museumRes.addProperty(RDF.type, model.createResource(NS_ARP + "Museum"));
            museumRes.addProperty(model.createProperty(NS_ARP + "name"), locName);
        }
        artRes.addProperty(model.createProperty(NS_ARP + "currentLocation"), museumRes);
    }

    private static void processArtist(Model model, Resource artRes, Resource creationRes, QuerySolution sol) {
        boolean artistFound = false;
        String artistNames = getLiteral(sol, "artistNames");

        if (!artistNames.isEmpty()) {
            String[] artists = artistNames.split("\\|");
            for (String artistName : artists) {
                if (artistName.isBlank()) continue;

                artistFound = true;
                String artistUri = NS_RES_AGENT + sanitizeURI(artistName);
                Resource artistRes = model.createResource(artistUri);

                if (!model.contains(artistRes, RDF.type)) {
                    artistRes.addProperty(RDF.type, model.createResource(NS_ARP + "Artist"));
                    artistRes.addProperty(model.createProperty(NS_ARP + "name"), artistName);
                }

                artRes.addProperty(model.createProperty(NS_PROV + "wasAttributedTo"), artistRes);
                addQualifiedAssociation(model, creationRes, artistRes);
            }
        }

        if (!artistFound) {
            Resource anonArtist = model.createResource(NS_RES_AGENT + "Unknown_Artist");
            if (!model.contains(anonArtist, RDF.type)) {
                anonArtist.addProperty(RDF.type, model.createResource(NS_ARP + "Artist"));
                anonArtist.addProperty(model.createProperty(NS_ARP + "name"), "Unknown Artist");
            }
            addQualifiedAssociation(model, creationRes, anonArtist);
        }
    }

    private static void addQualifiedAssociation(Model model, Resource creationRes, Resource agentRes) {
        Resource assoc = model.createResource();
        assoc.addProperty(RDF.type, model.createResource(NS_PROV + "Association"));
        assoc.addProperty(model.createProperty(NS_PROV + "agent"), agentRes);
        assoc.addProperty(model.createProperty(NS_PROV + "hadRole"), model.createResource(NS_ARP + "Artist"));
        creationRes.addProperty(model.createProperty(NS_PROV + "qualifiedAssociation"), assoc);
    }

    private static void processSignificantEvents(Model model, Resource artRes, QuerySolution sol, String varName, String qId) {
        String rawData = getLiteral(sol, varName);

        if (!rawData.isEmpty()) {
            int counter = 1;
            for (String entry : rawData.split("\\|")) {
                String[] parts = entry.split("::", -1);
                if (parts.length > 0 && !parts[0].isBlank()) {
                    Resource eventRes = model.createResource(NS_RES_ACTIVITY + "event_" + qId + "_" + counter++);
                    eventRes.addProperty(RDF.type, model.createResource(NS_ARP + "TransferOfCustody"));
                    eventRes.addProperty(RDFS.label, parts[0]);

                    if (parts.length > 1 && !parts[1].isBlank()) {
                        String dateStr = parts[1];
                        String year = dateStr.length() >= 4 ? dateStr.substring(0, 4) : dateStr;
                        eventRes.addProperty(model.createProperty(NS_ARP + "startedAtTime"), year, XSDDatatype.XSDgYear);
                    }
                    if (parts.length > 2 && !parts[2].isBlank()) {
                        eventRes.addProperty(model.createProperty(NS_ARP + "locationCity"), parts[2]);
                    }
                    if (parts.length > 3 && !parts[3].isBlank()) {
                        Resource from = model.createResource(NS_RES_AGENT + sanitizeURI(parts[3]));
                        from.addProperty(model.createProperty(NS_ARP + "name"), parts[3]);
                        from.addProperty(RDF.type, model.createResource(NS_ARP + "Agent"));
                        eventRes.addProperty(model.createProperty(NS_ARP + "transferredFrom"), from);
                    }
                    if (parts.length > 4 && !parts[4].isBlank()) {
                        Resource to = model.createResource(NS_RES_AGENT + sanitizeURI(parts[4]));
                        to.addProperty(model.createProperty(NS_ARP + "name"), parts[4]);
                        to.addProperty(RDF.type, model.createResource(NS_ARP + "Agent"));
                        eventRes.addProperty(model.createProperty(NS_ARP + "transferredTo"), to);
                    }
                    eventRes.addProperty(model.createProperty(NS_PROV + "used"), artRes);
                }
            }
        }
    }

    private static String sanitizeURI(String name) {
        return name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_").replaceAll("_+", "_");
    }

    private static String loadSparql(String path) throws Exception {
        InputStream is = SparqlToTTLArp.class.getResourceAsStream(path);
        if (is == null) throw new RuntimeException("Fișier SPARQL nu a fost găsit: " + path);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}