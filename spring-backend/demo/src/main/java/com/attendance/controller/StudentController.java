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
@CrossOrigin(origins = "*") // Allow frontend access
public class StudentController {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.api.url:http://localhost:8000}")
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
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("StudentController is working! Python API: " + pythonApiUrl);
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
            // Validate inputs
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Name is required");
            }
            if (rollNo == null || rollNo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Roll number is required");
            }
            if (className == null || className.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Class name is required");
            }
            if (faceImage == null || faceImage.isEmpty()) {
                return ResponseEntity.badRequest().body("Face image is required");
            }

            // Check if student already exists
            Optional<Student> existing = studentRepository.findByRollNo(rollNo);
            if (existing.isPresent()) {
                return ResponseEntity.badRequest()
                        .body("Student with roll number " + rollNo + " already exists");
            }

            // 1) Call Python API to get encoding
            String url = pythonApiUrl + "/encode-face";
            System.out.println("Calling Python API at: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", faceImage.getResource());

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response;
            try {
                response = restTemplate.postForEntity(url, requestEntity, String.class);
            } catch (Exception e) {
                System.err.println("Error calling Python API: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Cannot connect to face recognition service. Please ensure Python service is running at " + pythonApiUrl);
            }

            // Check if Python API returned success
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            if (!responseNode.has("success") || !responseNode.get("success").asBoolean()) {
                String errorMsg = responseNode.has("message") 
                    ? responseNode.get("message").asText() 
                    : "Unknown error from face recognition service";
                return ResponseEntity.badRequest().body("Face encoding failed: " + errorMsg);
            }

            // 2) Save student + encoding JSON
            Student s = new Student();
            s.setName(name.trim());
            s.setRollNo(rollNo.trim());
            s.setClassName(className.trim());
            s.setFaceEncoding(response.getBody());

            studentRepository.save(s);

            System.out.println("Student registered successfully: " + name);
            return ResponseEntity.ok()
                    .body("Student " + name + " registered successfully with roll number " + rollNo);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error during registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during registration: " + e.getMessage());
        }
    }

    // 🔹 Get all students
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        try {
            List<Student> students = studentRepository.findAll();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 🔹 Get student by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudentById(@PathVariable Long id) {
        try {
            Optional<Student> student = studentRepository.findById(id);
            if (student.isPresent()) {
                return ResponseEntity.ok(student.get());
            } else {
                return ResponseEntity.notFound().build();
            }
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
        if (encNode == null || !encNode.isArray()) {
            throw new Exception("Invalid encoding format in database");
        }
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
            if (faceImage == null || faceImage.isEmpty()) {
                return ResponseEntity.badRequest().body("Face image is required");
            }

            List<Student> students = studentRepository.findAll();
            if (students.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("No students registered. Please register students first.");
            }

            // Build list of encodings [[...], [...], ...]
            List<double[]> encList = new ArrayList<>();
            for (Student s : students) {
                try {
                    double[] arr = extractEncodingArray(s.getFaceEncoding());
                    encList.add(arr);
                } catch (Exception e) {
                    System.err.println("Error extracting encoding for student " + s.getName() + ": " + e.getMessage());
                }
            }

            if (encList.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("No valid face encodings found in database");
            }

            String encJson = objectMapper.writeValueAsString(encList);

            String url = pythonApiUrl + "/recognize-face";
            System.out.println("Calling Python API for recognition at: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", faceImage.getResource());
            body.add("known_encodings_json", encJson);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> pyResponse;
            try {
                pyResponse = restTemplate.postForEntity(url, requestEntity, String.class);
            } catch (Exception e) {
                System.err.println("Error calling Python API: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Cannot connect to face recognition service");
            }

            JsonNode root = objectMapper.readTree(pyResponse.getBody());
            if (!root.get("success").asBoolean()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Face recognition error: " + root.get("message").asText());
            }

            int bestIndex = root.get("best_index").asInt();
            double distance = root.get("distance").asDouble();

            // Threshold – tune this based on your images
            double threshold = 15.0;  // Adjust based on testing

            if (distance > threshold) {
                return ResponseEntity.badRequest()
                        .body("Face not recognized. Distance: " + String.format("%.2f", distance) + 
                              " (threshold: " + threshold + "). Please try again or register first.");
            }

            Student matched = students.get(bestIndex);

            LocalDate today = LocalDate.now();
            Optional<Attendance> existing =
                    attendanceRepository.findByStudentAndDate(matched, today);

            if (existing.isPresent()) {
                return ResponseEntity.ok()
                        .body("Attendance already marked for " + matched.getName() + 
                              " (Roll: " + matched.getRollNo() + ") today at " + 
                              existing.get().getTime());
            }

            Attendance att = new Attendance();
            att.setStudent(matched);
            att.setDate(today);
            att.setTime(LocalTime.now());
            att.setStatus("PRESENT");

            attendanceRepository.save(att);

            System.out.println("Attendance marked for: " + matched.getName());
            return ResponseEntity.ok()
                    .body("Attendance marked successfully for " + matched.getName() + 
                          " (Roll: " + matched.getRollNo() + ") at " + att.getTime());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error marking attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error marking attendance: " + e.getMessage());
        }
    }

    // 🔹 Delete student
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        try {
            Optional<Student> student = studentRepository.findById(id);
            if (student.isPresent()) {
                studentRepository.deleteById(id);
                return ResponseEntity.ok("Student deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
