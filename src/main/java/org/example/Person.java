package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class Person {
    private String name;
    private int age;
    private double height;
    private LocalDateTime dateOfBirth;


}
