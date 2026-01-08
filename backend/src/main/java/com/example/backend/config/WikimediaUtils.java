package com.example.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WikimediaUtils {

    public static String getWikimediaImage(String query) {
        try {
            // URL encode pentru termenul de căutare
            String encodedQuery = URLEncoder.encode(query + " filetype:jpg|png|svg", StandardCharsets.UTF_8);

            // Căutare în namespace 6 (fișiere)
            String searchUrl = "https://commons.wikimedia.org/w/api.php?action=query"
                    + "&list=search&srsearch=" + encodedQuery
                    + "&srnamespace=6&srlimit=5&format=json";

            URL url = new URL(searchUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Java:WikimediaImageSearch:1.0 (your_email@example.com)");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            in.close();
            conn.disconnect();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode searchResults = mapper.readTree(content.toString())
                    .path("query").path("search");

            if (searchResults.isArray() && searchResults.size() > 0) {
                // Iterăm prin primele 5 rezultate
                for (JsonNode fileNode : searchResults) {
                    String fileTitle = fileNode.path("title").asText();
                    String encodedTitle = URLEncoder.encode(fileTitle, StandardCharsets.UTF_8);

                    // Obținem informațiile fișierului
                    String fileApiUrl = "https://commons.wikimedia.org/w/api.php?action=query&titles="
                            + encodedTitle
                            + "&prop=imageinfo&iiprop=url|mime&format=json";

                    URL fileUrl = new URL(fileApiUrl);
                    HttpURLConnection fileConn = (HttpURLConnection) fileUrl.openConnection();
                    fileConn.setRequestMethod("GET");
                    fileConn.setRequestProperty("User-Agent", "Java:WikimediaImageSearch:1.0 (your_email@example.com)");

                    BufferedReader fileIn = new BufferedReader(new InputStreamReader(fileConn.getInputStream()));
                    StringBuilder fileContent = new StringBuilder();
                    while ((line = fileIn.readLine()) != null) {
                        fileContent.append(line);
                    }
                    fileIn.close();
                    fileConn.disconnect();

                    JsonNode fileRoot = mapper.readTree(fileContent.toString());
                    JsonNode pagesNode = fileRoot.path("query").path("pages");
                    String pageId = pagesNode.fieldNames().next();
                    JsonNode imageinfo = pagesNode.path(pageId).path("imageinfo");

                    if (imageinfo.isArray() && imageinfo.size() > 0) {
                        String mime = imageinfo.get(0).path("mime").asText();
                        if (mime.startsWith("image/")) { // acceptăm doar imagini
                            String imageUrl = imageinfo.get(0).path("url").asText();
                            System.out.println("URL-ul imaginii: " + imageUrl);
                            return imageUrl;
                        }
                    }
                }

                // Dacă niciuna nu e imagine
                System.out.println("Nu s-a găsit nicio imagine validă pentru termenul dat.");
                return "src/main/resources/images/no-img.png";
            } else {
                System.out.println("Nu s-au găsit rezultate pentru termenul de căutare.");
                return "src/main/resources/images/no-img.png";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "src/main/resources/images/no-img.png";
    }
}
