package com.example.backend.config;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleScraper {

    // User-Agent de browser real pentru a nu fi blocați de Google
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36";

    public static String searchImage(String query) {
        try {
            if (query == null || query.trim().isEmpty()) return null;

            // System.out.println("🕵️ [Google Scraper] Căutare pentru: " + query);

            String url = "https://www.google.com/search?tbm=isch&q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);

            // Descărcăm HTML-ul
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .timeout(5000)
                    .get();

            String html = doc.html();

            // Căutăm URL-uri de imagini (http...jpg/png) în interiorul scripturilor JSON din pagină
            // Regex-ul caută orice începe cu http, nu conține ghilimele și se termină în extensie de poză
            String regex = "\"(https?:\\/\\/[^\"]+?\\.(jpg|png|jpeg))\"";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);

            while (matcher.find()) {
                String imgUrl = matcher.group(1);

                // Google escapează slash-urile (ex: \u002F), trebuie să le corectăm
                imgUrl = imgUrl.replace("\\u002F", "/");

                // Filtre pentru a elimina iconițe mici sau rezultate irelevante
                if (imgUrl.contains("gstatic.com")) continue; // Thumbnails mici Google
                if (imgUrl.contains("favicon")) continue;

                // System.out.println("✅ [Google Scraper] Găsit: " + imgUrl);
                return imgUrl;
            }

        } catch (IOException e) {
            System.err.println("⚠️ [Google Scraper] Eroare: " + e.getMessage());
        }
        return null;
    }
}