package com.mathbuddy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildRequest {

    @NotBlank(message = "Please enter your name!")
    private String name;

    @NotNull(message = "Please select your age!")
    @Min(value = 3, message = "MathBuddy is designed for children aged 3 and above")
    @Max(value = 14, message = "MathBuddy is designed for children aged 14 and below")
    private Integer age;

    private String avatar; // "owl", "fox", "frog", "bunny", "lion", "bear"
}

