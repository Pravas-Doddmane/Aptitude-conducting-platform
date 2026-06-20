package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID attemptId,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String customName
    ) {
        CertificateService.CertificateFile file = certificateService.download(attemptId, format, customName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}
