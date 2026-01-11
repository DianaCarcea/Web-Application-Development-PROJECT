package com.example.backend.repository;

import com.example.backend.model.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class ArtworkRepository {

    private final Model artworkModel;
    private final Model wikiModel;

    public ArtworkRepository(Model artworkModel, Model wikiModel) {
        this.artworkModel = artworkModel;
        this.wikiModel = wikiModel;
    }

    public List<Artwork> findAll(String domain) {
        String sparql = loadSparql("/sparql/artwork-find-all.sparql");
        Query query = QueryFactory.create(sparql);

        List<Artwork> artworks = new ArrayList<>();

        Model modelChosen;
        if(Objects.equals(domain, "ro")) {
            modelChosen = artworkModel;
        } else {
            modelChosen = wikiModel;
        }


        try (QueryExecution qexec = QueryExecutionFactory.create(query, modelChosen)) {
            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                Artwork a = new Artwork();
                Creation creation = new Creation();
                creation.startedAtTime  = getLiteral(sol, "date");
                a.creation = creation;

                a.uri = sol.getResource("subject").getURI();
                a.id = a.uri.substring(a.uri.lastIndexOf("/") + 1);
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

                a.recordedAt = getLiteral(sol, "recordedAt");
                a.validatedAt = getLiteral(sol, "validatedAt");

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

                if (sol.contains("registrarUri")) {
                    Registrar registrar = new Registrar();
                    registrar.uri = sol.getResource("registrarUri").getURI();
                    registrar.name = getLiteral(sol, "registrarName");
                    a.registrar = registrar;
                }

                if (sol.contains("validatorUri")) {
                    Validator validator = new Validator();
                    validator.uri = sol.getResource("validatorUri").getURI();
                    validator.name = getLiteral(sol, "validatorName");
                    a.validator = validator;
                }

                artworks.add(a);
            }
        }
        return artworks;
    }

    public Artwork findByUri(String uri, String domain) {
        String sparqlTemplate = loadSparql("/sparql/artwork-find-by-uri.sparql");

        String sparql = sparqlTemplate.replace("{{URI}}", uri);
        Query query = QueryFactory.create(sparql);

        Model modelChosen;
        if(Objects.equals(domain, "ro")) {
            modelChosen = artworkModel;
        } else {
            modelChosen = wikiModel;
        }

        try (QueryExecution qexec = QueryExecutionFactory.create(query, modelChosen)) {
            ResultSet rs = qexec.execSelect();
            if (!rs.hasNext()) return null;

            QuerySolution sol = rs.nextSolution();
            Artwork a = new Artwork();
            Creation creation = new Creation();
            List<Collector> ownershipHistory = new ArrayList<>();

            creation.startedAtTime  = getLiteral(sol, "date");
            a.creation = creation;
            a.uri = uri;
            a.id = a.uri.substring(a.uri.lastIndexOf("/") + 1);
            a.title = getLiteral(sol, "title");
            a.imageLink = getLiteral(sol, "img");
            a.description = getLiteral(sol, "desc");
            a.category = getLiteral(sol, "category");
            a.condition = getLiteral(sol, "condition");
            a.inventoryNumber = getLiteral(sol, "inv");
            a.cimecLink = getLiteral(sol, "cimec");
            a.wikidataLink = getLiteral(sol, "wikidataLink");
            a.license = sol.contains("license") ? sol.getResource("license").getURI() : "";
            a.dimensions = getLiteral(sol, "dimensions");

            a.classification = splitConcat(sol, "classifications");
            a.cultures = splitConcat(sol, "cultures");
            a.techniques = splitConcat(sol, "techniques");
            a.materialsUsed = splitConcat(sol, "materialsUsed");

            a.recordedAt = getLiteral(sol, "recordedAt");
            a.validatedAt = getLiteral(sol, "validatedAt");

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

            if (sol.contains("registrarUri")) {
                Registrar registrar = new Registrar();
                registrar.uri = sol.getResource("registrarUri").getURI();
                registrar.name = getLiteral(sol, "registrarName");
                a.registrar = registrar;
            }

            if (sol.contains("validatorUri")) {
                Validator validator = new Validator();
                validator.uri = sol.getResource("validatorUri").getURI();
                validator.name = getLiteral(sol, "validatorName");
                a.validator = validator;
            }

            if (sol.contains("ownershipHistory")) {
                String raw = sol.getLiteral("ownershipHistory").getString();

                for (String entry : raw.split(";;")) {
                    String[] parts = entry.split("\\|\\|", -1);

                    Collector o = new Collector();
                    o.ownerName = parts.length > 0 ? parts[0] : "?";
                    o.startedAt = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;
                    o.endedAt = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;

                    if (o.startedAt == null) {
                        o.startedAt = "?";
                    }
                    if (o.endedAt == null) {
                        o.endedAt = "?";
                    }

                    a.ownershipHistory.add(o);
                }
            }

            a.ownershipHistory.sort((o1, o2) -> {
                // Păstrăm entry-urile cu "?" la final
                if ("?".equals(o1.startedAt)) return 1;
                if ("?".equals(o2.startedAt)) return -1;
                return o1.startedAt.compareTo(o2.startedAt);
            });

            return a;
        }
    }

    public List<Artwork> findByArtist(String artistUri) {
        // Încarci template-ul SPARQL din fișier
        String sparqlTemplate = loadSparql("/sparql/artworks-by-artist.sparql");

        // Înlocuiești {{ARTIST_URI}} cu URI-ul artistului
        String sparql = sparqlTemplate.replace("{{ARTIST_URI}}", artistUri);
        Query query = QueryFactory.create(sparql);
        List<Artwork> artworks = new ArrayList<>();

        try (QueryExecution qexec = QueryExecutionFactory.create(query, artworkModel)) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                Artwork a = new Artwork();
                Creation creation = new Creation();
                List<Collector> ownershipHistory = new ArrayList<>();

                creation.startedAtTime  = getLiteral(sol, "date");
                a.creation = creation;
                a.uri = sol.getResource("subject").getURI();
                a.id = a.uri.substring(a.uri.lastIndexOf("/") + 1);
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

                a.recordedAt = getLiteral(sol, "recordedAt");
                a.validatedAt = getLiteral(sol, "validatedAt");

                if (sol.contains("artistName") && sol.getResource("artistUri") != null) {
                    a.artist = new com.example.backend.model.Artist();
                    a.artist.name = getLiteral(sol, "artistName");
                    a.artist.uri = sol.getResource("artistUri").getURI();
                }

                if (sol.contains("museumName")) {
                    a.currentLocation = new com.example.backend.model.Agent();
                    a.currentLocation.name = getLiteral(sol, "museumName");
                    a.currentLocation.uri = sol.getResource("museumUri").getURI();
                }

                if (sol.contains("registrarUri")) {
                    Registrar registrar = new Registrar();
                    registrar.uri = sol.getResource("registrarUri").getURI();
                    registrar.name = getLiteral(sol, "registrarName");
                    a.registrar = registrar;
                }

                if (sol.contains("validatorUri")) {
                    Validator validator = new Validator();
                    validator.uri = sol.getResource("validatorUri").getURI();
                    validator.name = getLiteral(sol, "validatorName");
                    a.validator = validator;
                }

                if (sol.contains("ownershipHistory")) {
                    String raw = sol.getLiteral("ownershipHistory").getString();

                    for (String entry : raw.split(";;")) {
                        String[] parts = entry.split("\\|\\|", -1);

                        Collector o = new Collector();
                        o.ownerName = parts.length > 0 ? parts[0] : "?";
                        o.startedAt = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;
                        o.endedAt = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;

                        if (o.startedAt == null) {
                            o.startedAt = "?";
                        }
                        if (o.endedAt == null) {
                            o.endedAt = "?";
                        }

                        a.ownershipHistory.add(o);
                    }
                }

                a.ownershipHistory.sort((o1, o2) -> {
                    if ("?".equals(o1.startedAt)) return 1;
                    if ("?".equals(o2.startedAt)) return -1;
                    return o1.startedAt.compareTo(o2.startedAt);
                });

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
    private String loadSparql(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load SPARQL: " + path, e);
        }
    }

    public List<Artwork> findNext(int pageSize, int offset, String domain) {


        String sparql = loadSparql("/sparql/artwork-find-next.sparql")
                .replace("{{LIMIT}}", String.valueOf(pageSize))
                .replace("{{OFFSET}}", String.valueOf(offset));


        List<Artwork> results = new ArrayList<>();

        Model modelChosen;
        if(Objects.equals(domain, "ro")) {
            modelChosen = artworkModel;
        } else {
            modelChosen = wikiModel;
        }

        try (QueryExecution qexec = QueryExecutionFactory.create(sparql, modelChosen)) {
            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                Artwork a = new Artwork();
                Creation creation = new Creation();
                creation.startedAtTime  = getLiteral(sol, "date");
                a.creation = creation;
                List<Collector> ownershipHistory = new ArrayList<>();

                a.uri = sol.getResource("subject").getURI();
                a.id = a.uri.substring(a.uri.lastIndexOf("/") + 1);
                a.title = getLiteral(sol, "title");
                a.imageLink = getLiteral(sol, "img");
                a.description = getLiteral(sol, "desc");
                a.category = getLiteral(sol, "category");
                a.condition = getLiteral(sol, "condition");
                a.inventoryNumber = getLiteral(sol, "inv");
                a.cimecLink = getLiteral(sol, "cimec");
                a.wikidataLink = getLiteral(sol, "wikidataLink");
                a.license = sol.contains("license") ? sol.getResource("license").getURI() : "";
                a.dimensions = getLiteral(sol, "dimensions");

                a.classification = splitConcat(sol, "classifications");
                a.cultures = splitConcat(sol, "cultures");
                a.techniques = splitConcat(sol, "techniques");
                a.materialsUsed = splitConcat(sol, "materialsUsed");

                a.recordedAt = getLiteral(sol, "recordedAt");
                a.validatedAt = getLiteral(sol, "validatedAt");

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

                if (sol.contains("registrarUri")) {
                    Registrar registrar = new Registrar();
                    registrar.uri = sol.getResource("registrarUri").getURI();
                    registrar.name = getLiteral(sol, "registrarName");
                    a.registrar = registrar;
                }

                if (sol.contains("validatorUri")) {
                    Validator validator = new Validator();
                    validator.uri = sol.getResource("validatorUri").getURI();
                    validator.name = getLiteral(sol, "validatorName");
                    a.validator = validator;
                }

                if (sol.contains("ownershipHistory")) {
                    String raw = sol.getLiteral("ownershipHistory").getString();

                    for (String entry : raw.split(";;")) {
                        String[] parts = entry.split("\\|\\|", -1);

                        Collector o = new Collector();
                        o.ownerName = parts.length > 0 ? parts[0] : "?";
                        o.startedAt = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;
                        o.endedAt = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;

                        if (o.startedAt == null) {
                            o.startedAt = "?";
                        }
                        if (o.endedAt == null) {
                            o.endedAt = "?";
                        }

                        a.ownershipHistory.add(o);
                    }
                }

                a.ownershipHistory.sort((o1, o2) -> {
                    // Păstrăm entry-urile cu "?" la final
                    if ("?".equals(o1.startedAt)) return 1;
                    if ("?".equals(o2.startedAt)) return -1;
                    return o1.startedAt.compareTo(o2.startedAt);
                });

                results.add(a);
            }
        }
        return results;

    }

    public List<Artwork> getRecommendationsByArtist(String uri, int pageSize, int offset, String domain) {

        // 1. Încarcă și pregătește query-ul
        // Nota: URI-ul trebuie să fie curat, fără < > (le-am pus în fișierul SPARQL)
        String sparql = loadSparql("/sparql/artwork-recommendations.sparql")
                .replace("{{INPUT_URI}}", uri)
                .replace("{{LIMIT}}", String.valueOf(pageSize))
                .replace("{{OFFSET}}", String.valueOf(offset));

        List<Artwork> results = new ArrayList<>();

        // 2. Alege modelul
        Model modelChosen;
        if(Objects.equals(domain, "ro")) {
            modelChosen = artworkModel;
        } else {
            modelChosen = wikiModel; // sau cum se numește modelul tău pentru extern
        }

        // 3. Execută Query-ul
        try (QueryExecution qexec = QueryExecutionFactory.create(sparql, modelChosen)) {
            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();

                // Creăm un obiect Artwork nou (adaptează constructorul la clasa ta)
                Artwork a = new Artwork();

                a.uri = sol.getResource("subject").getURI();
                a.id = a.uri.substring(a.uri.lastIndexOf("/") + 1);
                a.title = getLiteral(sol, "title");
                a.imageLink = getLiteral(sol, "image");



                results.add(a);
            }
        } catch (Exception e) {
            System.err.println("Eroare la obținerea recomandărilor: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }
}
