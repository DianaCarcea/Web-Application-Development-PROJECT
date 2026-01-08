package com.example.backend.repository;
import com.example.backend.model.Artist;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ArtistRepository {

    private final org.apache.jena.rdf.model.Model artistModel;

    public ArtistRepository(org.apache.jena.rdf.model.Model artistModel) {
        this.artistModel = artistModel;
    }

    public List<Artist> findAll() {

        List<Artist> artists = new ArrayList<>();

        // 1️⃣ Query TTL
        String ttlQueryStr = loadSparql("/sparql/artist.sparql");
        Query ttlQuery = QueryFactory.create(ttlQueryStr);

        System.out.println("=== TTL ARTIST QUERY ===");
        System.out.println(ttlQueryStr);

        try (QueryExecution qexec = QueryExecutionFactory.create(ttlQuery, artistModel)) {

            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();


                String originalName = sol.getLiteral("name").getString();
                String artistUri = sol.getResource("artist").getURI();

                System.out.println("TTL artist: " + originalName);

                Artist artist = new Artist();
                artist.uri = artistUri;
                artist.name = originalName;

                // 2️⃣ Transform "Nume, Prenume" → "Prenume Nume"
                String searchName = toWikidataName(originalName);

                // 3️⃣ Query Wikidata
                String wdTemplate = loadSparql("/sparql/wikidata_artist_query.sparql");
                String wdQueryStr = String.format(wdTemplate, searchName);

                System.out.println("Wikidata search: " + searchName);

                try (QueryExecution qexecWd =
                             QueryExecutionHTTP.service("https://query.wikidata.org/sparql", wdQueryStr)) {

                    ResultSet wdRs = qexecWd.execSelect();

                    if (wdRs.hasNext()) {
                        QuerySolution wdSol = wdRs.nextSolution();

                        Resource wdArtist = wdSol.getResource("wikidataArtist");
                        Literal label = wdSol.getLiteral("wikidataArtistLabel");
                        Resource image = wdSol.contains("image")
                                ? wdSol.getResource("image")
                                : null;

                        artist.wikidataId = wdArtist.getURI();
                        artist.wikidataLabel = label.getString();
                        artist.imageLink = image != null ? image.getURI() : null;

                        System.out.println("✔ Wikidata match: " + artist.wikidataLabel);
                    } else {
                        System.out.println("✘ No Wikidata match");
                    }
                }

                artists.add(artist);
            }
        }

        System.out.println("=== TOTAL ARTISTS: " + artists.size() + " ===");
        return artists;
    }

    public Artist findByUri(String artistUri) {
        String sparqlTemplate = loadSparql("/sparql/artist-details.sparql");
        // În SPARQL folosește un placeholder {{ARTIST_URI}}
        String sparql = sparqlTemplate.replace("{{ARTIST_URI}}", artistUri);

        Query query = QueryFactory.create(sparql);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, artistModel)) {
            ResultSet rs = qexec.execSelect();

            if (!rs.hasNext()) return null;

            QuerySolution sol = rs.nextSolution();
            Artist artist = new Artist();
            artist.uri = artistUri;
            artist.name = sol.contains("name") ? sol.getLiteral("name").getString() : "";
            artist.wikidataLabel = sol.contains("wikidataName") ? sol.getLiteral("wikidataName").getString() : "";
            artist.imageLink = sol.contains("imageLink") ? sol.getResource("imageLink").getURI() : null;
//            System.out.println(artist.uri);
//            System.out.println(artist.name);
//            System.out.println(artist.wikidataLabel);
//            System.out.println(artist.imageLink);
            return artist;
        }
    }


    private String toWikidataName(String original) {
        // "Brâncuși, Constantin" → "Constantin Brancusi"
        String[] parts = original.split(",\\s*");
        String name = parts.length == 2 ? parts[1] + " " + parts[0] : original;

        return java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private String loadSparql(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load SPARQL: " + path, e);
        }
    }
}
