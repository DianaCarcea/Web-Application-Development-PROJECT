package com.example.backend.config;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TtlToHtmlGenerator {

    public static void main(String[] args) {
        String inputTtl = "output.ttl";     // Fișierul generat anterior
        String outputHtml = "index.html";   // Rezultatul final

        // 1. Încărcăm datele
        Model model = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(model, inputTtl);
            System.out.println("Model RDF încărcat. Total triplete: " + model.size());
        } catch (RiotException e) {
            System.err.println("EROARE RDF/TTL:");
            System.err.println(e.getMessage());

            // INFO EXTINS
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }

            printErrorLine(inputTtl, e);
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputHtml))) {
            writeHtmlHeader(writer);

            // SPARQL rămâne la fel (GROUP_CONCAT e foarte bun aici)
            String queryString =
                    "PREFIX arp: <http://arp.ro/schema#> " +
                            "PREFIX prov: <http://www.w3.org/ns/prov#> " +
                            "PREFIX dct: <http://purl.org/dc/terms/> " +
                            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                            "SELECT ?subject ?title ?img ?category ?condition ?inv ?desc ?cimec ?license " +
                            "       ?date ?dimensions ?medium " +
                            "       (GROUP_CONCAT(DISTINCT ?cls; separator=\"; \") AS ?classifications) " +
                            "       (GROUP_CONCAT(DISTINCT ?tech; separator=\"; \") AS ?techniques) " +
                            "       (GROUP_CONCAT(DISTINCT ?culture; separator=\"; \") AS ?cultures) " +
                            "       ?artistName ?artistUri " +
                            "       ?museumName ?museumUri " +
                            "       ?sourceName ?sourceWeb " +
                            "WHERE { " +
                            "  ?subject a arp:Artwork . " +
                            "  OPTIONAL { ?subject arp:title ?title } " +
                            "  OPTIONAL { ?subject arp:imageLink ?img } " +
                            "  OPTIONAL { ?subject arp:dimensions ?dimensions } " +
                            "  OPTIONAL { ?subject arp:medium ?medium } " +
                            "  OPTIONAL { ?subject arp:category ?category } " +
                            "  OPTIONAL { ?subject arp:condition ?condition } " +
                            "  OPTIONAL { ?subject arp:inventoryNumber ?inv } " +
                            "  OPTIONAL { ?subject arp:description ?desc } " +
                            "  OPTIONAL { ?subject arp:cimecLink ?cimec } " +
                            "  OPTIONAL { ?subject dct:license ?license } " +
                            "  OPTIONAL { ?subject arp:classification ?cls } " +
                            "  OPTIONAL { ?subject arp:culture ?culture } " +
                            "  OPTIONAL { " +
                            "    ?subject prov:wasGeneratedBy ?activity . " +
                            "    OPTIONAL { ?activity arp:startedAtTime ?date } " +
                            "    OPTIONAL { ?activity arp:technique ?tech } " +
                            "    OPTIONAL { ?activity arp:culture ?culture }" +
                            "  } " +
                            "  OPTIONAL { " +
                            "    ?subject prov:wasAttributedTo ?artistUri . " +
                            "    ?artistUri arp:name ?artistName " +
                            "  } " +
                            "  OPTIONAL { " +
                            "    ?subject arp:currentLocation ?museumUri . " +
                            "    ?museumUri arp:name ?museumName " +
                            "  } " +
                            "  OPTIONAL { " +
                            "     ?sourceUri prov:actedOnBehalfOf ?museumUri ; " +
                            "                a arp:Organization ; " +
                            "                arp:name ?sourceName . " +
                            "     OPTIONAL { ?sourceUri foaf:homepage ?sourceWeb } " +
                            "  } " +
                            "} " +
                            "GROUP BY ?subject ?title ?img ?category ?condition ?inv ?desc ?cimec ?license ?date ?dimensions ?medium ?artistName ?artistUri ?museumName ?museumUri ?sourceName ?sourceWeb";

            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();
                int count = 0;
                while (results.hasNext()) {
                    writeArtworkCard(writer, results.nextSolution());
                    count++;
                }
                System.out.println("Generat HTML pentru " + count + " opere.");
            }

            writeHtmlFooter(writer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeArtworkCard(PrintWriter writer, QuerySolution soln) {
        String uri = soln.getResource("subject").getURI();
        String title = getVal(soln, "title");
        String img = getVal(soln, "img");
        String desc = getVal(soln, "desc");

        String classifications = getVal(soln, "classifications");
        String techniques = getVal(soln, "techniques");
        String cultures = getVal(soln, "cultures");

        String date = getVal(soln, "date");
        String dims = getVal(soln, "dimensions");
        String medium = getVal(soln, "medium");
        String category = getVal(soln, "category");
        String condition = getVal(soln, "condition");
        String inv = getVal(soln, "inv");

        String cimec = getVal(soln, "cimec");
        String license = soln.contains("license") ? soln.getResource("license").getURI() : "";

        String artistName = getVal(soln, "artistName");
        String artistUri = soln.contains("artistUri") ? soln.getResource("artistUri").getURI() : "";
        String museumName = getVal(soln, "museumName");
        String museumUri = soln.contains("museumUri") ? soln.getResource("museumUri").getURI() : "";
        String sourceName = getVal(soln, "sourceName");
        String sourceWeb = getVal(soln, "sourceWeb");

        writer.println("<article resource=\"" + uri + "\" typeof=\"arp:Artwork schema:VisualArtwork\" class=\"art-card\">");

        // Header
        writer.println("  <div class=\"card-header\">");
        writer.println("    <h2 property=\"arp:title schema:name\">" + escapeHtml(title) + "</h2>");
        if (!category.isEmpty()) {
            writer.println("    <span class=\"badge category\" property=\"arp:category\">" + escapeHtml(category) + "</span>");
        }
        writer.println("  </div>");

        writer.println("  <div class=\"card-body\">");

        if (!img.isEmpty()) {
            writer.println("    <figure>");
            writer.println("      <img src=\"" + img + "\" alt=\"" + escapeHtml(title) + "\" property=\"arp:imageLink schema:image\" />");
            writer.println("    </figure>");
        }

        writer.println("    <div class=\"details\">");

        if (!desc.isEmpty()) {
            writer.println("      <div class=\"description\">");
            writer.println("        <h4>Descriere & Proveniență</h4>");
            writer.println("        <p property=\"arp:description schema:description\">" + escapeHtml(desc) + "</p>");
            writer.println("      </div>");
        }

        writer.println("      <table class=\"meta-table\">");

        if (!artistName.isEmpty()) {
            writer.println("        <tr property=\"prov:wasAttributedTo schema:creator\" resource=\"" + artistUri + "\" typeof=\"arp:Artist\">");
            writer.println("          <th>Artist</th>");
            writer.println("          <td property=\"arp:name schema:name\">" + escapeHtml(artistName) + "</td>");
            writer.println("        </tr>");
        }

        if (!date.isEmpty()) {
            writer.println("        <tr property=\"prov:wasGeneratedBy\" typeof=\"arp:Creation\">");
            writer.println("          <th>Datare</th>");
            writer.println("          <td property=\"arp:startedAtTime schema:dateCreated\">" + escapeHtml(date) + "</td>");
            writer.println("        </tr>");
        }

        if (!cultures.isEmpty()) {
            writer.println("        <tr><th>Cultură / Școală</th><td>" + formatAsTags(cultures, "arp:culture") + "</td></tr>");
        }

        // --- SECȚIUNEA MODIFICATĂ PENTRU TEHNICI/MATERIALE ---
        if (!techniques.isEmpty() || !medium.isEmpty()) {
            writer.println("        <tr><th>Tehnică/Material</th><td>");

            // Folosim funcția formatAsTags pentru a le separa
            if (!techniques.isEmpty()) {
                writer.println("<div class=\"tech-group\"><strong></strong> " +
                        formatAsTags(techniques, "arp:technique") + "</div>");
            }
            if (!medium.isEmpty()) {
                writer.println("<div class=\"tech-group\"><strong>Material:</strong> " +
                        formatAsTags(medium, "arp:medium") + "</div>");
            }
            writer.println("        </td></tr>");
        }
        // -----------------------------------------------------

        if (!dims.isEmpty()) writer.println("<tr><th>Dimensiuni</th><td property=\"arp:dimensions\">" + escapeHtml(dims) + "</td></tr>");
        if (!inv.isEmpty()) writer.println("<tr><th>Nr. Inventar</th><td property=\"arp:inventoryNumber\">" + escapeHtml(inv) + "</td></tr>");
        if (!condition.isEmpty()) writer.println("<tr><th>Stare</th><td property=\"arp:condition\">" + escapeHtml(condition) + "</td></tr>");

        // --- SI AICI PENTRU CLASIFICĂRI ---
        if (!classifications.isEmpty()) {
            writer.println("        <tr><th>Clasificări</th><td>" + formatAsTags(classifications, "arp:classification") + "</td></tr>");
        }

        if (!museumName.isEmpty()) {
            writer.println("        <tr property=\"arp:currentLocation\" resource=\"" + museumUri + "\" typeof=\"arp:Museum\">");
            writer.println("          <th>Deținător</th>");
            writer.println("          <td property=\"arp:name\">" + escapeHtml(museumName) + "</td>");
            writer.println("        </tr>");
        }

        writer.println("      </table>");

        writer.println("      <div class=\"links\">");
        if (!cimec.isEmpty()) {
            writer.println("        <a href=\"" + cimec + "\" target=\"_blank\" property=\"arp:cimecLink\">Vezi pe CIMEC</a>");
        }
        if (!license.isEmpty()) {
            writer.println("        <a href=\"" + license + "\" target=\"_blank\" property=\"dct:license\">Licență (Europeana)</a>");
        }
        if (!sourceName.isEmpty()) {
            String href = sourceWeb.isEmpty() ? "#" : sourceWeb;
            writer.println("        <span class=\"source\">Sursa: <a href=\"" + href + "\" target=\"_blank\">" + escapeHtml(sourceName) + "</a></span>");
        }
        writer.println("      </div>");

        writer.println("    </div>");
        writer.println("  </div>");
        writer.println("</article>");
    }

    private static String formatAsTags(String rawValue, String rdfProperty) {
        if (rawValue == null || rawValue.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        String[] parts = rawValue.split("; "); // Sparge șirul la punct și virgulă

        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // Creăm un SPAN pentru fiecare element
                sb.append("<span class=\"tech-tag\" property=\"").append(rdfProperty).append("\">")
                        .append(escapeHtml(part))
                        .append("</span> ");
            }
        }
        return sb.toString();
    }

    private static String getVal(QuerySolution soln, String var) {
        if (soln.contains(var)) {
            if (soln.get(var).isLiteral()) return soln.getLiteral(var).getString();
            if (soln.get(var).isResource()) return soln.getResource(var).getURI();
        }
        return "";
    }

    private static void writeHtmlHeader(PrintWriter writer) {
        writer.println("<!DOCTYPE html>");
        writer.println("<html lang='ro'>");
        writer.println("<head><meta charset='UTF-8'><title>Catalog Digital ArP</title>");
        writer.println("<style>");
        writer.println(" body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #eef2f3; padding: 20px; max-width: 1000px; margin: 0 auto; color: #333; }");
        writer.println(" h1 { text-align: center; color: #2c3e50; margin-bottom: 40px; }");
        writer.println(" .art-card { background: white; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); margin-bottom: 30px; overflow: hidden; border: 1px solid #e0e0e0; }");
        writer.println(" .card-header { background: #2c3e50; color: white; padding: 15px 25px; display: flex; justify-content: space-between; align-items: center; }");
        writer.println(" .card-header h2 { margin: 0; font-size: 1.4em; font-weight: 500; }");
        writer.println(" .badge { background: #e74c3c; color: white; padding: 4px 12px; border-radius: 20px; font-size: 0.85em; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }");
        writer.println(" .card-body { display: flex; flex-wrap: wrap; padding: 25px; gap: 30px; }");
        writer.println(" figure { flex: 0 0 300px; margin: 0; }");
        writer.println(" img { width: 100%; border-radius: 6px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        writer.println(" .details { flex: 1; min-width: 300px; }");

        // Stiluri pentru Descriere
        writer.println(" .description { background: #f8f9fa; padding: 15px; border-left: 4px solid #3498db; margin-bottom: 25px; border-radius: 0 4px 4px 0; }");
        writer.println(" .description h4 { margin: 0 0 8px 0; color: #3498db; font-size: 0.9em; text-transform: uppercase; }");
        writer.println(" .description p { margin: 0; line-height: 1.5; color: #555; }");

        // Stiluri pentru Tabel
        writer.println(" .meta-table { width: 100%; border-collapse: separate; border-spacing: 0; }");
        writer.println(" .meta-table th { text-align: left; padding: 10px 0; border-bottom: 1px solid #eee; width: 130px; color: #7f8c8d; font-weight: 600; vertical-align: top; }");
        writer.println(" .meta-table td { padding: 10px 0; border-bottom: 1px solid #eee; vertical-align: top; }");

        // --- STILURI NOI PENTRU TAG-URI (Tehnici/Materiale) ---
        writer.println(" .tech-group { margin-bottom: 5px; }");
        writer.println(" .tech-tag { display: inline-block; background: #ecf0f1; color: #2c3e50; padding: 3px 10px; border-radius: 15px; font-size: 0.9em; margin-right: 5px; margin-bottom: 5px; border: 1px solid #bdc3c7; }");
        // -----------------------------------------------------

        writer.println(" .links { margin-top: 25px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }");
        writer.println(" .links a { text-decoration: none; color: white; background: #3498db; padding: 8px 16px; border-radius: 4px; font-size: 0.9em; transition: background 0.2s; }");
        writer.println(" .links a:hover { background: #2980b9; }");
        writer.println(" .source { font-size: 0.85em; color: #95a5a6; margin-left: auto; }");
        writer.println(" .source a { background: none; color: #7f8c8d; padding: 0; }");
        writer.println("</style>");
        writer.println("</head>");
        writer.println("<body prefix='arp: http://arp.ro/schema# schema: http://schema.org/ prov: http://www.w3.org/ns/prov# dct: http://purl.org/dc/terms/'>");
        writer.println("<h1>Catalog Digital - Linked Data & Provenance</h1>");
    }

    private static void writeHtmlFooter(PrintWriter writer) {
        writer.println("</body></html>");
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void printErrorLine(String file, RiotException e) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(file));

            // Extragem linia din mesaj
            String msg = e.getMessage();
            int lineNumber = -1;

            // caută "line: xxxx"
            int idx = msg.indexOf("line:");
            if (idx != -1) {
                String sub = msg.substring(idx + 5).trim();
                String number = sub.split(",")[0].trim();
                lineNumber = Integer.parseInt(number);
            }

            if (lineNumber > 0 && lineNumber <= lines.size()) {
                System.err.println("\n>>> LINIA CU EROARE (" + lineNumber + "):");
                System.err.println(lines.get(lineNumber - 1));

                System.err.println("\n>>> LINIA ANTERIOARĂ:");
                if (lineNumber > 1)
                    System.err.println(lines.get(lineNumber - 2));

                System.err.println("\n>>> LINIA URMĂTOARE:");
                if (lineNumber < lines.size())
                    System.err.println(lines.get(lineNumber));
            }
        } catch (Exception ex) {
            System.err.println("Nu am putut citi fișierul pentru debug.");
        }
    }
}