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

    public List<Artist> getAllArtists() {
        return repository.findAll();
    }
    public Artist getArtistByUri(String artistUri) {
        return repository.findByUri(artistUri);
    }
    public List<Artist> getAllArtistsWithFirstArtwork() {
        return repository.findAllArtistWithFirstArtwork();
    }
}
