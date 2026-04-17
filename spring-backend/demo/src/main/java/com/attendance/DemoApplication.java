package com.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {

        // 🔹 SET JVM TIMEZONE TO IST BEFORE SPRING STARTS
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        System.out.println(">>> JVM Timezone Set To: " + TimeZone.getDefault());

        SpringApplication.run(DemoApplication.class, args);
    }
}
