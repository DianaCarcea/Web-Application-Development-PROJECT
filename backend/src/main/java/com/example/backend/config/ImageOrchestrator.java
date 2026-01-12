package com.example.backend.config;

public class ImageOrchestrator {

    private static final String DEFAULT_IMG = "/images/no-img.png";

    public static String getBestImage(String query) {

        if (query == null || query.trim().isEmpty()) return DEFAULT_IMG;

        String img;

        // 1. Wikipedia
        img = WikimediaUtils.searchWikipediaPageImage(query);
        if (img != null) return fixCommonsUrl(img);

        // 2. Google
        img = GoogleScraper.searchImage(query);
        if (img != null) {

            return fixCommonsUrl(img);
        }

        // 3. Commons (link-ul cu Special:Redirect)
        img = WikimediaUtils.searchCommonsFile(query);

        // 4. Verificare finală
        if (img != null) {
            return fixCommonsUrl(img);
        }

        return DEFAULT_IMG;
    }

    public static String fixCommonsUrl(String url) {
        if (url == null || url.isEmpty()) return "src/main/resources/images/no-img.png";

        String filename = null;

        // CAZUL 1: fișier TIFF
        if (url.toLowerCase().endsWith(".tif") || url.toLowerCase().endsWith(".tiff")) {
            filename = url.substring(url.lastIndexOf("/") + 1);
        }
        // CAZUL 2: Este link (/wiki/File:)?
        else if (url.contains("/wiki/File:")) {
            filename = url.substring(url.lastIndexOf("File:") + 5);
        }

        // Dacă am detectat că trebuie reparat (avem un filename extras)
        if (filename != null) {
            // Construim link-ul de redirect.
            return "https://commons.wikimedia.org/w/index.php?title=Special:Redirect/file/" + filename + "&width=800";
        }

        // CAZUL 3: Link direct JPG/PNG sau Special:FilePath (care nu e TIFF)
        return url;
    }
}