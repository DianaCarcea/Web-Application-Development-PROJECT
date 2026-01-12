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
@RequestMapping("/api/artists") // <--- Prefixed with /api
@Tag(name = "Artist API", description = "Endpoints for artist data (JSON)")
public class ApiArtistController {

    private final ArtistService service;
    private final ArtworkService artworkService;

    public ApiArtistController(ArtistService service, ArtworkService artworkService) {
        this.service = service;
        this.artworkService = artworkService;
    }

    @Operation(summary = "Get artist list", description = "Returns a paginated list of artists.")
    @GetMapping
    public ResponseEntity<?> getAllArtistsJson(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "30") int pageSize,


            @Parameter(description = "Selects the data domain", schema = @Schema(allowableValues = {"ro", "int"}))
            @RequestParam(name = "domain", defaultValue = "int") String domain

    ) {

        if (!domain.equals("ro") && !domain.equals("int")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid domain. Allowed values: 'ro', 'int'"));
        }


        if (pageSize <= 0) pageSize = 30;
        if (pageSize > 100) pageSize = 100;


        List<Artist> pagedArtists = service.getAllArtistsWithFirstArtworkHome(domain, page, pageSize);
        List<Artist> allArtistsForDropdown = service.getAllArtistsWithFirstArtwork(domain);

        int totalArtists = allArtistsForDropdown.size();
        int totalPages = (int) Math.ceil((double) totalArtists / pageSize);

        Map<String, Object> response = new HashMap<>();
        response.put("artists", pagedArtists);
        response.put("currentPage", page);
        response.put("pageSize", pageSize);
        response.put("totalPages", totalPages);
        response.put("totalItems", totalArtists);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get artist details", description = "Returns data for a single artist by ID.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getArtistDetails(
            @PathVariable("id") String artistId,
            @Parameter(description = "Selects the data domain", schema = @Schema(allowableValues = {"ro", "int"}))
            @RequestParam(name = "domain", defaultValue = "int") String domain)
    {

        String artistUri = "http://arp.ro/resource/agent/" + artistId;
        Artist artist = service.getArtistByUri(artistUri, domain);

        if (artist == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("description", artist.description);
        response.put("id", artist.id);
        response.put("name", artist.name);
        response.put("uri", artist.uri);
        response.put("wikidataLabel", artist.wikidataLabel);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get artworks by an artist", description = "Returns artist details and the list of their artworks.")
    @GetMapping("/{id}/artworks")
    public ResponseEntity<Map<String, Object>> getArtworksByArtist(
            @PathVariable("id") String artistId,
            @Parameter(schema = @Schema(allowableValues = {"ro", "int"}))
            @RequestParam(defaultValue = "int") String domain
    ) {
        String artistUri = "http://arp.ro/resource/agent/" + artistId;

        Artist artist = service.getArtistByUri(artistUri, domain);

        if (artist == null) {
            return ResponseEntity.notFound().build();
        }

        List<Artwork> artworks = artworkService.getArtworkByArtist(artistUri, domain);

        Map<String, Object> response = new HashMap<>();
        response.put("artist", artist);
        response.put("artworks", artworks);

        return ResponseEntity.ok(response);
    }
}