package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.certificate.CertificateTemplateRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.certificate.CertificateTemplateResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.CertificateTemplate;
import com.PassFamilyDoddmane.QuizeBackend.repository.CertificateTemplateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CertificateTemplateService {

    private final CertificateTemplateRepository certificateTemplateRepository;

    public CertificateTemplateResponse create(CertificateTemplateRequest request) {
        CertificateTemplate template = new CertificateTemplate();
        apply(template, request);
        return toResponse(certificateTemplateRepository.save(template));
    }

    public CertificateTemplateResponse update(UUID id, CertificateTemplateRequest request) {
        CertificateTemplate template = certificateTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate template not found"));
        apply(template, request);
        return toResponse(certificateTemplateRepository.save(template));
    }

    public void delete(UUID id) {
        CertificateTemplate template = certificateTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate template not found"));
        template.setActive(Boolean.FALSE);
        certificateTemplateRepository.save(template);
    }

    public List<CertificateTemplateResponse> findAll() {
        return certificateTemplateRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CertificateTemplateResponse get(UUID id) {
        return toResponse(certificateTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate template not found")));
    }

    private void apply(CertificateTemplate template, CertificateTemplateRequest request) {
        template.setName(request.name().trim());
        template.setDescription(request.description());
        template.setLogoUrl(normalizeLogoUrl(request.logoUrl()));
        template.setHtmlTemplate(request.htmlTemplate());
        template.setCssTemplate(request.cssTemplate());
        template.setActive(request.active() == null || Boolean.TRUE.equals(request.active()));
    }

    private String normalizeLogoUrl(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return null;
        }
        return logoUrl.trim();
    }

    private CertificateTemplateResponse toResponse(CertificateTemplate template) {
        return new CertificateTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getLogoUrl(),
                template.getHtmlTemplate(),
                template.getCssTemplate(),
                template.getActive()
        );
    }
}
