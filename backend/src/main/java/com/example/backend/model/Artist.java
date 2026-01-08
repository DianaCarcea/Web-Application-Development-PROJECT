package com.example.backend.model;

import java.util.ArrayList;
import java.util.List;

public class Artist extends Person {
    public String wikidataId;    // ID Wikidata (ex: Q6063117)
    public String wikidataLabel; // Label de pe Wikidata
    public String imageLink;
    public String description;
    public Artist() {}
}