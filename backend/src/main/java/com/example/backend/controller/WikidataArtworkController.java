//package com.example.backend.controller;
//
//import com.example.backend.model.Artwork;
//import com.example.backend.service.WikidataArtworkService;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Controller
//public class WikidataArtworkController {
//
//    private final WikidataArtworkService service;
//
//    public WikidataArtworkController(WikidataArtworkService service) {
//        this.service = service;
//    }
//
//    @GetMapping("/top-artworks")
//    public String showTopArtworks(Model model) {
//
//        List<Artwork> artworks = service.getTopArtworks();
//        model.addAttribute("artworks", artworks);
//        return "artwork-int";
//    }
//
//    @GetMapping("/artworks-int")
//    public String showWikiHome(Model model) {
//
//        List<Artwork> artworks = service.getHomepageArtworks();
//
//        List<String> ids = artworks.stream()
//                .limit(5)
//                .map(a -> a.id)
//                .toList();
//
//        List<Artwork> artworksWithDetails = service.getByIds(ids);
//
//        model.addAttribute("artworks", artworksWithDetails);
//        model.addAttribute("currentPage", 1);
//        return "artwork-int";
//    }
//
//    @GetMapping("/artworks-int/{id}")
//    public String showWikiArtwork(
//            @PathVariable String id,  // Q-ID, ex: Q599
//            Model model) {
//
//        Artwork artwork = service.getById(id);
//
//        if (artwork == null) {
//            return "404";
//        }
//
//        model.addAttribute("art", artwork);
//        return "artwork-detail"; // Thymeleaf template pentru detalii artwork
//    }
//
//    @GetMapping("/artworks-int-next")
//    public String showWikiHome(
//            @RequestParam(defaultValue = "1") int page,
//            Model model) {
//
//        int pageSize = 5;
//
//        List<Artwork> artworks = service.getHomepageArtworks(page, pageSize);
//
//        List<String> ids = artworks.stream()
//                .limit(5)
//                .map(a -> a.id)
//                .toList();
//
//        List<Artwork> artworksWithDetails = service.getByIds(ids);
//
//        model.addAttribute("artworks", artworksWithDetails);
//        model.addAttribute("currentPage", page);
//
//        return "artwork-int";
//    }
//
//
//}
