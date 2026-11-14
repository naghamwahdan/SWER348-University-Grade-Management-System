package edu.bethlehem.service;

import edu.bethlehem.model.*;
import java.time.LocalDateTime;
import java.util.*;

/** Assessment management and deadline queries for one semester. */
public class AssessmentService {
    private static final double WEIGHT_TOLERANCE = 1e-7;
    private final Semester semester;
    private final List<Enrollment> enrollments;
    private final List<Assessment> assessments = new ArrayList<>();
    private final Map<String, AssessmentResult> results = new LinkedHashMap<>();

    public AssessmentService(Semester semester, List<Enrollment> enrollments) {
        this(semester, enrollments, List.of());
    }

    public AssessmentService(Semester semester, List<Enrollment> enrollments,
                             Collection<Assessment> initialAssessments) {
        this.semester = Objects.requireNonNull(semester);
        this.enrollments = new ArrayList<>(Objects.requireNonNull(enrollments));
        for (Assessment a : Objects.requireNonNull(initialAssessments)) addAssessment(a);
    }

    public void addAssessment(Assessment assessment) {
        Objects.requireNonNull(assessment);
        if (assessments.stream().anyMatch(a -> a.getId().equals(assessment.getId())))
            throw new IllegalArgumentException("Duplicate assessment ID");
        LocalDateTime start = semester.getStartDate().atStartOfDay();
        LocalDateTime end = semester.getEndDate().atTime(23, 59, 59);
        if (assessment.getDeadline().isBefore(start) || assessment.getDeadline().isAfter(end))
            throw new IllegalArgumentException("Assessment deadline must be within semester");
        double total = assessments.stream()
                .filter(a -> a.getCourse().getCode().equals(assessment.getCourse().getCode()))
                .mapToDouble(Assessment::getWeight).sum() + assessment.getWeight();
        if (total > 100.0 + WEIGHT_TOLERANCE)
            throw new IllegalArgumentException("Course assessment weights cannot exceed 100%");
        assessments.add(assessment);
    }

    public List<Assessment> getAssessments() {
        return assessments.stream().sorted(Comparator.comparing(Assessment::getDeadline)
                .thenComparing(Assessment::getId)).toList();
    }

    public List<Assessment> findAssessmentsBetween(LocalDateTime start, LocalDateTime end) {
        Objects.requireNonNull(start); Objects.requireNonNull(end);
        if (end.isBefore(start)) throw new IllegalArgumentException("Range end cannot be before start");
        return getAssessments().stream()
                .filter(a -> !a.getDeadline().isBefore(start) && !a.getDeadline().isAfter(end))
                .toList();
    }

    public List<Assessment> findUpcomingForStudent(String studentId, LocalDateTime referenceTime) {
        Objects.requireNonNull(studentId); Objects.requireNonNull(referenceTime);
        Set<String> courseCodes = enrollments.stream()
                .filter(e -> e.getStudent().getId().equals(studentId))
                .map(e -> e.getCourse().getCode()).collect(java.util.stream.Collectors.toSet());
        return getAssessments().stream()
                .filter(a -> courseCodes.contains(a.getCourse().getCode()))
                .filter(a -> !a.getDeadline().isBefore(referenceTime)).toList();
    }

    public Optional<Assessment> findNextUpcoming(String studentId, LocalDateTime referenceTime) {
        return findUpcomingForStudent(studentId, referenceTime).stream().findFirst();
    }

    public List<Assessment> findOverdueWithoutMark(String studentId, LocalDateTime referenceTime) {
        Objects.requireNonNull(studentId); Objects.requireNonNull(referenceTime);
        List<Enrollment> studentEnrollments = enrollments.stream()
                .filter(e -> e.getStudent().getId().equals(studentId)).toList();
        return studentEnrollments.stream()
                .flatMap(e -> assessments.stream()
                        .filter(a -> a.getCourse().getCode().equals(e.getCourse().getCode()))
                        .filter(a -> a.getDeadline().isBefore(referenceTime))
                        .filter(a -> !hasResult(e, a)))
                .sorted(Comparator.comparing(Assessment::getDeadline).thenComparing(Assessment::getId))
                .toList();
    }

    public void recordOrUpdateResult(Enrollment enrollment, Assessment assessment, double mark) {
        Objects.requireNonNull(enrollment); Objects.requireNonNull(assessment);
        if (!enrollments.contains(enrollment))
            throw new IllegalArgumentException("Enrollment is not part of this semester");
        if (!assessments.contains(assessment))
            throw new IllegalArgumentException("Assessment is not registered in this service");
        AssessmentResult result = new AssessmentResult(enrollment, assessment, mark);
        results.put(key(enrollment, assessment), result);
    }

    public Optional<AssessmentResult> findResult(Enrollment enrollment, Assessment assessment) {
        return Optional.ofNullable(results.get(key(enrollment, assessment)));
    }
    public boolean hasResult(Enrollment enrollment, Assessment assessment) {
        return findResult(enrollment, assessment).isPresent();
    }
    public List<AssessmentResult> getResults() { return List.copyOf(results.values()); }
    public List<Assessment> getAssessmentsForCourse(Course course) {
        return getAssessments().stream()
                .filter(a -> a.getCourse().getCode().equals(course.getCode())).toList();
    }
    public List<Enrollment> getEnrollments() { return List.copyOf(enrollments); }
    public Semester getSemester() { return semester; }
    private String key(Enrollment e, Assessment a) { return e.getId() + "|" + a.getId(); }
}
