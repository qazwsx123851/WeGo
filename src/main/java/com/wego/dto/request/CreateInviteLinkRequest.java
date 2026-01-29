package com.wego.dto.request;

import com.wego.entity.Role;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an invite link.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInviteLinkRequest {

    @NotNull(message = "角色不可為空")
    private Role role;

    @Min(value = 1, message = "有效期至少1天")
    @Max(value = 30, message = "有效期最多30天")
    @Builder.Default
    private int expiryDays = 7;
}
