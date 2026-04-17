package com.attendance.repository;

import com.attendance.model.Attendance;
import com.attendance.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    Optional<Attendance> findByStudentAndDate(Student student, LocalDate date);

    // 🔹 NEW: list all attendance for a date
    List<Attendance> findByDate(LocalDate date);
}
