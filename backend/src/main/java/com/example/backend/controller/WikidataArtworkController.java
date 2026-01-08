package com.example.backend.controller;

import com.example.backend.model.Artwork;
import com.example.backend.model.WikidataArtwork;
import com.example.backend.service.WikidataArtworkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class WikidataArtworkController {

    private final WikidataArtworkService service;

    public WikidataArtworkController(WikidataArtworkService service) {
        this.service = service;
    }

    @GetMapping("/top-artworks")
    public String showTopArtworks(Model model) {

        List<Artwork> artworks = service.getTopArtworks();
        model.addAttribute("artworks", artworks);
        return "artwork2";
    }

    @GetMapping("/wiki-home")
    public String showWikiHome(Model model) {

        List<Artwork> artworks = service.getHomepageArtworks();
        model.addAttribute("artworks", artworks);
        return "artwork2";
    }

    @GetMapping("/wiki-artworks/{id}")
    public String showWikiArtwork(
            @PathVariable String id,  // Q-ID, ex: Q599
            Model model) {

        Artwork artwork = service.getById(id);

        if (artwork == null) {
            return "404";
        }

        model.addAttribute("art", artwork);
        return "artwork-detail"; // Thymeleaf template pentru detalii artwork
    }


}
