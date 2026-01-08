package com.example.backend.config;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RDFConfig {

    @Bean
    public Model artworkModel() {
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, "output.ttl");
        return model;
    }

    @Bean
    public Model artistModel() {
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, "artists_wikidata.ttl");
        return model;
    }
}