package com.example.backend.config;

public class ImageOrchestrator {

    // Imaginea default locală dacă nu găsim nimic pe net
    // Asigură-te că fișierul există în folderul static al proiectului (src/main/resources/static/images/...)
    // Dacă e URL web, pune un link direct către o imagine placeholder
    private static final String DEFAULT_IMG = "/images/no-img.png";

    /**
     * Găsește cea mai bună imagine folosind strategia Waterfall:
     * 1. Wikipedia PageImages (Rapid, Legal, Precis)
     * 2. Google Images Scraper (Puternic, Fără API Key)
     * 3. Wikimedia Commons (Fallback)
     * 4. Imagine Default
     */
    public static String getBestImage(String query) {

        if (query == null || query.trim().isEmpty()) return DEFAULT_IMG;

        String img = null;

        // 1. Wikipedia
        img = WikimediaUtils.searchWikipediaPageImage(query);
        if (img != null) return fixCommonsUrl(img);

        // 2. Google
        img = GoogleScraper.searchImage(query);
        if (img != null) {

            return fixCommonsUrl(img);
        }

        // 3. Commons (Aici va returna link-ul cu Special:Redirect)
        img = WikimediaUtils.searchCommonsFile(query);

        // 4. Verificare finală
        if (img != null) {
            return fixCommonsUrl(img);
        }

        return DEFAULT_IMG;
    }

    // --- METODA DE FIXARE (SAFETY NET) ---
    // Apelează asta și în SparqlToTTLArp.java dacă link-ul vine din baza de date
    public static String fixCommonsUrl(String url) {
        if (url == null || url.isEmpty()) return "src/main/resources/images/no-img.png";

        String filename = null;

        // CAZUL 1: Este fișier TIFF? (Browserele nu afișează TIFF, deci trebuie convertit forțat)
        if (url.toLowerCase().endsWith(".tif") || url.toLowerCase().endsWith(".tiff")) {
            // Extragem numele fișierului indiferent de structura URL-ului
            // Ex: .../Special:FilePath/Tempio%20di.tif -> Tempio%20di.tif
            filename = url.substring(url.lastIndexOf("/") + 1);
        }
        // CAZUL 2: Este link către pagina HTML (/wiki/File:)?
        else if (url.contains("/wiki/File:")) {
            // Ex: .../wiki/File:Nume.jpg -> Nume.jpg
            filename = url.substring(url.lastIndexOf("File:") + 5);
        }

        // Dacă am detectat că trebuie reparat (avem un filename extras)
        if (filename != null) {
            // Construim link-ul de redirect.
            // Parametrul '&width=800' este MAGIC: obligă Wikimedia să convertească TIF în JPG
            return "https://commons.wikimedia.org/w/index.php?title=Special:Redirect/file/" + filename + "&width=800";
        }

        // CAZUL 3: Link direct JPG/PNG sau Special:FilePath (care nu e TIFF)
        // Pe acestea le lăsăm așa cum sunt, browserul le poate afișa.
        return url;
    }
}