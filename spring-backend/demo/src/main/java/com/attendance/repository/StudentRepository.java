package com.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.attendance.model.Student;

public interface StudentRepository extends JpaRepository<Student, Integer> {
}
