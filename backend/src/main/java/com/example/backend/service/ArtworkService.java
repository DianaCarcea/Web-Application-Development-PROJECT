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
}