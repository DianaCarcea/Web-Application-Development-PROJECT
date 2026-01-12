package com.example.backend.repository;
import com.example.backend.model.Artist;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Repository
public class ArtistRepository {

    private final Model artistModel;
    private final Model artworkModel;
    private final Model wikiModel;

    public ArtistRepository(Model artistModel, Model artworkModel, Model wikiModel) {
        this.artistModel = artistModel;
        this.artworkModel = artworkModel;
        this.wikiModel = wikiModel;
    }

    public Artist findByUri(String artistUri, String domain) {
        String sparqlTemplate = loadSparql("/sparql/artist-details.sparql");
        String sparql = sparqlTemplate.replace("{{ARTIST_URI}}", artistUri);

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
            Artist artist = new Artist();

            artist.uri = artistUri;
            artist.name = sol.contains("name") ? sol.getLiteral("name").getString() : "";
            artist.wikidataLabel = sol.contains("wikidataName") ? sol.getLiteral("wikidataName").getString() : "";

            String imageArtist = getArtistImage(artistUri);
            artist.imageLink = sol.contains("imageLink") ? sol.getResource("imageLink").getURI() : null;
            if(imageArtist != null) {
                artist.imageLink = imageArtist;
            }
            // ID compatibil URL

            String[] parts = artistUri.split("/");

            artist.id = parts[parts.length - 1];
            return artist;
        }
    }

    public List<Artist> findAllArtistWithFirstArtwork(String domain) {
        List<Artist> artists = new ArrayList<>();

        String ttlQueryStr = loadSparql("/sparql/artist-first-artwork.sparql");
        Query ttlQuery = QueryFactory.create(ttlQueryStr);

        Model modelChosen;
        if(Objects.equals(domain, "ro")) {
            modelChosen = artworkModel;
        } else {
            modelChosen = wikiModel;
        }

        try (QueryExecution qexec = QueryExecutionFactory.create(ttlQuery, modelChosen)) {
            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();

                Resource artistRes = sol.getResource("artist");
                if (artistRes == null) continue;

                String artistUri = artistRes.getURI();
                String imageArtist = getArtistImage(artistUri);

                String[] parts = artistUri.split("/");
                String artistId = parts[parts.length - 1];

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

                if(imageArtist != null) {
                    image = imageArtist;
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


    private String loadSparql(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load SPARQL: " + path, e);
        }
    }

    public List<Artist> findAllArtistWithFirstArtworkHome(String domain, int pageSize, int offset) {


        List<Artist> artists = new ArrayList<>();

        String ttlQueryStr = loadSparql("/sparql/artist-first-artwork-home.sparql")
                .replace("{{LIMIT}}", String.valueOf(pageSize))
                .replace("{{OFFSET}}", String.valueOf(offset));

        Query ttlQuery = QueryFactory.create(ttlQueryStr);

        Model modelChosen;
        if(Objects.equals(domain, "ro")) {
            modelChosen = artworkModel;
        } else {
            modelChosen = wikiModel;
        }

        try (QueryExecution qexec = QueryExecutionFactory.create(ttlQuery, modelChosen)) {
            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();

                Resource artistRes = sol.getResource("artist");
                if (artistRes == null) continue;

                String artistUri = artistRes.getURI();
                String imageArtist = getArtistImage(artistUri);


                // ID compatibil URL
                String[] parts = artistUri.split("/");
                String artistId = parts[parts.length - 1];

                // Wikidata > name > fallback
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

                if(imageArtist != null) {
                    image = imageArtist;
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

    public String getArtistImage(String artistUri) {

        String sparql = loadSparql("/sparql/artist-image-simple.sparql")
                .replace("{{ARTIST_URI}}", artistUri);

        try (QueryExecution qexec = QueryExecutionFactory.create(sparql, artistModel)) {
            ResultSet rs = qexec.execSelect();

            if (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();

                if (sol.contains("image")) {
                    RDFNode node = sol.get("image");

                    if (node.isResource()) {
                        return node.asResource().getURI();
                    } else {
                        return node.asLiteral().getString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
