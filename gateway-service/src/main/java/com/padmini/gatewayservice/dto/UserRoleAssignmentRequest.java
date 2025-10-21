package com.padmini.gatewayservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to represent a role assignment operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleAssignmentRequest {
    private String userId;
    private String roleName;
}
