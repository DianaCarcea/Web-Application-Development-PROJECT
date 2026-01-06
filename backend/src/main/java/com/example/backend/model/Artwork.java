package com.example.backend.model;

import java.util.List;

public class Artwork extends Entity{
    public String inventoryNumber;
    public String category;
    public List<String> classification;
    public String condition;
    public String imageLink;
    public String cimecLink;
    public String license;

    public List<String> cultures; // poate fi mai multe valori
    public List<String> techniques; // din Creation
    public List<String> materialsUsed; // din Restoration
//    public List<Activity> activities; // alte activități (restaurare, transfer, expoziție)

    public Artwork() {}
}