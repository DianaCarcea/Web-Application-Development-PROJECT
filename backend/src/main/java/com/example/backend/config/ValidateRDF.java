package com.example.backend.config;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;

public class ValidateRDF {
    public static void main(String[] args) {

        RDFConfig rdfConfig = new RDFConfig();
        Model artworkModel = rdfConfig.artworkModel();
        Model artistModel = rdfConfig.artistModel();

        Model shapesModel = RDFDataMgr.loadModel("src/main/resources/rdf/arp-shapes.ttl");

        ValidationReport reportArtwork = ShaclValidator.get().validate(shapesModel.getGraph(), artworkModel.getGraph());
        ValidationReport reportArtist = ShaclValidator.get().validate(shapesModel.getGraph(), artistModel.getGraph());

        System.out.println("Artwork Model Conforms? " + reportArtwork.conforms());
        reportArtwork.getEntries().forEach(System.out::println);

        System.out.println("Artist Model Conforms? " + reportArtist.conforms());
        reportArtwork.getEntries().forEach(System.out::println);
    }
}
