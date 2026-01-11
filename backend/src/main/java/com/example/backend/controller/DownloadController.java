package com.example.backend.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class DownloadController {

    @GetMapping("/ttl/list")
    public List<String> listTTLFiles() throws IOException {

        ClassPathResource folder = new ClassPathResource("data_ttl");

        return Files.list(folder.getFile().toPath())
                .filter(p -> p.toString().endsWith(".ttl"))
                .map(p -> p.getFileName().toString())
                .sorted()
                .toList();
    }

    @GetMapping(value = "/ttl/preview/{file}", produces = "text/plain; charset=UTF-8")
    public ResponseEntity<String> previewTTL(@PathVariable String file) throws IOException {

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
