package com.example.backend.controller;

import com.example.backend.model.Artist;
import com.example.backend.service.ArtistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ArtistController {

    private final ArtistService service;

    public ArtistController(ArtistService service) {
        this.service = service;
    }

    @GetMapping("/artists")
    public String showAllArtists(
            Model model,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(name = "domain", defaultValue = "int") String domain
    ) {
        int pageSize = 23; // Numărul de artiști per pagină

        List<Artist> pagedArtists = service.getAllArtistsWithFirstArtworkHome(domain, page, pageSize);

        List<Artist> allArtistsForDropdown = service.getAllArtistsWithFirstArtwork(domain);

        int totalArtists = allArtistsForDropdown.size();
        int totalPages = (int) Math.ceil((double) totalArtists / pageSize);

        model.addAttribute("artistsForDropdown", allArtistsForDropdown); // Lista completă pt <select>
        model.addAttribute("artists", pagedArtists);                   // Lista scurtă (23) pt Grid
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("domain", domain);

        return "artists";
    }
}
