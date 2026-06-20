package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.certificate.CertificateTemplateRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.certificate.CertificateTemplateResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.CertificateTemplateService;
import com.PassFamilyDoddmane.QuizeBackend.service.FileUploadService;
import com.PassFamilyDoddmane.QuizeBackend.dto.file.ImageUploadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/certificate-templates")
@RequiredArgsConstructor
public class AdminCertificateTemplateController {

    private final CertificateTemplateService certificateTemplateService;
    private final FileUploadService fileUploadService;

    @GetMapping
    public ResponseEntity<List<CertificateTemplateResponse>> all() {
        return ResponseEntity.ok(certificateTemplateService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificateTemplateResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(certificateTemplateService.get(id));
    }

    @PostMapping
    public ResponseEntity<CertificateTemplateResponse> create(@Valid @RequestBody CertificateTemplateRequest request) {
        return ResponseEntity.ok(certificateTemplateService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CertificateTemplateResponse> update(@PathVariable UUID id, @Valid @RequestBody CertificateTemplateRequest request) {
        return ResponseEntity.ok(certificateTemplateService.update(id, request));
    }

    @PostMapping(value = "/upload-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadLogo(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(fileUploadService.uploadImage(file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        certificateTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
