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

        // 1. Obținem DOAR artiștii pentru pagina curentă (folosind LIMIT și OFFSET din SPARQL)
        // Aici apelăm metoda nouă creată în pașii anteriori
        List<Artist> pagedArtists = service.getAllArtistsWithFirstArtworkHome(domain, page, pageSize);

        // 2. Obținem TOȚI artiștii pentru Dropdown și pentru a calcula numărul total de pagini
        // Aici apelăm metoda veche (fără limită) sau o metodă simplificată doar cu nume/id
        List<Artist> allArtistsForDropdown = service.getAllArtistsWithFirstArtwork(domain);
        // ^ Asigură-te că metoda asta există în continuare în Service (poate fi cea veche care făcea findAll)

        // 3. Calculăm totalurile pe baza listei complete
        int totalArtists = allArtistsForDropdown.size();
        int totalPages = (int) Math.ceil((double) totalArtists / pageSize);

        // 4. Setăm atributele în Model
        model.addAttribute("artistsForDropdown", allArtistsForDropdown); // Lista completă pt <select>
        model.addAttribute("artists", pagedArtists);                   // Lista scurtă (23) pt Grid
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("domain", domain);

        return "artists";
    }
}
