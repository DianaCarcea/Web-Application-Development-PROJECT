package com.example.backend.controller;

import com.example.backend.config.PredefinedQueryLoader;
import com.example.backend.config.RDFConfig;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.system.Txn;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class SparqlController {

    @Autowired
    private PredefinedQueryLoader queryLoader;

    @Autowired
    private  RDFConfig rdfConfig;

    // Endpoint pentru rularea SPARQL pe dataset-ul complet
    @PostMapping("/sparql")
    public Map<String, Object> runQuery(@RequestBody String sparql) {
        Dataset dataset = rdfConfig.dataset(); // folosim dataset-ul cu toate TTL-urile
        Map<String, Object> resultMap = new HashMap<>();

        Txn.executeRead(dataset, () -> {  // asigură tranzacție read-only
            try {
                Query query = QueryFactory.create(sparql);
                if (query.isSelectType()) {
                    try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
                        ResultSet results = qexec.execSelect();
                        resultMap.put("head", Map.of("vars", results.getResultVars()));

                        List<Map<String, Map<String, String>>> bindingsList = new ArrayList<>();
                        while (results.hasNext()) {
                            QuerySolution sol = results.next();
                            Map<String, Map<String, String>> row = new HashMap<>();
                            for (String var : results.getResultVars()) {
                                if (sol.contains(var)) {
                                    RDFNode rdfNode = sol.get(var);
                                    Map<String, String> valueMap = new HashMap<>();
                                    if (rdfNode.isURIResource()) {
                                        valueMap.put("type", "uri");
                                        valueMap.put("value", rdfNode.asResource().getURI());
                                    } else if (rdfNode.isLiteral()) {
                                        valueMap.put("type", "literal");
                                        valueMap.put("value", rdfNode.asLiteral().getString());
                                    }
                                    row.put(var, valueMap);
                                }
                            }
                            bindingsList.add(row);
                        }
                        resultMap.put("results", Map.of("bindings", bindingsList));
                    }
                } else {
                    resultMap.put("error", "Only SELECT queries are supported.");
                }
            } catch (Exception e) {
                resultMap.put("error", e.getMessage());
            }
        });

        return resultMap;
    }

    @GetMapping("/predefined-queries")
    public Map<String, String> getPredefinedQueries() {
        return queryLoader.getPredefinedQueries();
    }
}
