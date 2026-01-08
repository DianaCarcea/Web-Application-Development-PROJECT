package com.example.backend.repository;

//import com.example.backend.config.WikimediaUtils;
import com.example.backend.config.WikimediaUtils;
import com.example.backend.model.Artwork;
import com.example.backend.model.Creation;
import com.example.backend.model.Registrar;
import com.example.backend.model.Validator;
import org.apache.jena.base.Sys;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Repository
public class WikidataArtworkRepository {


    public List<Artwork> findAllHome() {

        String sparql = loadSparql("/sparql/wikidata-artwork-find-all-home.sparql");

        List<Artwork> artworks = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionHTTP.service("https://query.wikidata.org/sparql")
                .query(sparql)
                .build()) {

            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                Artwork a = new Artwork();
                Creation creation = new Creation();


                // URI și ID
                a.uri = getResourceUri(sol, "artwork");
                a.id = a.uri.isEmpty() ? "" : a.uri.substring(a.uri.lastIndexOf("/") + 1);

                // Titlu
                a.title = getLiteral(sol, "title");

                // Imagine
                String wikidataImage = getResourceUri(sol, "imageSample");
                if (wikidataImage != null && !wikidataImage.isEmpty()) {
                    a.imageLink = wikidataImage;
                } else {
                    a.imageLink = WikimediaUtils.getWikimediaImage(a.title);
                }

                artworks.add(a);

            }
        }

        return artworks;

    }


    public List<Artwork> findAll() {
        String sparql = loadSparql("/sparql/wikidata-artwork-find-all.sparql");

        List<Artwork> artworks = new ArrayList<>();

        try (QueryExecution qexec = QueryExecutionHTTP.service("https://query.wikidata.org/sparql")
                .query(sparql)
                .build()) {

            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                Artwork a = new Artwork();
                Creation creation = new Creation();

                // --- Mapare câmpuri pentru modelul tău ---
                // URI și ID
                a.uri = getResourceUri(sol, "artwork");
                a.id = a.uri.isEmpty() ? "" : a.uri.substring(a.uri.lastIndexOf("/") + 1);

                // Titlu
                a.title = getLiteral(sol, "title");

                // Imagine
                String wikidataImage = getResourceUri(sol, "imageSample");

                // dacă Wikidata nu are imagine
                if (wikidataImage != null && !wikidataImage.isEmpty()) {
                    a.imageLink = wikidataImage;
                } else {
                    a.imageLink = WikimediaUtils.getWikimediaImage(a.title); // caută pe Wikimedia
                }

                // Descriere
                a.description = getLiteral(sol, "descriptionSample"); // Wikidata nu are descriere direct, poți lăsa gol sau să faci alt query

                // Dimensiuni
                String heightValue = getLiteral(sol, "heightValueSample");
                String heightUnit  = getLiteral(sol, "heightUnitSample");

                String widthValue  = getLiteral(sol, "widthValueSample");
                String widthUnit   = getLiteral(sol, "widthUnitSample");

                String height = "";
                String width  = "";

                if (heightValue != null && !heightValue.isEmpty()) {
                    height = heightValue + (heightUnit != null && !heightUnit.isEmpty() ? " " + heightUnit : "");
                }

                if (widthValue != null && !widthValue.isEmpty()) {
                    width = widthValue + (widthUnit != null && !widthUnit.isEmpty() ? " " + widthUnit : "");
                }

                a.dimensions = (!width.isEmpty() && !height.isEmpty())
                        ? width + " × " + height
                        : "";

                // Materiale folosite
                a.materialsUsed = new ArrayList<>();
                if (!getLiteral(sol, "materialLabel").isEmpty()) {
                    a.materialsUsed.add(getLiteral(sol, "materialLabel"));
                }

                // Tip / Clasificare
                a.classification = new ArrayList<>();

                addIfNotEmpty(a.classification, getLiteral(sol, "artFormSample"));     // drawing
                addIfNotEmpty(a.classification, getLiteral(sol, "genreSample"));      // portrait
                addIfNotEmpty(a.classification, getLiteral(sol, "instanceSample"));   // wall painting
                addIfNotEmpty(a.classification, getLiteral(sol, "subclassSample"));   // fine art

                // Artist
                if (!getLiteral(sol, "artistNameSample").isEmpty()) {
                    a.artist = new com.example.backend.model.Artist();
                    a.artist.name = getLiteral(sol, "artistNameSample");
                    a.artist.uri = getResourceUri(sol, "artistUriSample");
                }

                // Muzeu / Locație
                if (!getLiteral(sol, "museumName").isEmpty()) {
                    a.currentLocation = new com.example.backend.model.Agent();
                    a.currentLocation.name = getLiteral(sol, "museumName");
                    a.currentLocation.uri = getResourceUri(sol, "museum");
                }

                String inception = getLiteral(sol, "inceptionSample"); // poate fi null
                String startTime = getLiteral(sol, "startTimeSample"); // poate fi null
                String pointInTime = getLiteral(sol, "pointInTimeSample");
                String publicationDate = getLiteral(sol, "publicationDateSample"); // poate fi null


                // folosește inception dacă există, altfel fallback la startTime
                String year = null;
                if (inception != null && !inception.isEmpty()) {
                    // dacă e în format ISO (1712-01-01T00:00:00Z)
                    if (inception.contains("T")) {
                        year = inception.substring(0, 4);
                    } else {
                        // dacă e doar label numeric, extrage cifrele
                        inception = inception.replaceAll("[^0-9]", "");
                        if (inception.length() >= 4) {
                            year = inception.substring(0, 4);
                        } else {
                            year = inception; // dacă e mai scurt, ia tot ce e
                        }
                    }
                } else if (startTime != null && !startTime.isEmpty()) {
                    // fallback: ia primele 4 cifre din startTimeLabel
                    startTime = startTime.replaceAll("[^0-9]", "");
                    if (startTime.length() >= 4) {
                        year = startTime.substring(0, 4);
                    } else {
                        year = startTime;
                    }
                } else if (pointInTime != null && !pointInTime.isEmpty()) {
                    // Extrage primul grup de 4 cifre → anul
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(\\d{4})\\b").matcher(pointInTime);
                    if (matcher.find()) {
                        year = matcher.group(1);
                    } else {
                        year = null; // dacă nu găsește cifre, poți lăsa null
                    }
                } else if (publicationDate != null && !publicationDate.isEmpty()) {
                    if (publicationDate.contains("T")) {
                        year = publicationDate.substring(0, 4);
                    } else {
                        publicationDate = publicationDate.replaceAll("[^0-9]", "");
                        if (publicationDate.length() >= 4) {
                            year = publicationDate.substring(0, 4);
                        } else {
                            year = publicationDate;
                        }
                    }
                }


                String condition = getLiteral(sol, "conditionSample");

                if (condition != null && !condition.isEmpty()) {
                    a.condition = condition; // ex: "good condition"
                }

                // setează în obiect
                creation.startedAtTime = year;


                // Alte câmpuri ARP care nu există în Wikidata
                a.inventoryNumber = "";
                a.category = "";
//                a.condition = "";
                a.cimecLink = "";
                a.license = "";
                a.cultures = new ArrayList<>();
                a.techniques = new ArrayList<>();
                a.recordedAt = "";
                a.validatedAt = "";
                a.registrar = null;
                a.validator = null;
                a.creation = creation;

                artworks.add(a);
            }
        }
        return artworks;
    }

    public Artwork findById(String qId) {
        String sparql = loadSparql("/sparql/wikidata-artwork-findby-id.sparql");

        sparql = sparql.replace("Q_ID_PLACEHOLDER", qId);

        try (QueryExecution qexec = QueryExecutionHTTP.service("https://query.wikidata.org/sparql")
                .query(sparql)
                .build()) {

            ResultSet rs = qexec.execSelect();

            if (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                Artwork a = new Artwork();
                Creation creation = new Creation();

                // --- Mapare câmpuri pentru modelul tău ---
                // URI și ID
                a.uri = getResourceUri(sol, "artwork");
                a.id = a.uri.isEmpty() ? "" : a.uri.substring(a.uri.lastIndexOf("/") + 1);

                // Titlu
                a.title = getLiteral(sol, "title");

                // Imagine
                String wikidataImage = getResourceUri(sol, "image");

                // dacă Wikidata nu are imagine
                if (wikidataImage != null && !wikidataImage.isEmpty()) {
                    a.imageLink = wikidataImage;
                } else {
                    a.imageLink = WikimediaUtils.getWikimediaImage(a.title); // caută pe Wikimedia
                }

                // Descriere
                a.description = getLiteral(sol, "description"); // Wikidata nu are descriere direct, poți lăsa gol sau să faci alt query

                // Dimensiuni
                String heightValue = getLiteral(sol, "heightValue");
                String heightUnit  = getLiteral(sol, "heightUnitLabel");

                String widthValue  = getLiteral(sol, "widthValue");
                String widthUnit   = getLiteral(sol, "widthUnitLabel");

                String height = "";
                String width  = "";

                if (heightValue != null && !heightValue.isEmpty()) {
                    height = heightValue + (heightUnit != null && !heightUnit.isEmpty() ? " " + heightUnit : "");
                }

                if (widthValue != null && !widthValue.isEmpty()) {
                    width = widthValue + (widthUnit != null && !widthUnit.isEmpty() ? " " + widthUnit : "");
                }

                a.dimensions = (!width.isEmpty() && !height.isEmpty())
                        ? width + " × " + height
                        : "";

                // Materiale folosite
                a.materialsUsed = getListFromConcat(sol, "materialLabels");

                // Tip / Clasificare
                a.classification = new ArrayList<>();

                a.classification.addAll(getListFromConcat(sol, "artFormLabels"));   // drawing
                a.classification.addAll(getListFromConcat(sol, "genreLabels"));     // portrait
                a.classification.addAll(getListFromConcat(sol, "instanceLabels"));  // wall painting
                a.classification.addAll(getListFromConcat(sol, "subclassLabels"));  // fine art


                // Artist
                if (!getLiteral(sol, "artistName").isEmpty()) {
                    a.artist = new com.example.backend.model.Artist();
                    a.artist.name = getLiteral(sol, "artistName");
                    a.artist.uri = getResourceUri(sol, "artist");
                }

                // Muzeu / Locație
                if (!getLiteral(sol, "locationName").isEmpty()) {
                    a.currentLocation = new com.example.backend.model.Agent();
                    a.currentLocation.name = getLiteral(sol, "locationName");
                    a.currentLocation.uri = getResourceUri(sol, "location");
                }

                String inception = getLiteral(sol, "inception"); // poate fi null
                String startTime = getLiteral(sol, "startTime"); // poate fi null
                String pointInTime = getLiteral(sol, "pointInTime");
                String publicationDate = getLiteral(sol, "publicationDate"); // poate fi null


                // folosește inception dacă există, altfel fallback la startTime
                String year = null;
                if (inception != null && !inception.isEmpty()) {
                    // dacă e în format ISO (1712-01-01T00:00:00Z)
                    if (inception.contains("T")) {
                        year = inception.substring(0, 4);
                    } else {
                        // dacă e doar label numeric, extrage cifrele
                        inception = inception.replaceAll("[^0-9]", "");
                        if (inception.length() >= 4) {
                            year = inception.substring(0, 4);
                        } else {
                            year = inception; // dacă e mai scurt, ia tot ce e
                        }
                    }
                } else if (startTime != null && !startTime.isEmpty()) {
                    // fallback: ia primele 4 cifre din startTimeLabel
                    startTime = startTime.replaceAll("[^0-9]", "");
                    if (startTime.length() >= 4) {
                        year = startTime.substring(0, 4);
                    } else {
                        year = startTime;
                    }
                } else if (pointInTime != null && !pointInTime.isEmpty()) {
                    // Extrage primul grup de 4 cifre → anul
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(\\d{4})\\b").matcher(pointInTime);
                    if (matcher.find()) {
                        year = matcher.group(1);
                    } else {
                        year = null; // dacă nu găsește cifre, poți lăsa null
                    }
                } else if (publicationDate != null && !publicationDate.isEmpty()) {
                    if (publicationDate.contains("T")) {
                        year = publicationDate.substring(0, 4);
                    } else {
                        publicationDate = publicationDate.replaceAll("[^0-9]", "");
                        if (publicationDate.length() >= 4) {
                            year = publicationDate.substring(0, 4);
                        } else {
                            year = publicationDate;
                        }
                    }
                }


                String condition = getLiteral(sol, "conditionLabel");

                if (condition != null && !condition.isEmpty()) {
                    a.condition = condition; // ex: "good condition"
                }

                // setează în obiect
                creation.startedAtTime = year;


                // Alte câmpuri ARP care nu există în Wikidata
                a.inventoryNumber = getLiteral(sol, "inventoryNumbers");
                a.category = "";
//                a.condition = "";
                a.cimecLink = "";
                a.license = "";
                a.cultures = new ArrayList<>();
                a.techniques = new ArrayList<>();
                a.recordedAt = "";
                a.validatedAt = "";
                a.registrar = null;
                a.validator = null;
                a.creation = creation;

                return a;
            }
        }
        return null;
    }

    // --- Helpers ---
    private String getLiteral(QuerySolution sol, String var) {
        return sol.contains(var) && sol.get(var).isLiteral() ? sol.getLiteral(var).getString() : "";
    }

    private String getResourceUri(QuerySolution sol, String var) {
        return sol.contains(var) && sol.get(var).isResource() ? sol.getResource(var).getURI() : "";
    }

    private String loadSparql(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load SPARQL: " + path, e);
        }
    }

    private void addIfNotEmpty(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty()) {
            list.add(value);
        }
    }

    private List<String> getListFromConcat(QuerySolution sol, String varName) {
        String concat = getLiteral(sol, varName);
        if (concat == null || concat.isEmpty()) return new ArrayList<>();
        String[] items = concat.split("\\|"); // separatorul folosit în SPARQL
        List<String> list = new ArrayList<>();
        for (String item : items) {
            if (!item.isEmpty()) list.add(item.trim());
        }
        return list;
    }

}
