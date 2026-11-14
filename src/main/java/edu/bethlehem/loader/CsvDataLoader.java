
package edu.bethlehem.loader;
import edu.bethlehem.model.AssessmentResult;
import edu.bethlehem.model.Assessment;

import edu.bethlehem.model.Enrollment;
import edu.bethlehem.model.Student;
import java.util.HashSet;
import java.util.Set;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import edu.bethlehem.model.Course;
import edu.bethlehem.model.Semester;
import java.time.LocalDate;
import java.time.LocalDateTime;
import edu.bethlehem.model.ClassMeeting;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.HashMap;

public class CsvDataLoader {

    private final Path dataFolder;

    public CsvDataLoader(Path dataFolder) {
        if (dataFolder == null) {
            throw new IllegalArgumentException(
                    "Data folder cannot be null");
        }

        this.dataFolder = dataFolder;
    }

    public List<String[]> readCsv(String filename) {
        Path file = dataFolder.resolve(filename);
        List<String[]> rows = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(
                    file, StandardCharsets.UTF_8);

            if (lines.isEmpty()) {
                throw new IllegalArgumentException(
                        filename + ": file is empty");
            }

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);

                if (line.isBlank()) {
                    throw new IllegalArgumentException(
                            filename + ", line " + (i + 1)
                                    + ": empty row");
                }

                rows.add(line.split(",", -1));
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Cannot read " + filename, e);
        }

        return rows;
    }

    public List<Student> loadStudents() {

        List<String[]> rows = readCsv("students.csv");
        List<Student> students = new ArrayList<>();
        Set<String> studentIds = new HashSet<>();

        int lineNumber = 2;

        for (String[] row : rows) {

            try {
                if (row.length != 3) {
                    throw new IllegalArgumentException(
                            "Expected 3 columns");
                }

                Student student = new Student(
                        row[0].trim(),
                        row[1].trim(),
                        row[2].trim()
                );

                if (!studentIds.add(student.getId())) {
                    throw new IllegalArgumentException(
                            "Duplicate student ID: " + student.getId());
                }

                students.add(student);

            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "students.csv, line " + lineNumber
                                + ": " + e.getMessage(), e);
            }

            lineNumber++;
        }

        return students;
    }

    public List<Course> loadCourses() {

        List<String[]> rows = readCsv("courses.csv");
        List<Course> courses = new ArrayList<>();
        Set<String> courseCodes = new HashSet<>();

        int lineNumber = 2;

        for (String[] row : rows) {
            try {
                if (row.length != 3) {
                    throw new IllegalArgumentException(
                            "Expected 3 columns");
                }

                String code = row[0].trim();
                String title = row[1].trim();
                int credits = Integer.parseInt(row[2].trim());

                Course course = new Course(code, title, credits);

                if (!courseCodes.add(course.getCode())) {
                    throw new IllegalArgumentException(
                            "Duplicate course code: " + code);
                }

                courses.add(course);

            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "courses.csv, line " + lineNumber
                                + ": " + e.getMessage(), e);
            }

            lineNumber++;
        }

        return courses;
    }

    public Semester loadSemester() {

        List<String[]> rows = readCsv("semester.csv");

        if (rows.size() != 1) {
            throw new IllegalArgumentException(
                    "semester.csv must contain exactly one semester");
        }

        String[] row = rows.get(0);

        try {
            if (row.length != 5) {
                throw new IllegalArgumentException(
                        "Expected 5 columns");
            }

            return new Semester(
                    row[0].trim(),
                    row[1].trim(),
                    LocalDate.parse(row[2].trim()),
                    LocalDate.parse(row[3].trim()),
                    LocalDateTime.parse(row[4].trim())
            );

        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "semester.csv, line 2: " + e.getMessage(), e);
        }
    }

    public void loadClassMeetings(List<Course> courses) {

        List<String[]> rows = readCsv("class_meetings.csv");

        Map<String, Course> courseMap = new HashMap<>();

        for (Course course : courses) {
            courseMap.put(course.getCode(), course);
        }

        Set<String> meetingIds = new HashSet<>();

        int lineNumber = 2;

        for (String[] row : rows) {
            try {
                if (row.length != 6) {
                    throw new IllegalArgumentException(
                            "Expected 6 columns");
                }

                String meetingId = row[0].trim();
                String courseCode = row[1].trim();

                if (meetingId.isBlank()
                        || !meetingIds.add(meetingId)) {
                    throw new IllegalArgumentException(
                            "Invalid or duplicate meeting ID: " + meetingId);
                }

                Course course = courseMap.get(courseCode);

                if (course == null) {
                    throw new IllegalArgumentException(
                            "Unknown course: " + courseCode);
                }

                DayOfWeek day = DayOfWeek.valueOf(
                        row[2].trim().toUpperCase());

                LocalTime start = LocalTime.parse(row[3].trim());
                LocalTime end = LocalTime.parse(row[4].trim());
                String room = row[5].trim();

                ClassMeeting meeting = new ClassMeeting(
                        day, start, end, room);

                course.addMeeting(meeting);

            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "class_meetings.csv, line " + lineNumber
                                + ": " + e.getMessage(), e);
            }

            lineNumber++;
        }
    }

    public List<Enrollment> loadEnrollments(
            List<Student> students,
            List<Course> courses,
            Semester semester) {

        List<String[]> rows = readCsv("enrollments.csv");

        Map<String, Student> studentMap = new HashMap<>();
        Map<String, Course> courseMap = new HashMap<>();

        for (Student student : students) {
            studentMap.put(student.getId(), student);
        }

        for (Course course : courses) {
            courseMap.put(course.getCode(), course);
        }

        List<Enrollment> enrollments = new ArrayList<>();
        Set<String> enrollmentIds = new HashSet<>();
        Set<String> registrationKeys = new HashSet<>();

        int lineNumber = 2;

        for (String[] row : rows) {
            try {
                if (row.length != 4) {
                    throw new IllegalArgumentException(
                            "Expected 4 columns");
                }

                String enrollmentId = row[0].trim();
                String studentId = row[1].trim();
                String courseCode = row[2].trim();
                String semesterId = row[3].trim();

                if (enrollmentId.isBlank()
                        || !enrollmentIds.add(enrollmentId)) {
                    throw new IllegalArgumentException(
                            "Invalid or duplicate enrollment ID");
                }

                Student student = studentMap.get(studentId);
                Course course = courseMap.get(courseCode);

                if (student == null) {
                    throw new IllegalArgumentException(
                            "Unknown student: " + studentId);
                }

                if (course == null) {
                    throw new IllegalArgumentException(
                            "Unknown course: " + courseCode);
                }

                if (!semester.getId().equals(semesterId)) {
                    throw new IllegalArgumentException(
                            "Unknown semester: " + semesterId);
                }

                String key = studentId + "|" + courseCode
                        + "|" + semesterId;

                if (!registrationKeys.add(key)) {
                    throw new IllegalArgumentException(
                            "Duplicate registration: " + key);
                }

                enrollments.add(
                        new Enrollment(enrollmentId, student, course, semester));

            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "enrollments.csv, line " + lineNumber
                                + ": " + e.getMessage(), e);
            }

            lineNumber++;
        }

        return enrollments;
    }

    public List<Assessment> loadAssessments(List<Course> courses) {

        List<String[]> rows = readCsv("assessments.csv");
        List<Assessment> assessments = new ArrayList<>();

        Map<String, Course> courseMap = new HashMap<>();
        for (Course course : courses) {
            courseMap.put(course.getCode(), course);
        }

        Set<String> assessmentIds = new HashSet<>();
        int lineNumber = 2;

        for (String[] row : rows) {
            try {
                if (row.length != 6) {
                    throw new IllegalArgumentException("Expected 6 columns");
                }

                String id = row[0].trim();
                String courseCode = row[1].trim();
                String title = row[2].trim();
                double maximumMark = Double.parseDouble(row[3].trim());
                double weight = Double.parseDouble(row[4].trim());
                LocalDateTime deadline = LocalDateTime.parse(row[5].trim());

                Course course = courseMap.get(courseCode);

                if (course == null) {
                    throw new IllegalArgumentException(
                            "Unknown course: " + courseCode);
                }

                if (!assessmentIds.add(id)) {
                    throw new IllegalArgumentException(
                            "Duplicate assessment ID: " + id);
                }

                assessments.add(new Assessment(
                        id, course, title, maximumMark, weight, deadline));

            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "assessments.csv, line " + lineNumber
                                + ": " + e.getMessage(), e);
            }

            lineNumber++;
        }

        return assessments;
    }

    public List<AssessmentResult> loadAssessmentResults(
            List<Enrollment> enrollments,
            List<Assessment> assessments) {

        List<String[]> rows = readCsv("assessment_results.csv");
        List<AssessmentResult> results = new ArrayList<>();

        Map<String, Enrollment> enrollmentMap = new HashMap<>();
        Map<String, Assessment> assessmentMap = new HashMap<>();

        for (Enrollment enrollment : enrollments) {
            enrollmentMap.put(enrollment.getId(), enrollment);
        }

        for (Assessment assessment : assessments) {
            assessmentMap.put(assessment.getId(), assessment);
        }

        Set<String> resultKeys = new HashSet<>();
        int lineNumber = 2;

        for (String[] row : rows) {
            try {
                if (row.length != 3) {
                    throw new IllegalArgumentException(
                            "Expected 3 columns");
                }

                String enrollmentId = row[0].trim();
                String assessmentId = row[1].trim();
                double mark = Double.parseDouble(row[2].trim());

                Enrollment enrollment = enrollmentMap.get(enrollmentId);
                Assessment assessment = assessmentMap.get(assessmentId);

                if (enrollment == null) {
                    throw new IllegalArgumentException(
                            "Unknown enrollment: " + enrollmentId);
                }

                if (assessment == null) {
                    throw new IllegalArgumentException(
                            "Unknown assessment: " + assessmentId);
                }

                String key = enrollmentId + "|" + assessmentId;

                if (!resultKeys.add(key)) {
                    throw new IllegalArgumentException(
                            "Duplicate assessment result: " + key);
                }

                results.add(new AssessmentResult(
                        enrollment, assessment, mark));

            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "assessment_results.csv, line " + lineNumber
                                + ": " + e.getMessage(), e);
            }

            lineNumber++;
        }

        return results;
    }

}
