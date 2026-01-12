package com.example.backend.service;

import com.example.backend.model.Artist;
import com.example.backend.repository.ArtistRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArtistService {

    private final ArtistRepository repository;

    public ArtistService(ArtistRepository repository) {
        this.repository = repository;
    }

//    public List<Artist> getAllArtists() {
//        return repository.findAll();
//    }
    public Artist getArtistByUri(String artistUri, String domain) {
        return repository.findByUri(artistUri, domain);
    }

    public List<Artist> getAllArtistsWithFirstArtwork(String domain) {
        return repository.findAllArtistWithFirstArtwork(domain);
    }


    public List<Artist> getAllArtistsWithFirstArtworkHome(String domain, int page, int pageSize) {

        // Calculăm offset-ul: (Pagina 1 -> 0, Pagina 2 -> 20, etc.)
        int offset = (page - 1) * pageSize;
        if (offset < 0) offset = 0;

        return repository.findAllArtistWithFirstArtworkHome(domain, pageSize, offset);
    }


}
