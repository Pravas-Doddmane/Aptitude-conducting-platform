package com.PassFamilyDoddmane.QuizeBackend.dto.certificate;

import java.util.UUID;

public record CertificateTemplateResponse(
        UUID id,
        String name,
        String description,
        String logoUrl,
        String htmlTemplate,
        String cssTemplate,
        Boolean active
) {
}
