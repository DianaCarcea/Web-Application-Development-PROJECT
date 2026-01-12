package com.example.backend.controller;

import com.example.backend.model.Artist;
import com.example.backend.model.Artwork;
import com.example.backend.service.ArtistService;
import com.example.backend.service.ArtworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api") // General prefix, routes will be /api/artworks etc.
@Tag(name = "Artwork API", description = "Endpoints for managing artworks (JSON)")
public class ApiArtworkController {

    private final ArtworkService artworkService;

    public ApiArtworkController(ArtworkService artworkService) {
        this.artworkService = artworkService;
    }

    @Operation(summary = "Get artwork list", description = "Returns a paginated list of artworks.")
    @GetMapping("/artworks")
    public ResponseEntity<Map<String, Object>> getAllArtworks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int pageSize,

            @Parameter(schema = @Schema(allowableValues = {"ro", "int"}))
            @RequestParam(defaultValue = "int") String domain
    ) {
        // Simple validation
        if (pageSize <= 0) pageSize = 30;
        if (pageSize > 100) pageSize = 100;

        List<Artwork> artworks = artworkService.getHomepageArtworks(page, pageSize, domain);

        Map<String, Object> response = new HashMap<>();
        response.put("artworks", artworks);
        response.put("currentPage", page);
        response.put("pageSize", pageSize);


        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get artwork details", description = "Returns the artwork and recommendation lists.")
    @GetMapping("/artworks/{id}")
    public ResponseEntity<Map<String, Object>> getArtworkDetails(
            @PathVariable String id,
            @Parameter(description = "Limits the number of recommendations per category")
            @RequestParam(defaultValue = "8") int limit,
            @Parameter(schema = @Schema(allowableValues = {"ro", "int"}))
            @RequestParam(defaultValue = "int") String domain
    ) {
        String uri = "http://arp.ro/resource/artwork/" + id;
        Artwork artwork = artworkService.getArtworkByUri(uri, domain);

        if (artwork == null) {
            return ResponseEntity.notFound().build();
        }

        List<Artwork> rawRecArtist = artworkService.getRecommendations(uri, 0, limit, domain);
        List<Artwork> rawRecMuseum = artworkService.getRecommendationsMuseums(uri, 0, limit, domain);
        List<Artwork> rawRecCategory = artworkService.getRecommendationsCategory(uri, 0, limit, domain);

        Map<String, Object> response = new HashMap<>();
        response.put("artwork", artwork);

        Map<String, Object> recommendations = new HashMap<>();

        recommendations.put("byArtist", simplifyArtworks(rawRecArtist));
        recommendations.put("byMuseum", simplifyArtworks(rawRecMuseum));
        recommendations.put("byCategory", simplifyArtworks(rawRecCategory));

        response.put("recommendations", recommendations);

        return ResponseEntity.ok(response);
    }



    @Operation(summary = "Get recommendations (Pagination)", description = "Returns recommendations filtered by type. Optimizes performance by executing only the necessary query.")
    @GetMapping("/artworks/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendationsData(
            @RequestParam("id") String id,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "8") int limit,

            @Parameter(description = "Desired recommendation type", schema = @Schema(allowableValues = {"artist", "museum", "category", "all"}))
            @RequestParam(defaultValue = "all") String type,

            @Parameter(schema = @Schema(allowableValues = {"ro", "int"}))
            @RequestParam(defaultValue = "int") String domain
    ) {
        Map<String, Object> response = new HashMap<>();


        String uri = "http://arp.ro/resource/artwork/" + id;

        switch (type.toLowerCase()) {
            case "artist":
                List<Artwork> recArtist = artworkService.getRecommendations(uri, offset, limit, domain);
                response.put("byArtist", simplifyArtworks(recArtist));
                break;

            case "museum":
                List<Artwork> recMuseum = artworkService.getRecommendationsMuseums(uri, offset, limit, domain);
                response.put("byMuseum", simplifyArtworks(recMuseum));
                break;

            case "category":
                List<Artwork> recCategory = artworkService.getRecommendationsCategory(uri, offset, limit, domain);
                response.put("byCategory", simplifyArtworks(recCategory));
                break;

            case "all":
            default:
                response.put("byArtist", simplifyArtworks(artworkService.getRecommendations(uri, offset, limit, domain)));
                response.put("byMuseum", simplifyArtworks(artworkService.getRecommendationsMuseums(uri, offset, limit, domain)));
                response.put("byCategory", simplifyArtworks(artworkService.getRecommendationsCategory(uri, offset, limit, domain)));
                break;
        }

        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> simplifyArtworks(List<Artwork> artworks) {
        if (artworks == null) return List.of();

        return artworks.stream().map(art -> {
            Map<String, Object> simpleArt = new HashMap<>();
            simpleArt.put("id", art.id);
            simpleArt.put("title", art.title);
            simpleArt.put("imageLink", art.imageLink);
            simpleArt.put("uri", art.uri);
            return simpleArt;
        }).toList();
    }
}