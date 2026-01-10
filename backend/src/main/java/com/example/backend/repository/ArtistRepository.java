package com.example.backend.repository;
import com.example.backend.model.Artist;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ArtistRepository {

    private final Model artistModel;
    private final Model artworkModel;
    public ArtistRepository(Model artistModel, Model artworkModel) {
        this.artistModel = artistModel;
        this.artworkModel = artworkModel;
    }

    public Artist findByUri(String artistUri) {
        String sparqlTemplate = loadSparql("/sparql/artist-details.sparql");
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
            return artist;
        }
    }

    public List<Artist> findAllArtistWithFirstArtwork() {
        List<Artist> artists = new ArrayList<>();

        String ttlQueryStr = loadSparql("/sparql/artist-first-artwork.sparql");
        Query ttlQuery = QueryFactory.create(ttlQueryStr);

        Model combinedModel = ModelFactory.createUnion(artistModel, artworkModel);

        try (QueryExecution qexec = QueryExecutionFactory.create(ttlQuery, combinedModel)) {
            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();

                Resource artistRes = sol.getResource("artist");
                if (artistRes == null) continue;

                String artistUri = artistRes.getURI();

                // ID compatibil URL
                String[] parts = artistUri.split("/");
                String artistId = parts[parts.length - 1];

                // Nume: Wikidata > name > fallback
                String name =
                        sol.contains("wikidataName") ? sol.getLiteral("wikidataName").getString()
                                : sol.contains("name") ? sol.getLiteral("name").getString()
                                : "Unknown";

                String image = null;
                if (sol.contains("artistImage")) {
                    RDFNode node = sol.get("artistImage");
                    if (node.isResource()) {
                        image = node.asResource().getURI();
                    } else if (node.isLiteral()) {
                        image = node.asLiteral().getString();
                    }
                }

                Artist artist = new Artist();
                artist.uri = artistUri;
                artist.id = artistId;
                artist.name = name;
                artist.imageLink = image;

                artists.add(artist);
            }
        }

        return artists;
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
