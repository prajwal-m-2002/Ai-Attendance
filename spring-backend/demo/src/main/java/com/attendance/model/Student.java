package com.attendance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String rollNo;
    private String className;

    // Store JSON encoding as LONGTEXT (no LOB streaming)
    @Column(columnDefinition = "LONGTEXT")
    private String faceEncoding; // store JSON of encoding
}
