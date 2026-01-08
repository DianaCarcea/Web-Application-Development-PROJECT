package com.example.backend.config;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDFS;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ExtractMaterials {

    public static void main(String[] args) throws Exception {
        // 1. Încarcă fisierul output.ttl
        Model model = RDFDataMgr.loadModel("output.ttl");

        // 2. Creează modelul TTL pentru getty-mapping
        Model mappingModel = ModelFactory.createDefaultModel();
        mappingModel.setNsPrefix("arp", "http://arp.ro/schema#");
        mappingModel.setNsPrefix("aat", "http://vocab.getty.edu/aat/");
        mappingModel.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        mappingModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        // 3. Mapare cuvinte-cheie -> coduri AAT
        Map<String, String> aatMap = new HashMap<>();
        aatMap.put("ulei", "300015050");
        aatMap.put("creion", "300014876");
        aatMap.put("acuarel", "300015056");
        aatMap.put("sanguină", "300015053");
        aatMap.put("peniță", "300014938");
        aatMap.put("tuș", "300014938");
        aatMap.put("lemn", "300015072");
        aatMap.put("bronz", "300015090");
        aatMap.put("marmură", "300015078");
        aatMap.put("teracotă", "300015099");
        aatMap.put("sticlă", "300015062");
        aatMap.put("pastel", "300015051");
        aatMap.put("tempera", "300015057");
        aatMap.put("grafit", "300014876");

        // 4. Iterează materialele din output.ttl
        Property materialsUsed = model.getProperty("http://arp.ro/schema#materialsUsed");
        Property rdfsLabel = mappingModel.createProperty("http://www.w3.org/2000/01/rdf-schema#label");
        StmtIterator iter = model.listStatements(null, materialsUsed, (RDFNode) null);

        while (iter.hasNext()) {
            Statement stmt = iter.next();
            String materialText = stmt.getObject().asLiteral().getString().toLowerCase();

            for (Map.Entry<String, String> entry : aatMap.entrySet()) {
                if (materialText.contains(entry.getKey())) {
                    String aatCode = entry.getValue();
                    String safeName = entry.getKey().replaceAll("\\s+","_");

                    Resource concept = mappingModel.createResource("http://arp.ro/material/" + safeName);
                    concept.addProperty(rdfsLabel, entry.getKey());
                    concept.addProperty(mappingModel.createProperty("http://www.w3.org/2004/02/skos/core#exactMatch"),
                            mappingModel.createResource("http://vocab.getty.edu/aat/" + aatCode));
                }
            }
        }

        // 5. Scrie TTL rezultat
        try (PrintWriter out = new PrintWriter(new FileOutputStream("getty-mappings.ttl"))) {
            mappingModel.write(out, "TURTLE");
        }

        System.out.println("getty-mappings.ttl generat cu succes!");
    }
}
