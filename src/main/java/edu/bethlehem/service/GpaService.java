package edu.bethlehem.service;

import edu.bethlehem.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Credit-weighted semester GPA and a detailed semester grade report. */
public class GpaService {
    public enum GpaStatus { FINAL, PROVISIONAL }
    public record CourseGrade(Course course, int credits, OptionalDouble numericGrade,
                              Optional<LetterGrade> letterGrade, OptionalDouble gradePoints,
                              boolean incomplete) {}
    public record SemesterGradeReport(Student student, List<CourseGrade> courses,
            int totalRegisteredCredits, int creditsIncludedInGpa,
            OptionalDouble gpa, Optional<GpaStatus> status) {}

    private final GradeService gradeService;
    private final List<Enrollment> enrollments;

    public GpaService(GradeService gradeService, List<Enrollment> enrollments) {
        this.gradeService = Objects.requireNonNull(gradeService);
        this.enrollments = List.copyOf(Objects.requireNonNull(enrollments));
    }

    public OptionalDouble calculateGpa(String studentId) {
        List<Enrollment> studentEnrollments = forStudent(studentId);
        double qualityPoints = studentEnrollments.stream()
                .filter(e -> gradeService.calculateFinalNumericGrade(e).isPresent())
                .mapToDouble(e -> gradeService.getGradePoints(e).orElseThrow()
                        * e.getCourse().getCreditHours()).sum();
        int credits = studentEnrollments.stream()
                .filter(e -> gradeService.calculateFinalNumericGrade(e).isPresent())
                .mapToInt(e -> e.getCourse().getCreditHours()).sum();
        return credits == 0 ? OptionalDouble.empty() : OptionalDouble.of(qualityPoints / credits);
    }

    public SemesterGradeReport createReport(String studentId) {
        List<Enrollment> studentEnrollments = forStudent(studentId);
        Student student = studentEnrollments.stream().findFirst()
                .map(Enrollment::getStudent)
                .orElseThrow(() -> new IllegalArgumentException("Student has no enrollments: " + studentId));
        List<CourseGrade> rows = studentEnrollments.stream().map(e -> {
            OptionalDouble numeric = gradeService.calculateFinalNumericGrade(e);
            Optional<LetterGrade> letter = gradeService.getLetterGrade(e);
            OptionalDouble points = gradeService.getGradePoints(e);
            return new CourseGrade(e.getCourse(), e.getCourse().getCreditHours(),
                    numeric, letter, points, numeric.isEmpty());
        }).toList();
        int totalCredits = studentEnrollments.stream().mapToInt(e -> e.getCourse().getCreditHours()).sum();
        int includedCredits = rows.stream().filter(r -> r.numericGrade().isPresent())
                .mapToInt(CourseGrade::credits).sum();
        OptionalDouble gpa = calculateGpa(studentId);
        boolean incomplete = rows.stream().anyMatch(CourseGrade::incomplete);
        Optional<GpaStatus> status = gpa.isEmpty() ? Optional.empty()
                : Optional.of(incomplete ? GpaStatus.PROVISIONAL : GpaStatus.FINAL);
        return new SemesterGradeReport(student, rows, totalCredits, includedCredits, gpa, status);
    }

    public String formatGpa(OptionalDouble gpa) {
        return gpa.isEmpty() ? "Not available"
                : BigDecimal.valueOf(gpa.getAsDouble()).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    public List<Enrollment> getEnrollments() { return enrollments; }
    private List<Enrollment> forStudent(String id) {
        return enrollments.stream().filter(e -> e.getStudent().getId().equals(id)).toList();
    }
}
