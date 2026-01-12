package com.example.backend.config;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleScraper {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36";

    public static String searchImage(String query) {
        try {
            if (query == null || query.trim().isEmpty()) return null;

            String url = "https://www.google.com/search?tbm=isch&q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .timeout(5000)
                    .get();

            String html = doc.html();

            String regex = "\"(https?:\\/\\/[^\"]+?\\.(jpg|png|jpeg))\"";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);

            while (matcher.find()) {
                String imgUrl = matcher.group(1);

                imgUrl = imgUrl.replace("\\u002F", "/");

                if (imgUrl.contains("gstatic.com")) continue;
                if (imgUrl.contains("favicon")) continue;

                // System.out.println("[Google Scraper] Găsit: " + imgUrl);
                return imgUrl;
            }

        } catch (IOException e) {
            System.err.println("⚠️ [Google Scraper] Eroare: " + e.getMessage());
        }
        return null;
    }
}