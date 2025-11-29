package com.attendance.controller;

import com.attendance.model.Attendance;
import com.attendance.model.Student;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.StudentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.api.url}")
    private String pythonApiUrl;

    public StudentController(StudentRepository studentRepository,
                             AttendanceRepository attendanceRepository,
                             RestTemplate restTemplate) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.restTemplate = restTemplate;
    }

    // 🔹 Simple GET to test mapping
    @GetMapping("/test")
    public String test() {
        return "StudentController is working!";
    }

    // 🔹 Register student + store face encoding
    @PostMapping("/register")
    public ResponseEntity<?> registerStudent(
            @RequestParam String name,
            @RequestParam String rollNo,
            @RequestParam String className,
            @RequestParam("faceImage") MultipartFile faceImage
    ) {
        try {
            // 1) Call Python API to get encoding
            String url = pythonApiUrl + "/encode-face";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", faceImage.getResource());

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, requestEntity, String.class);

            // 2) Save student + encoding JSON
            Student s = new Student();
            s.setName(name);
            s.setRollNo(rollNo);
            s.setClassName(className);
            s.setFaceEncoding(response.getBody());

            studentRepository.save(s);

            return ResponseEntity.ok("Student registered successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // 🔹 Helper: extract encoding array from JSON stored in DB
    private double[] extractEncodingArray(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode encNode = root.get("encoding");
        double[] arr = new double[encNode.size()];
        for (int i = 0; i < encNode.size(); i++) {
            arr[i] = encNode.get(i).asDouble();
        }
        return arr;
    }

    // 🔹 Mark attendance using face recognition
    @PostMapping("/mark-attendance")
    public ResponseEntity<?> markAttendance(
            @RequestParam("faceImage") MultipartFile faceImage
    ) {
        try {
            List<Student> students = studentRepository.findAll();
            if (students.isEmpty()) {
                return ResponseEntity.badRequest().body("No students registered.");
            }

            // Build list of encodings [[...], [...], ...]
            List<double[]> encList = new ArrayList<>();
            for (Student s : students) {
                double[] arr = extractEncodingArray(s.getFaceEncoding());
                encList.add(arr);
            }

            String encJson = objectMapper.writeValueAsString(encList);

            String url = pythonApiUrl + "/recognize-face";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", faceImage.getResource());
            body.add("known_encodings_json", encJson);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> pyResponse =
                    restTemplate.postForEntity(url, requestEntity, String.class);

            JsonNode root = objectMapper.readTree(pyResponse.getBody());
            if (!root.get("success").asBoolean()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Python error: " + root.get("message").asText());
            }

            int bestIndex = root.get("best_index").asInt();
            double distance = root.get("distance").asDouble();

            // Threshold – tune this based on your images
double threshold = 20.0;  // start here, you can adjust

if (distance > threshold) {
    return ResponseEntity.badRequest()
            .body("Face not confidently recognized. Distance=" + distance);
}

            Student matched = students.get(bestIndex);

            LocalDate today = LocalDate.now();
            Optional<Attendance> existing =
                    attendanceRepository.findByStudentAndDate(matched, today);

            if (existing.isPresent()) {
                return ResponseEntity.ok("Already marked present for " + matched.getName());
            }

            Attendance att = new Attendance();
            att.setStudent(matched);
            att.setDate(today);
            att.setTime(LocalTime.now());
            att.setStatus("PRESENT");

            attendanceRepository.save(att);

            return ResponseEntity.ok("Attendance marked for: " + matched.getName());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
