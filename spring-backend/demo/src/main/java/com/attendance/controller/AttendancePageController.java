package com.attendance.controller;

import com.attendance.model.Attendance;
import com.attendance.repository.AttendanceRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/attendance")
public class AttendancePageController {

    private final AttendanceRepository attendanceRepository;

    public AttendancePageController(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping
    public String attendancePage(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        if (date == null) {
            date = LocalDate.now();
        }

        List<Attendance> records = attendanceRepository.findByDate(date);

        model.addAttribute("records", records);
        model.addAttribute("selectedDate", date);

        return "attendance"; // attendance.html
    }
}
