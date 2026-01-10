package com.example.backend.controller;

import com.example.backend.service.ArtistService;
import com.example.backend.service.ArtworkService;
import com.example.backend.service.WikidataArtworkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ArtworkService artworkService;
    private final ArtistService artistService;

    public HomeController(ArtworkService artworkService, WikidataArtworkService wikidataArtworkService,ArtistService artistService) {
        this.artworkService = artworkService;
        this.artistService = artistService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("artworks", artworkService.getHomepageArtworks(1, 30, "ro"));
        model.addAttribute("wikidataArtworks", artworkService.getHomepageArtworks(1, 30,"int"));
        model.addAttribute("artists", artistService.getAllArtistsWithFirstArtwork());
        return "home"; // Thymeleaf template home.html
    }


}