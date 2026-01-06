package com.example.backend.config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class LidoToTtlConverter {

    private static final Set<String> writtenArtists = new HashSet<>();
    private static final Set<String> writtenMuseums = new HashSet<>();
    private static final Set<String> writtenOrganizations = new HashSet<>();
    private static final Set<String> writtenRegistrars = new HashSet<>();
    private static final Set<String> writtenValidators = new HashSet<>();

    public static void main(String[] args) {
        // Asigură-te că path-ul este corect
        String inputPath = "src/main/resources/rdf/inp-clasate-arp-2014-02-02.xml";
        String outputPath = "output.ttl";

        try {
            File xmlFile = new File(inputPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(false);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("lido:lido");
            // Scriem cu UTF-8
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8))) {

                // HEADER
                writer.println("@prefix arp: <http://arp.ro/schema#> .");
                writer.println("@prefix prov: <http://www.w3.org/ns/prov#> .");
                writer.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
                writer.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
                writer.println("@prefix owl: <http://www.w3.org/2002/07/owl#> .");
                writer.println("@prefix dct: <http://purl.org/dc/terms/> .");
                writer.println("@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n");

                System.out.println("Procesez " + nList.getLength() + " opere...");

                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);

                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element lidoRecord = (Element) nNode;

                        // --- 1. EXTRAGERE DATE GENERALE ---
                        String id = getSimpleTagValue(lidoRecord, "lido:lidoRecID");
                        String titlu = getNestedValue(lidoRecord, "lido:titleSet", "lido:appellationValue");
                        String artist = getNestedValue(lidoRecord, "lido:eventActor", "lido:appellationValue");
                        String data = getNestedValue(lidoRecord, "lido:eventDate", "lido:displayDate");
                        String dimensiuni = getSimpleTagValue(lidoRecord, "lido:displayObjectMeasurements");
                        String imgLink = getNestedValue(lidoRecord, "lido:resourceRepresentation", "lido:linkResource");

                        if (artist.isEmpty()){
                            artist="unknown";
                        }
                        // --- 2. EXTRAGERE LISTE (NOU) ---

                        List<String> listMaterials = new ArrayList<>();
                        List<String> listTechniques = new ArrayList<>();
                        extractMaterialsAndTechniques(lidoRecord, listMaterials, listTechniques);

                        // B. Clasificări (Listă completă: IMAGE, artă plastică, fine arts...)
                        // Se vor duce la Operă
                        List<String> clasificari = getClassificationValues(lidoRecord);

                        // --- 3. EXTRAGERE DATE EXTINSE ---
                        String categorie = getNestedValue(lidoRecord, "lido:objectWorkType", "lido:term");
                        String descriere = getNestedValue(lidoRecord, "lido:objectDescriptionSet", "lido:descriptiveNoteValue");
                        String stare = getSimpleTagValue(lidoRecord, "lido:displayState");
                        String nrInventar = getNestedValue(lidoRecord, "lido:repositorySet", "lido:workID");

                        List<String> cultureList = getCultureList(lidoRecord);

                        // Link-uri externe
                        String cimecLink = getNestedValue(lidoRecord, "lido:recordInfoSet", "lido:recordInfoLink");
                        // Căutăm adânc pentru drepturi (Europeana rights)
                        String rightsLink = getDeepNestedValue(lidoRecord, "lido:rightsResource", "lido:rightsType", "lido:term");


                        String[] recordedInfo = getRecordMetadataDate(lidoRecord, "creation date");
                        String recordedAt = recordedInfo[0];
                        String registrarName = recordedInfo[1];
                        String registrarId = registrarName.replaceAll("[^a-zA-Z0-9]", "_");

                        String[] validatedInfo = getRecordMetadataDate(lidoRecord, "validation date");
                        String validatedAt = validatedInfo[0];
                        String validatorName = validatedInfo[1];
                        String validatorId = validatorName.replaceAll("[^a-zA-Z0-9]", "_");

                        // Muzeu & Sursă
                        String muzeuName = getNestedValue(lidoRecord, "lido:repositoryName", "lido:appellationValue");
                        String recordSourceAgent = "";
                        String sourceWebsite = "";
                        NodeList sourceList = lidoRecord.getElementsByTagName("lido:recordSource");
                        if (sourceList.getLength() > 0) {
                            Element sourceElem = (Element) sourceList.item(0);
                            recordSourceAgent = getNestedValue(sourceElem, "lido:legalBodyName", "lido:appellationValue");
                            sourceWebsite = getSimpleTagValue(sourceElem, "lido:legalBodyWeblink");
                        }

                        // --- 4. GENERARE TTL ---
                        String ttl = generateTTL(id, titlu, artist, data,
                                listMaterials, listTechniques, clasificari,
                                dimensiuni, muzeuName, recordSourceAgent, sourceWebsite,
                                imgLink, cimecLink, rightsLink, recordedAt, registrarName, registrarId,
                                validatedAt , validatorName,  validatorId,
                                categorie, descriere, stare, nrInventar, cultureList);

                        writer.println(ttl);
                    }
                }
                System.out.println("Gata! Output generat în " + outputPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String generateTTL(String id, String titlu, String artist, String data,
                                     List<String> listMaterials, List<String> listTechniques, List<String> clasificari,
                                     String dims, String muzeuName, String sourceAgentName, String sourceWeb,
                                     String img, String cimecUrl, String rightsUrl, String recordedAt , String registrarName, String registrarId,
                                     String validatedAt , String validatorName, String validatorId, String categorie, String descriere, String stare, String nrInventar, List<String> cultureList) {

        StringBuilder sb = new StringBuilder();
        String safeId = id.replaceAll("[^a-zA-Z0-9]", "_");
        String activityId = "creation_" + safeId;
        String artistId = artist.replaceAll("[^a-zA-Z0-9]", "_");
        String museumId = muzeuName.replaceAll("[^a-zA-Z0-9]", "_");

        // ==========================================
        // 1. ENTITY (Opera de Artă)
        // ==========================================
        sb.append("<http://arp.ro/resource/artwork/").append(safeId).append("> a arp:Artwork ;\n");
        if (!titlu.isEmpty()) sb.append("    arp:title \"").append(escape(titlu)).append("\" ;\n");
        if (!dims.isEmpty())  sb.append("    arp:dimensions \"").append(escape(dims)).append("\" ;\n");
        if (!muzeuName.isEmpty()) sb.append("    arp:currentLocation <http://arp.ro/resource/agent/").append(museumId).append("> ;\n");
        if (!img.isEmpty())   sb.append("    arp:imageLink \"").append(img).append("\"^^xsd:anyURI ;\n");

        // Link-uri externe
        if (!cimecUrl.isEmpty()) sb.append("    arp:cimecLink \"").append(cimecUrl).append("\"^^xsd:anyURI ;\n");
        if (!rightsUrl.isEmpty()) sb.append("    dct:license <").append(rightsUrl).append("> ;\n");

        // Detalii specifice (String-uri simple)
        if (!categorie.isEmpty()) sb.append("    arp:category \"").append(escape(categorie)).append("\" ;\n");
        if (!stare.isEmpty())     sb.append("    arp:condition \"").append(escape(stare)).append("\" ;\n");
        if (!nrInventar.isEmpty()) sb.append("    arp:inventoryNumber \"").append(escape(nrInventar)).append("\" ;\n");
        if (!descriere.isEmpty()) sb.append("    arp:description \"").append(escape(descriere)).append("\" ;\n");

        List<String> predicates = new ArrayList<>();
        if (!clasificari.isEmpty()) {
            for (String cls : clasificari) {
                predicates.add("arp:classification \"" + escape(cls) + "\"");
            }
        }
        if (!registrarName.isEmpty()) {
            predicates.add("arp:recordedBy <http://arp.ro/resource/registrar/" + registrarId + ">");
        }
        if (!recordedAt.isEmpty()) {
            predicates.add("arp:recordedAt \"" + recordedAt + "\"^^xsd:date");
        }
        if (!validatorName.isEmpty()) {
            predicates.add("arp:validatedBy <http://arp.ro/resource/validator/" + validatorId+ ">");
        }
        if (!validatedAt.isEmpty()) {
            predicates.add("arp:validatedAt \"" + validatedAt + "\"^^xsd:date");
        }

        predicates.add("prov:wasGeneratedBy <http://arp.ro/resource/activity/" + activityId + ">");
        if (!artist.isEmpty()) {
            predicates.add("prov:wasAttributedTo <http://arp.ro/resource/agent/" + artistId + ">");
        }

        for (int i = 0; i < predicates.size(); i++) {
            sb.append("    ").append(predicates.get(i));
            if (i < predicates.size() - 1) sb.append(" ;\n");
            else sb.append(" .\n");
        }

        // ==========================================
        // 2. ACTIVITY (Creația)
        // ==========================================
        sb.append("\n<http://arp.ro/resource/activity/").append(activityId).append("> a arp:Creation ;\n");
        sb.append("    rdfs:label \"Crearea operei ").append(escape(titlu)).append("\" ;\n");

        if (!data.isEmpty()) {
            sb.append("    arp:startedAtTime ").append(getFormattedDateLiteral(data)).append(" ;\n");
        }

        // [NOU] Iterăm prin LISTA de tehnici și le punem la Activitate
        for (String tehnica : listTechniques) {
            sb.append("    arp:technique \"").append(escape(tehnica)).append("\" ;\n");
        }

        // [MODIFICARE] Scriem Materialele
        for (String material : listMaterials) {
            sb.append("    arp:materialsUsed \"").append(escape(material)).append("\" ;\n");
        }

        for (String culture : cultureList) {
            if (culture.contains("@")) {
                String[] parts = culture.split("@", 2);
                sb.append("    arp:culture \"").append(escape(parts[0])).append("\"@").append(parts[1]).append(" ;\n");
            } else {
                sb.append("    arp:culture \"").append(escape(culture)).append("\" ;\n");
            }
        }

        sb.append("    prov:generated <http://arp.ro/resource/artwork/").append(safeId).append(">");

        if (!artist.isEmpty()) {
            sb.append(" ;\n");
            sb.append("    prov:qualifiedAssociation [\n");
            sb.append("        a prov:Association ;\n");
            sb.append("        prov:agent <http://arp.ro/resource/agent/").append(artistId).append("> ;\n");
            sb.append("        prov:hadRole arp:Artist\n");
            sb.append("    ] .\n");
        } else {
            sb.append(" .\n");
        }

        // ==========================================
        // 3. AGENTS
        // ==========================================
        if (!artist.isEmpty() && writtenArtists.add(artistId)) {
            sb.append("\n<http://arp.ro/resource/agent/").append(artistId).append("> a arp:Artist ;\n");
            sb.append("    arp:name \"").append(escape(artist)).append("\" .\n");
        }

        if (!registrarName.isEmpty() && writtenRegistrars.add(registrarId)) {
            sb.append("\n<http://arp.ro/resource/registrar/").append(registrarId).append("> a arp:Registrar ;\n");
            sb.append("    arp:name \"").append(escape(registrarName)).append("\" .\n");
        }

        if (!validatorName.isEmpty() && writtenValidators.add(validatorId)) {
            sb.append("\n<http://arp.ro/resource/validator/").append(validatorId).append("> a arp:Validator ;\n");
            sb.append("    arp:name \"").append(escape(validatorName)).append("\" .\n");
        }

        if (!muzeuName.isEmpty() && writtenMuseums.add(museumId)) {
            sb.append("\n<http://arp.ro/resource/agent/").append(museumId).append("> a arp:Museum ;\n");
            sb.append("    arp:name \"").append(escape(muzeuName)).append("\" .\n");
        }

        if (!sourceAgentName.isEmpty()) {
            String sourceId = sourceAgentName.replaceAll("[^a-zA-Z0-9]", "_");

            if (writtenOrganizations.add(sourceId)) {
                sb.append("\n<http://arp.ro/resource/agent/").append(sourceId).append("> a arp:Organization ;\n");
                sb.append("    arp:name \"").append(escape(sourceAgentName)).append("\" ;\n");

                if (!sourceWeb.isEmpty()) {
                    sb.append("    foaf:homepage \"")
                            .append(sourceWeb)
                            .append("\"^^xsd:anyURI ;\n");
                }

                if (!muzeuName.isEmpty()) {
                    sb.append("    prov:actedOnBehalfOf <http://arp.ro/resource/agent/")
                            .append(museumId)
                            .append("> ;\n");
                }

                // închidem corect cu punct
                sb.setLength(sb.length() - 2); // scoate " ;"
                sb.append(" .\n");
            }
        }


        return sb.toString();
    }

    // --- Helpers ---

    // [NOU] Metodă specială pentru a extrage TOATE clasificările din structura complexă lido:classificationWrap
    private static List<String> getClassificationValues(Element element) {
        List<String> results = new ArrayList<>();
        NodeList wraps = element.getElementsByTagName("lido:classificationWrap");
        if (wraps.getLength() > 0) {
            Element wrap = (Element) wraps.item(0);
            NodeList classifications = wrap.getElementsByTagName("lido:classification");
            // Iterăm prin fiecare bloc <lido:classification>
            for (int i = 0; i < classifications.getLength(); i++) {
                Element classif = (Element) classifications.item(i);
                NodeList terms = classif.getElementsByTagName("lido:term");
                // Iterăm prin fiecare <lido:term> din bloc
                for (int j = 0; j < terms.getLength(); j++) {
                    String val = terms.item(j).getTextContent().trim();
                    if (!val.isEmpty()) {
                        results.add(val);
                    }
                }
            }
        }
        return results;
    }

    // Metodă pentru materiale (structura e mai simplă, doar tag-uri repetate)
    private static List<String> getListValues(Element element, String parentTag, String childTag) {
        List<String> results = new ArrayList<>();
        NodeList parents = element.getElementsByTagName(parentTag);
        if (parents.getLength() > 0) {
            Element p = (Element) parents.item(0);
            NodeList children = p.getElementsByTagName(childTag);
            for (int i = 0; i < children.getLength(); i++) {
                String val = children.item(i).getTextContent().trim();
                if (!val.isEmpty()) results.add(val);
            }
        }
        return results;
    }

    private static String getDeepNestedValue(Element element, String tag1, String tag2, String tag3) {
        NodeList l1 = element.getElementsByTagName(tag1);
        if (l1.getLength() > 0) {
            Element e1 = (Element) l1.item(0);
            NodeList l2 = e1.getElementsByTagName(tag2);
            if (l2.getLength() > 0) {
                Element e2 = (Element) l2.item(0);
                NodeList l3 = e2.getElementsByTagName(tag3);
                if (l3.getLength() > 0) return l3.item(0).getTextContent().trim();
            }
        }
        return "";
    }

    private static String getSimpleTagValue(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        return list.getLength() > 0 ? list.item(0).getTextContent().trim() : "";
    }

    private static String getNestedValue(Element element, String parentTag, String childTag) {
        NodeList parents = element.getElementsByTagName(parentTag);
        if (parents.getLength() > 0) {
            Element p = (Element) parents.item(0);
            NodeList children = p.getElementsByTagName(childTag);
            if (children.getLength() > 0) return children.item(0).getTextContent().trim();
        }
        return "";
    }

    private static List<String> getCultureList(Element lidoRecord) {
        List<String> cultureList = new ArrayList<>();
        NodeList eventNodes = lidoRecord.getElementsByTagName("lido:event");
        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventElem = (Element) eventNodes.item(i);
            NodeList cultureNodes = eventElem.getElementsByTagName("lido:culture");
            for (int j = 0; j < cultureNodes.getLength(); j++) {
                Element cultureElem = (Element) cultureNodes.item(j);
                NodeList termNodes = cultureElem.getElementsByTagName("lido:term");
                for (int k = 0; k < termNodes.getLength(); k++) {
                    Element termElem = (Element) termNodes.item(k);
                    String val = termElem.getTextContent().trim();
                    String lang = termElem.getAttribute("xml:lang").trim(); // extrage limba
                    if (!val.isEmpty()) {
                        if (!lang.isEmpty()) {
                            cultureList.add(val + "@" + lang); // păstrează limba
                        } else {
                            cultureList.add(val);
                        }
                    }
                }
            }
        }
        return cultureList;
    }

//    private static String escape(String val) {
//        if (val == null) return "";
//        return val.replace("\"", "\\\"").replace("\\", "\\\\").replace("\n", " ").trim();
//    }

    private static void extractMaterialsAndTechniques(Element element, List<String> materials, List<String> techniques) {
        NodeList eventMatTechList = element.getElementsByTagName("lido:eventMaterialsTech");

        for (int i = 0; i < eventMatTechList.getLength(); i++) {
            Element eventNode = (Element) eventMatTechList.item(i);
            NodeList displayNodes = eventNode.getElementsByTagName("lido:displayMaterialsTech");

            for (int j = 0; j < displayNodes.getLength(); j++) {
                Element displayElem = (Element) displayNodes.item(j);
                String value = displayElem.getTextContent().trim();
                // Verificăm atributul lido:label (sau doar label dacă namespaceAware e false)
                String type = displayElem.getAttribute("lido:label");

                if (!value.isEmpty()) {
                    if ("material".equalsIgnoreCase(type)) {
                        materials.add(value);
                    } else if ("technique".equalsIgnoreCase(type)) {
                        techniques.add(value);
                    } else {
                        // Dacă nu are etichetă sau e altceva, putem decide unde să îl punem
                        // De regulă, îl tratăm ca tehnică sau material generic.
                        // Aici îl punem la tehnici ca fallback:
                        techniques.add(value);
                    }
                }
            }
        }
    }

    private static String[] getRecordMetadataDate(Element lidoRecord, String type) {
        NodeList nodes = lidoRecord.getElementsByTagName("lido:recordMetadataDate");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            String elemType = elem.getAttribute("lido:type");
            if (type.equalsIgnoreCase(elemType)) {
                String text = elem.getTextContent().trim();
                if (!text.isEmpty() && text.contains("/")) {
                    String[] parts = text.split("/", 2);
                    String date = parts[0].trim();
                    String name = parts[1].trim();
                    return new String[]{date, name};
                }
            }
        }
        return new String[]{"", ""};
    }



    private static String escape(String val) {
        if (val == null) return "";
        return val
                .replace("\\", "\\\\")   // 1️⃣ backslash primul
                .replace("\"", "\\\"")   // 2️⃣ ghilimele
                .replace("\n", " ")
                .trim();
    }

    private static String getFormattedDateLiteral(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
        String cleanDate = rawDate.trim();
        if (cleanDate.matches("\\d{4}-\\d{2}-\\d{2}")) return "\"" + cleanDate + "\"^^xsd:date";
        if (cleanDate.matches("\\d{4}")) return "\"" + cleanDate + "\"^^xsd:gYear";
        return "\"" + escape(cleanDate) + "\"";
    }
}