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

    public Artwork getArtworkByUri(String uri, String domain) {
        return repository.findByUri(uri, domain);
    }
    public List<Artwork> getArtworkByArtist(String artistUri, String domain) {
        return repository.findByArtist(artistUri, domain);
    }

    public List<Artwork> getHomepageArtworks(int page, int pageSize, String domain) {
        int offset = (page - 1) * pageSize;
        return repository.findNext(pageSize, offset, domain);
    }

    public List<Artwork> getRecommendations(String uri, int offset, int pageSize, String domain) {

        return repository.getRecommendationsByArtist(uri, pageSize, offset, domain);
    }

    public List<Artwork> getRecommendationsMuseums(String uri, int offset, int pageSize, String domain) {
        return repository.getRecommendationsByMuseums(uri, pageSize, offset, domain);
    }

    public List<Artwork> getRecommendationsCategory(String uri, int offset, int limit, String domain) {
        return repository.getRecommendationsByCategory(uri, offset, limit, domain);
    }
}