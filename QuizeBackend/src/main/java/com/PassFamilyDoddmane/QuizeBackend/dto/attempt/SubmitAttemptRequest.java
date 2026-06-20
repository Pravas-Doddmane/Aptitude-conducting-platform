package com.PassFamilyDoddmane.QuizeBackend.dto.attempt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAttemptRequest(
        @NotEmpty @Valid List<SubmitAnswerRequest> answers
) {
}
