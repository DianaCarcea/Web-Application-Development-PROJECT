package com.example.backend.controller;

import com.example.backend.model.Artist;
import com.example.backend.service.ArtistService;
import com.example.backend.service.ArtworkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final ArtworkService artworkService;
    private final ArtistService artistService;

    public HomeController(ArtworkService artworkService, ArtistService artistService) {
        this.artworkService = artworkService;
        this.artistService = artistService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("artworks", artworkService.getHomepageArtworks(1, 30, "ro"));
        model.addAttribute("wikidataArtworks", artworkService.getHomepageArtworks(1, 30,"int"));

        List<Artist> allArtists = artistService.getAllArtistsWithFirstArtworkHome("ro", 1, 15);
        List<Artist> allArtistsInt = artistService.getAllArtistsWithFirstArtworkHome("int", 1, 15);

        model.addAttribute("artists", allArtists);
        model.addAttribute("artistsWiki", allArtistsInt);
        return "home";
    }


}