package com.example.backend.service;

import com.example.backend.model.Artwork;
import com.example.backend.model.WikidataArtwork;
import com.example.backend.repository.WikidataArtworkRepository;
import com.example.backend.repository.WikidataArtworkRepository2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WikidataArtworkService {

    private final WikidataArtworkRepository wikidataArtworkRepository;

    public WikidataArtworkService(WikidataArtworkRepository wikidataArtworkRepository) {
        this.wikidataArtworkRepository = wikidataArtworkRepository;
    }

    public List<Artwork> getTopArtworks() {

//        return wikidataArtworkRepository.findAll();
        return wikidataArtworkRepository.findAll();
    }

    public List<Artwork> getHomepageArtworks() {

        return wikidataArtworkRepository.findAllHome();
    }

    public Artwork getById(String qId) {

        return wikidataArtworkRepository.findById(qId);
    }

    public List<Artwork> getByIds(List<String> artworksIds) {

        return wikidataArtworkRepository.findByIds(artworksIds);
    }

    public List<Artwork> getHomepageArtworks(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return wikidataArtworkRepository.findPopularArtworks(pageSize, offset);
    }
}
