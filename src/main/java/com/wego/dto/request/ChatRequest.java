package com.wego.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for chat endpoint.
 *
 * @contract
 *   - message: Must not be blank and must be ≤ 500 characters
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "訊息不能為空")
    @Size(max = 500, message = "訊息長度不能超過 500 字")
    private String message;

    @Size(max = 50, message = "時區格式無效")
    private String timezone;
}
