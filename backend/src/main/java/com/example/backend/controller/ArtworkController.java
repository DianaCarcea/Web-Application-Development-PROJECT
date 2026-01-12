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
    public String showAllArtworks(
            @RequestParam(name = "domain", defaultValue = "int") String domain,
            Model model) {

        List<Artwork> artworks = artworkService.getHomepageArtworks(1, 30, domain);
        model.addAttribute("domain", domain);
        model.addAttribute("artworks", artworks);
        model.addAttribute("currentPage", 1);
        return "artwork";
    }

    @GetMapping("/artworks/{id}")
    public String showArtwork(
            @PathVariable String id,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "8") int limit,
            @RequestParam(name = "domain", defaultValue = "int") String domain,
            Model model) {

        String uri = "http://arp.ro/resource/artwork/" + id;
        Artwork artwork = artworkService.getArtworkByUri(uri, domain);

        if (artwork == null) {
            return "404";
        }

        model.addAttribute("art", artwork);
        model.addAttribute("domain", domain);


        List<Artwork> recommendations = artworkService.getRecommendations(uri, offset, limit, domain);
        List<Artwork> recommendationsMuseums = artworkService.getRecommendationsMuseums(uri, offset, limit, domain);
        List<Artwork> recommendationsCategory = artworkService.getRecommendationsCategory(uri, offset, limit, domain);

        model.addAttribute("recommendations", recommendations);
        model.addAttribute("recommendationsMuseums", recommendationsMuseums);
        model.addAttribute("recommendationsCategory", recommendationsCategory);

        return "artwork-detail";
    }
    @GetMapping("artists/{id}/artworks")
    public String showArtworksByArtist(
            @PathVariable("id") String artistId,
            @RequestParam(name = "domain", defaultValue = "int") String domain,
            Model model) {

        String artistUri = "http://arp.ro/resource/agent/" + artistId;

        Artist artist = artistService.getArtistByUri(artistUri, domain);

        List<Artwork> artworks = artworkService.getArtworkByArtist(artistUri, domain);

        model.addAttribute("artist", artist);
        model.addAttribute("artworks", artworks);
        model.addAttribute("domain", domain);

        return "artist-artworks";
    }

    @GetMapping("/artworks-next")
    public String showWikiHome(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "domain", defaultValue = "int") String domain,
            Model model) {

        int pageSize = 30;

        List<Artwork> artworks = artworkService.getHomepageArtworks(page, pageSize, domain);

        model.addAttribute("artworks", artworks);
        model.addAttribute("currentPage", page);
        model.addAttribute("domain",domain);

        return "artwork";
    }

    @GetMapping("/artworks/recommendations-fragment")
    public String getRecommendationsFragment(
            @RequestParam("uri") String uri,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "8") int limit,
            @RequestParam(value = "domain", defaultValue = "int") String domain,
            Model model) {

        List<Artwork> recommendations = artworkService.getRecommendations(uri, offset, limit, domain);

        model.addAttribute("recommendations", recommendations);
        List<Artwork> recommendationsMuseums = artworkService.getRecommendationsMuseums(uri, offset, limit, domain);
        model.addAttribute("recommendationsMuseums", recommendationsMuseums);
        List<Artwork> recommendationsCategory = artworkService.getRecommendationsCategory(uri, offset, limit, domain);
        model.addAttribute("recommendationsCategory", recommendationsCategory);

        model.addAttribute("domain", domain);

        return "rec-fragment";
    }
}