package com.example.backend.service;

import com.example.backend.model.Artwork;
import com.example.backend.repository.ArtworkRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArtworkService {

    private final ArtworkRepository repository;

    public ArtworkService(ArtworkRepository repository) {
        this.repository = repository;
    }

    public List<Artwork> getAllArtworks() {
        return repository.findAll(); // simplu, curat
    }

    public Artwork getArtworkByUri(String uri) {
        return repository.findByUri(uri);
    }
    public List<Artwork> getArtworkByArtist(String artistUri) {
        return repository.findByArtist(artistUri);
    }
}