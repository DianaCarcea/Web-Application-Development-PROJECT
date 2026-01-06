package com.example.backend.controller;

import com.example.backend.model.Artwork;
import com.example.backend.service.ArtworkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ArtworkController {

    private final ArtworkService service;

    public ArtworkController(ArtworkService service) {
        this.service = service;
    }

    @GetMapping("/artworks")
    public String showAllArtworks(Model model) {
        List<Artwork> artworks = service.getAllArtworks();
        model.addAttribute("artworks", artworks);
        return "artwork"; // Thymeleaf va încărca artwork.html
    }
}