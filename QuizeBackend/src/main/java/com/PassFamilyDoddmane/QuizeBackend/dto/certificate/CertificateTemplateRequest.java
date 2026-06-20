package com.PassFamilyDoddmane.QuizeBackend.dto.certificate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CertificateTemplateRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @Size(max = 1000) String logoUrl,
        @NotBlank String htmlTemplate,
        String cssTemplate,
        Boolean active
) {
}
