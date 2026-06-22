package com.bidly.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "firstName is required")
//    @Size(min = 2, max = 50, message = "first name must be between 2 and 50 chars")
    private String firstName;

    @NotBlank(message = "lastName is required")
//    @Size(min = 2, max = 30, message = "last name must be between 2 and 30 chars")
    private String lastName;

    @NotBlank(message = "username is required")
//    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,25}$",
//            message = "Username can only contain letters, numbers and the characters '.', '_', '-' (between 3 and 25 chars)")
    private String username;

    @NotBlank(message = "email is required")
//    @Email(message = "email should be valid")
//    @Size(min = 2, max = 100, message = "email must be between 2 and 100 chars")
    private String email;

    @NotBlank(message = "password is required")
//    @Pattern(
//            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,100}$",
//            message = "Password must be between 8 and 100 characters and contain at least one digit, one lowercase letter, one uppercase letter, and one special character (@#$%^&+=!)"
//    )
    private String password;

//    @Pattern(regexp = "^(\\+?4)?07[0-9]{8}$",
//            message = "The phone number must be a valid Romanian number(ex: 0712345678, +40712345678 sau 40712345678)")
    private String phoneNumber;
}
