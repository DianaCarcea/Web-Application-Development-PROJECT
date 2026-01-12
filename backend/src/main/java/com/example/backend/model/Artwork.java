package com.example.backend.model;

import java.util.ArrayList;
import java.util.List;

public class Artwork extends Entity{
    public String inventoryNumber;
    public String category;
    public List<String> classification;
    public String condition;
    public String imageLink;
    public String cimecLink;
    public String wikidataLink;
    public String license;

    public List<String> cultures;
    public List<String> techniques;
    public List<String> materialsUsed;
    public Creation creation;
    public Registrar registrar;
    public String recordedAt;
    public Validator validator;
    public String validatedAt;
    public List<Collector> ownershipHistory = new ArrayList<>();

    public Artwork() {}
}