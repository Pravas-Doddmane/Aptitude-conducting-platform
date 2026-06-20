package com.PassFamilyDoddmane.QuizeBackend.dto.identity;

import jakarta.validation.constraints.NotBlank;

public record CaptureReferenceImageRequest(
        @NotBlank(message = "Reference image URL is required")
        String referenceImageUrl
) {
}
