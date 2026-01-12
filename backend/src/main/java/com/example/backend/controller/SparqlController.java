package com.example.backend.controller;

import com.example.backend.config.PredefinedQueryLoader;
import com.example.backend.config.RDFConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.system.Txn;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Tag(name = "SPARQL Endpoint", description = "Endpoints for executing raw SPARQL queries against the RDF Knowledge Graph")
public class SparqlController {

    @Autowired
    private PredefinedQueryLoader queryLoader;

    @Autowired
    private RDFConfig rdfConfig;

    @Operation(
            summary = "Execute SPARQL Query",
            description = "Accepts a raw SPARQL SELECT query string and returns the results in a JSON format compatible with SPARQL 1.1 Query Results.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "The SPARQL SELECT query string",
                    required = true,
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(
                                    value = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 5"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Query executed successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = """
                                            {
                                                "head": {
                                                    "vars": [ "s", "p", "o" ]
                                                },
                                                "results": {
                                                    "bindings": [
                                                        {
                                                            "s": { "type": "uri", "value": "http://example.org/subject" },
                                                            "p": { "type": "uri", "value": "http://example.org/predicate" },
                                                            "o": { "type": "literal", "value": "Object Value" }
                                                        }
                                                    ]
                                                }
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Syntax error in SPARQL query or server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{ \"error\": \"Encountered '...' at line 1...\" }")
                            )
                    )
            }
    )
    @PostMapping("/sparql")
    public Map<String, Object> runQuery(@RequestBody String sparql) {
        Dataset dataset = rdfConfig.dataset(); // Use the dataset with all TTLs
        Map<String, Object> resultMap = new HashMap<>();

        Txn.executeRead(dataset, () -> {  // Ensure read-only transaction
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

    @Operation(
            summary = "Get Predefined Queries",
            description = "Returns a map of sample SPARQL queries loaded from the configuration file. Useful for testing or demonstration."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Map of query names to query strings",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{ \"Select All\": \"SELECT * WHERE { ?s ?p ?o } LIMIT 10\" }")
            )
    )
    @GetMapping("/predefined-queries")
    public Map<String, String> getPredefinedQueries() {
        return queryLoader.getPredefinedQueries();
    }
}