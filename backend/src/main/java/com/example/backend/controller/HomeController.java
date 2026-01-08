package com.example.backend.controller;

import com.example.backend.service.ArtworkService;
import com.example.backend.service.WikidataArtworkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ArtworkService artworkService;
    private final WikidataArtworkService wikidataArtworkService;

    public HomeController(ArtworkService artworkService, WikidataArtworkService wikidataArtworkService) {
        this.artworkService = artworkService;
        this.wikidataArtworkService = wikidataArtworkService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("artworks", artworkService.getAllArtworks());
        model.addAttribute("wikidataArtworks", wikidataArtworkService.getHomepageArtworks());

        return "home"; // Thymeleaf template home.html
    }


}