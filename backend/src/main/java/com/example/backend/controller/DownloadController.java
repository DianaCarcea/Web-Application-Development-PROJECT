package com.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@Tag(name = "Data Export", description = "Endpoints for managing and downloading raw RDF data (Turtle .ttl files)")
public class DownloadController {

    @Operation(
            summary = "List Available TTL Files",
            description = "Scans the server's data folder and returns a list of all available Turtle (.ttl) files."
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of filenames",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "[\"artists.ttl\", \"artworks.ttl\", \"museums.ttl\"]")
            )
    )
    @GetMapping("/ttl/list")
    public List<String> listTTLFiles() throws IOException {

        ClassPathResource folder = new ClassPathResource("data_ttl");

        return Files.list(folder.getFile().toPath())
                .filter(p -> p.toString().endsWith(".ttl"))
                .map(p -> p.getFileName().toString())
                .sorted()
                .toList();
    }

    @Operation(
            summary = "Preview TTL File Content",
            description = "Returns the raw text content of a specific .ttl file. Useful for inspecting data without downloading."
    )
    @ApiResponse(
            responseCode = "200",
            description = "File content returned successfully",
            content = @Content(mediaType = "text/plain")
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid file request (file must end with .ttl)"
    )
    @ApiResponse(
            responseCode = "404",
            description = "File not found on server"
    )
    @GetMapping(value = "/ttl/preview/{file}", produces = "text/plain; charset=UTF-8")
    public ResponseEntity<String> previewTTL(
            @Parameter(description = "The name of the file to preview (e.g., 'artworks.ttl')", required = true)
            @PathVariable String file
    ) throws IOException {

        if (!file.endsWith(".ttl")) {
            return ResponseEntity.badRequest().body("Invalid file type");
        }

        ClassPathResource resource =
                new ClassPathResource("data_ttl/" + file);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream is = resource.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        }
    }

    @Operation(
            summary = "Download All Data (ZIP)",
            description = "Compresses all available .ttl files into a single ZIP archive and initiates a file download."
    )
    @ApiResponse(
            responseCode = "200",
            description = "ZIP file generated successfully",
            content = @Content(
                    mediaType = "application/octet-stream",
                    schema = @Schema(type = "string", format = "binary") // This enables the "Download" button in Swagger UI
            )
    )
    @GetMapping("/download/ttl")
    public ResponseEntity<ByteArrayResource> downloadAllTTL() throws IOException {

        ClassPathResource folder = new ClassPathResource("data_ttl");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos);

        Files.walk(folder.getFile().toPath())
                .filter(p -> p.toString().endsWith(".ttl"))
                .forEach(p -> {
                    try {
                        zipOut.putNextEntry(new ZipEntry(p.getFileName().toString()));
                        Files.copy(p, zipOut);
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        zipOut.close();

        ByteArrayResource resource =
                new ByteArrayResource(baos.toByteArray());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=arp-ttl-files.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}