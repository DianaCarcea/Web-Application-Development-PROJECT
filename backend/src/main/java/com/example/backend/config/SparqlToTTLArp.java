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

        List<String> qIds = fetchPopularArtworkIds(100, 0);

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

    // --- Funcția Helper ---
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

                // Metadata de bază
                String title = getLiteral(sol, "title");
                if (!title.isEmpty()) artRes.addProperty(model.createProperty(NS_ARP + "title"), title);

                String desc = getLiteral(sol, "description");
                if (!desc.isEmpty()) artRes.addProperty(model.createProperty(NS_ARP + "description"), desc);

                if (sol.contains("image") && sol.get("image").isResource()) {
                    String imgUrl = sol.getResource("image").getURI();
                    imgUrl = ImageOrchestrator.fixCommonsUrl(imgUrl);
                    artRes.addProperty(model.createProperty(NS_ARP + "imageLink"), imgUrl, XSDDatatype.XSDanyURI);
                } else {
                    String imgUrl = ImageOrchestrator.getBestImage(title);
                    artRes.addProperty(model.createProperty(NS_ARP + "imageLink"), imgUrl, XSDDatatype.XSDanyURI);
                }

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

                processClassifications(model, artRes, sol);
                processLocation(model, artRes, sol);

                artRes.addProperty(model.createProperty(NS_DCT + "license"),
                        model.createResource("http://www.europeana.eu/rights/rr-f/"));

                // ==========================================
                // 2. ENTITATEA CREATION
                // ==========================================
                Resource creationRes = model.createResource(NS_RES_ACTIVITY + "creation_" + qId);
                creationRes.addProperty(RDF.type, model.createResource(NS_ARP + "Creation"));

                String titleStr = !title.isEmpty() ? title : "Unknown";
                creationRes.addProperty(RDFS.label, "Crearea operei " + titleStr);

                artRes.addProperty(model.createProperty(NS_PROV + "wasGeneratedBy"), creationRes);
                creationRes.addProperty(model.createProperty(NS_PROV + "generated"), artRes);

                String inception = getLiteral(sol, "inception");
                String startTime = getLiteral(sol, "startTime");
                String publicationDate = getLiteral(sol, "publicationDate");
                String pointInTime = getLiteral(sol, "pointInTime");

                setCreationYear(model, creationRes, inception, startTime, publicationDate, pointInTime);

                if (!getLiteral(sol, "materialLabels").isEmpty()) {
                    String materials = getLiteral(sol, "materialLabels").replace("|", "; ");
                    creationRes.addProperty(model.createProperty(NS_ARP + "materialsUsed"), materials);
                }

                processArtist(model, artRes, creationRes, sol);

                // ==========================================
                // 3. OWNERSHIP HISTORY (arp:hasOwnership) -> NOU!
                // ==========================================
                processOwnershipHistory(model, artRes, sol, "ownersData");

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

    // --- Helper Methods ---

    private static void processOwnershipHistory(Model model, Resource artRes, QuerySolution sol, String varName) {
        String rawData = getLiteral(sol, varName);

        if (!rawData.isEmpty()) {
            for (String entry : rawData.split("\\|")) {
                String[] parts = entry.split("::", -1);
                // Format: OwnerName::Start::End
                if (parts.length > 0 && !parts[0].isBlank()) {
                    String ownerName = parts[0];
                    String startDate = (parts.length > 1) ? parts[1] : "";
                    String endDate = (parts.length > 2) ? parts[2] : "";

                    // 1. Creăm Agentul (Collector)
                    // Folosim prefixul "Collector_" în URI pentru a-l distinge
                    String collectorUri = NS_RES_AGENT + "Collector_" + sanitizeURI(ownerName);
                    Resource collectorRes = model.createResource(collectorUri);

                    if (!model.contains(collectorRes, RDF.type)) {
                        collectorRes.addProperty(RDF.type, model.createResource(NS_ARP + "Collector"));
                        collectorRes.addProperty(model.createProperty(NS_ARP + "name"), ownerName);
                    }

                    // 2. Creăm Blank Node pentru hasOwnership
                    Resource ownershipNode = model.createResource(); // Nod anonim []
                    ownershipNode.addProperty(RDF.type, model.createResource(NS_ARP + "TransferOfCustody"));

                    // Link către Collector
                    ownershipNode.addProperty(model.createProperty(NS_PROV + "wasAssociatedWith"), collectorRes);

                    // Date (formatăm xsd:date, tăiem ora dacă există)
                    if (!startDate.isBlank()) {
                        String dateOnly = startDate.length() >= 10 ? startDate.substring(0, 10) : startDate;
                        ownershipNode.addProperty(model.createProperty(NS_ARP + "startedAtTime"), dateOnly, XSDDatatype.XSDdate);
                    }
                    if (!endDate.isBlank()) {
                        String dateOnly = endDate.length() >= 10 ? endDate.substring(0, 10) : endDate;
                        ownershipNode.addProperty(model.createProperty(NS_ARP + "endedAtTime"), dateOnly, XSDDatatype.XSDdate);
                    }

                    // 3. Legăm opera de acest ownership
                    artRes.addProperty(model.createProperty(NS_ARP + "hasOwnership"), ownershipNode);
                }
            }
        }
    }

    private static void processClassifications(Model model, Resource artRes, QuerySolution sol) {
        String instanceLabels = getLiteral(sol, "instanceLabels");
        if (!instanceLabels.isEmpty()) {
            String[] cats = instanceLabels.split("\\|");
            if (cats.length > 0) artRes.addProperty(model.createProperty(NS_ARP + "category"), cats[0]);
            for (String c : cats) {
                if (!c.isBlank()) artRes.addProperty(model.createProperty(NS_ARP + "classification"), c);
            }
        } else {
            artRes.addProperty(model.createProperty(NS_ARP + "category"), "Unknown");
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

    /**
     * Caută prima dată validă dintr-o listă de candidați, extrage anul și îl adaugă în model.
     */
    private static void setCreationYear(Model model, Resource res, String... potentialDates) {
        for (String dateStr : potentialDates) {
            // Verificăm dacă data nu e goală
            if (!dateStr.isEmpty()) {
                String year;

                // Logica de extragere a anului (inclusiv pentru anii negativi/BC)
                if (dateStr.startsWith("-")) {
                    // Ex: "-0500-01-01" -> "-0500"
                    year = dateStr.length() >= 5 ? dateStr.substring(0, 5) : dateStr;
                } else {
                    // Ex: "1990-05-20" -> "1990"
                    year = dateStr.length() >= 4 ? dateStr.substring(0, 4) : dateStr;
                }

                // Adăugăm proprietatea și ne oprim (break/return) pentru a respecta prioritatea
                res.addProperty(model.createProperty(NS_ARP + "startedAtTime"), year, XSDDatatype.XSDgYear);
                return;
            }
        }
    }

}