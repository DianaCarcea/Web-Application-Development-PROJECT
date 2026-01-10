package com.example.backend.config;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

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

    @Bean
    public Model wikiModel() {
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, "artworks_arp.ttl");
        return model;
    }

    @Bean
    public Dataset dataset() {
        Dataset ds = DatasetFactory.createTxnMem();

        RDFDataMgr.read(ds, "output.ttl");
        RDFDataMgr.read(ds, "artists_wikidata.ttl");
        RDFDataMgr.read(ds, "artworks_arp.ttl");
        RDFDataMgr.read(ds, "getty-materials.ttl");
        RDFDataMgr.read(ds, "getty-categories.ttl");
        RDFDataMgr.read(ds, "museums_wikidata_getty.ttl");
        RDFDataMgr.read(ds, "museums_wikidata_getty_int.ttl");
        RDFDataMgr.read(ds, "artists_wikidata_getty.ttl");
        return ds;
    }
}