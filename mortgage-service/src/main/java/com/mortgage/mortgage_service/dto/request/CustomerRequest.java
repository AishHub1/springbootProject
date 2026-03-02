package com.mortgage.mortgage_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    // Accepts formats: +1-800-555-0199, (800) 555-0199, 8005550199
    @Pattern(
            regexp = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]{7,14}$",
            message = "Invalid phone number format"
    )
    private String phone;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;
}