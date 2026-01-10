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
            @RequestParam(value = "page", defaultValue = "1") int page
    ) {
        List<Artist> allArtists = service.getAllArtistsWithFirstArtwork();

        int pageSize = 23; // number of artists per page
        int totalArtists = allArtists.size();
        int totalPages = (int) Math.ceil((double) totalArtists / pageSize);

        // sublist pentru grid (paginare)
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalArtists);
        List<Artist> pagedArtists = allArtists.subList(fromIndex, toIndex);

        // toate artiștii pentru dropdown
        model.addAttribute("artistsForDropdown", allArtists);
        model.addAttribute("artists", pagedArtists);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        return "artists";
    }
}
