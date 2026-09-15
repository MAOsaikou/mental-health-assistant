package com.itmao.aispringboot.controller;

import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.service.FileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
public class UploadedFileController {

    private final FileService fileService;

    public UploadedFileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/files/{*relativePath}")
    public ResponseEntity<Resource> get(@PathVariable String relativePath) {
        try {
            Path file = fileService.resolveReadableFile(relativePath);
            String contentType = fileService.probeContentType(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new FileSystemResource(file));
        } catch (BusinessException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
