package com.PassFamilyDoddmane.QuizeBackend.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        Integer sortOrder,
        Boolean active
) {
}
