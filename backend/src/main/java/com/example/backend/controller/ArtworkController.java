package com.example.backend.controller;

import com.example.backend.model.Artwork;
import com.example.backend.service.ArtworkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

    @GetMapping("/artworks/{id}")
    public String showArtwork(
            @PathVariable String id,
            Model model) {

        String uri = "http://arp.ro/resource/artwork/" + id;
        Artwork artwork = service.getArtworkByUri(uri);

        if (artwork == null) {
            return "404";
        }

        model.addAttribute("art", artwork);
        return "artwork-detail";
    }

}