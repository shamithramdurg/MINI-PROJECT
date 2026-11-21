package com.installment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RoleRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "user|admin", message = "Role must be 'user' or 'admin'")
    private String role;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

