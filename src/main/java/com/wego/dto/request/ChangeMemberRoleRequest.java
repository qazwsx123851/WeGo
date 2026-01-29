package com.wego.dto.request;

import com.wego.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for changing a member's role.
 *
 * @contract
 *   - role: required, cannot be OWNER
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeMemberRoleRequest {

    @NotNull(message = "角色不可為空")
    private Role role;
}
