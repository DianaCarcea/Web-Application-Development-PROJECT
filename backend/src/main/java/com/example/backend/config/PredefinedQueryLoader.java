package com.example.backend.config;

import org.springframework.stereotype.Component;  // schimbă importul

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PredefinedQueryLoader {

    private static final String QUERY_PATH = "/predefined-queries/";

    public Map<String, String> getPredefinedQueries() {
        Map<String, String> predefinedQueries = new HashMap<>();

        String[] files = {"artists.sparql","artworks.sparql","artworks_count_by_material.sparql","artworks_by_getty_material.sparql"};

        for (String file : files) {
            try (InputStream is = getClass().getResourceAsStream(QUERY_PATH + file)) {
                if (is != null) {
                    String query = new BufferedReader(new InputStreamReader(is))
                            .lines()
                            .collect(Collectors.joining("\n"));
                    // extrage cheia fără extensie
                    String key = file.replace(".sparql", "");
                    predefinedQueries.put(key, query);
                } else {
                    System.err.println("File not found: " + QUERY_PATH + file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return predefinedQueries;
    }
}
