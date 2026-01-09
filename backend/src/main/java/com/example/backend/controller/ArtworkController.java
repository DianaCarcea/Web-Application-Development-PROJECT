package com.example.backend.controller;

import com.example.backend.model.Artist;
import com.example.backend.model.Artwork;
import com.example.backend.service.ArtistService;
import com.example.backend.service.ArtworkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ArtworkController {

    private final ArtworkService artworkService;
    private final ArtistService artistService;

    public ArtworkController(ArtworkService artworkService, ArtistService artistService) {
        this.artworkService = artworkService;
        this.artistService = artistService;
    }

    @GetMapping("/artworks")
    public String showAllArtworks(Model model) {
        List<Artwork> artworks = artworkService.getAllArtworks();
        model.addAttribute("artworks", artworks);
        model.addAttribute("currentPage", 1);
        return "artwork"; // Thymeleaf va încărca artwork.html
    }

    @GetMapping("/artworks/{id}")
    public String showArtwork(
            @PathVariable String id,
            Model model) {

        String uri = "http://arp.ro/resource/artwork/" + id;
        Artwork artwork = artworkService.getArtworkByUri(uri);

        if (artwork == null) {
            return "404";
        }

        model.addAttribute("art", artwork);
        return "artwork-detail";
    }
    @GetMapping("artists/{id}/artworks")
    public String showArtworksByArtist(@PathVariable("id") String artistId, Model model) {
        String artistUri = "http://arp.ro/resource/agent/" + artistId;

        // 1️⃣ Ia datele artistului (inclusiv poza)
        Artist artist = artistService.getArtistByUri(artistUri);

        // 2️⃣ Ia operele artistului
        List<Artwork> artworks = artworkService.getArtworkByArtist(artistUri);

        model.addAttribute("artist", artist);
        model.addAttribute("artworks", artworks);

        return "artist-artworks";
    }

    @GetMapping("/artworks-next")
    public String showWikiHome(
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        int pageSize = 30;

        List<Artwork> artworks = artworkService.getHomepageArtworks(page, pageSize);

        model.addAttribute("artworks", artworks);
        model.addAttribute("currentPage", page);

        return "artwork";
    }
}