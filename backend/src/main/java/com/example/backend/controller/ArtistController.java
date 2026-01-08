package com.example.backend.controller;

import com.example.backend.model.Artist;
import com.example.backend.service.ArtistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ArtistController {

    private final ArtistService service;

    public ArtistController(ArtistService service) {
        this.service = service;
    }

    @GetMapping("/artists")
    public String showAllArtists(Model model) {
        List<Artist> artists = service.getAllArtists();
        model.addAttribute("artists", artists);
        return "artists"; // Thymeleaf va încărca artist.html
    }
}
