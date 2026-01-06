package com.example.backend.repository;

import com.example.backend.model.Artwork;
import com.example.backend.model.Creation;
import org.apache.jena.base.Sys;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ArtworkRepository {

    private final Model rdfModel;

    public ArtworkRepository(Model rdfModel) {
        this.rdfModel = rdfModel;
    }

    public List<Artwork> findAll() {
        String sparql = """
            PREFIX arp: <http://arp.ro/schema#>
            PREFIX prov: <http://www.w3.org/ns/prov#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX foaf: <http://xmlns.com/foaf/0.1/>
            
            SELECT ?subject ?title ?img ?category ?condition ?inv ?desc ?cimec ?license 
                   ?date ?dimensions 
                   (GROUP_CONCAT(DISTINCT ?cls; separator="; ") AS ?classifications) 
                   (GROUP_CONCAT(DISTINCT ?tech; separator="; ") AS ?techniques) 
                   (GROUP_CONCAT(DISTINCT ?culture; separator="; ") AS ?cultures)
                   (GROUP_CONCAT(DISTINCT ?mat; separator="; ") AS ?materialsUsed)  
                   ?artistName ?artistUri 
                   ?museumName ?museumUri 
            WHERE {
              ?subject a arp:Artwork .
              OPTIONAL { ?subject arp:title ?title }
              OPTIONAL { ?subject arp:imageLink ?img }
              OPTIONAL { ?subject arp:dimensions ?dimensions }
              OPTIONAL { ?subject arp:category ?category }
              OPTIONAL { ?subject arp:condition ?condition }
              OPTIONAL { ?subject arp:inventoryNumber ?inv }
              OPTIONAL { ?subject arp:description ?desc }
              OPTIONAL { ?subject arp:cimecLink ?cimec }
              OPTIONAL { ?subject dct:license ?license }
              OPTIONAL { ?subject arp:classification ?cls }
              OPTIONAL { ?subject arp:culture ?culture }
              OPTIONAL {
                  ?subject prov:wasGeneratedBy ?activity .
                  OPTIONAL { ?activity arp:startedAtTime ?date }
                  OPTIONAL { ?activity arp:technique ?tech }
                  OPTIONAL { ?activity arp:culture ?culture }
                  OPTIONAL { ?activity arp:materialsUsed ?mat }
              }
              OPTIONAL {
                  ?subject prov:wasAttributedTo ?artistUri .
                  ?artistUri arp:name ?artistName
              }
              OPTIONAL {
                  ?subject arp:currentLocation ?museumUri .
                  ?museumUri arp:name ?museumName
              }
            }
            GROUP BY ?subject ?title ?img ?category ?condition ?inv ?desc ?cimec ?license ?date ?dimensions ?artistName ?artistUri ?museumName ?museumUri
        """;

        Query query = QueryFactory.create(sparql);
        List<Artwork> artworks = new ArrayList<>();

        try (QueryExecution qexec = QueryExecutionFactory.create(query, rdfModel)) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                Artwork a = new Artwork();
                Creation creation = new Creation();

                a.uri = sol.getResource("subject").getURI();
                a.title = getLiteral(sol, "title");
                a.imageLink = getLiteral(sol, "img");
                a.description = getLiteral(sol, "desc");
                a.category = getLiteral(sol, "category");
                a.condition = getLiteral(sol, "condition");
                a.inventoryNumber = getLiteral(sol, "inv");
                a.cimecLink = getLiteral(sol, "cimec");
                a.license = sol.contains("license") ? sol.getResource("license").getURI() : "";
                a.dimensions = getLiteral(sol, "dimensions");

                a.classification = splitConcat(sol, "classifications");
                a.cultures = splitConcat(sol, "cultures");
                a.techniques = splitConcat(sol, "techniques");
                a.materialsUsed = splitConcat(sol, "materialsUsed");

                if (sol.contains("artistName")) {
                    a.artist = new com.example.backend.model.Artist();
                    a.artist.name = getLiteral(sol, "artistName");
                    a.artist.uri = sol.getResource("artistUri").getURI();
                }

                if (sol.contains("museumName")) {
                    a.currentLocation = new com.example.backend.model.Agent();
                    a.currentLocation.name = getLiteral(sol, "museumName");
                    a.currentLocation.uri = sol.getResource("museumUri").getURI();
                }

                artworks.add(a);
            }
        }
        return artworks;
    }

    private String getLiteral(QuerySolution sol, String var) {
        return sol.contains(var) ? sol.getLiteral(var).getString() : "";
    }

    private List<String> splitConcat(QuerySolution sol, String var) {
        List<String> list = new ArrayList<>();
        if (sol.contains(var)) {
            String raw = sol.getLiteral(var).getString();
            for (String s : raw.split(";")) list.add(s.trim());
        }
        return list;
    }
}
