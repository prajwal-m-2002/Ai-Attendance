package com.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "dashboard";   // dashboard.html
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
