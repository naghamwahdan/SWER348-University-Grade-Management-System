package edu.bethlehem.service;

import edu.bethlehem.model.*;
import java.util.*;
import java.util.stream.Collectors;

/** Six Stream reports assigned with Parts D-G. */
public class StreamReportService {
    public record CourseCompletionSummary(long passed, long failed, long incomplete) {}
    public record CourseRanking(Course course, double averageFinalGrade) {}
    public record GpaRanking(Student student, double gpa, GpaService.GpaStatus status) {}
    private final List<Enrollment> enrollments;
    private final AssessmentService assessmentService;
    private final GradeService gradeService;
    private final GpaService gpaService;

    public StreamReportService(List<Enrollment> enrollments, AssessmentService assessmentService,
                               GradeService gradeService, GpaService gpaService) {
        this.enrollments = List.copyOf(Objects.requireNonNull(enrollments));
        this.assessmentService = Objects.requireNonNull(assessmentService);
        this.gradeService = Objects.requireNonNull(gradeService);
        this.gpaService = Objects.requireNonNull(gpaService);
    }

    /** Highest n available final grades in one course; ties are resolved by student ID. */
    public List<Enrollment> topStudentsInCourse(String courseCode, int n) {
        if (n <= 0) throw new IllegalArgumentException("n must be positive");
        return enrollments.stream()
                .filter(e -> e.getCourse().getCode().equals(courseCode))
                .filter(e -> gradeService.calculateFinalNumericGrade(e).isPresent())
                .sorted(Comparator.<Enrollment>comparingDouble(
                        e -> gradeService.calculateFinalNumericGrade(e).orElseThrow()).reversed()
                        .thenComparing(e -> e.getStudent().getId()))
                .limit(n).toList();
    }

    public CourseCompletionSummary courseCompletionSummary(String courseCode) {
        List<Enrollment> selected = enrollments.stream()
                .filter(e -> e.getCourse().getCode().equals(courseCode)).toList();
        return new CourseCompletionSummary(
                selected.stream().filter(e -> gradeService.classifyOutcome(e) == GradeService.CourseOutcome.PASSED).count(),
                selected.stream().filter(e -> gradeService.classifyOutcome(e) == GradeService.CourseOutcome.FAILED).count(),
                selected.stream().filter(e -> gradeService.classifyOutcome(e) == GradeService.CourseOutcome.INCOMPLETE).count());
    }

    /** Includes every enum value, including grades with zero students. */
    public Map<LetterGrade, Long> letterGradeDistribution(String courseCode) {
        Map<LetterGrade, Long> counts = Arrays.stream(LetterGrade.values())
                .collect(Collectors.toMap(g -> g, g -> 0L, (a,b) -> a, () -> new EnumMap<>(LetterGrade.class)));
        Map<LetterGrade, Long> actual = enrollments.stream()
                .filter(e -> e.getCourse().getCode().equals(courseCode))
                .map(gradeService::getLetterGrade).flatMap(Optional::stream)
                .collect(Collectors.groupingBy(g -> g, () -> new EnumMap<>(LetterGrade.class), Collectors.counting()));
        counts.putAll(actual);
        return Collections.unmodifiableMap(counts);
    }

    /** Courses with at least one available final grade, highest average first. */
    public List<CourseRanking> courseRanking() {
        return enrollments.stream()
                .collect(Collectors.groupingBy(e -> e.getCourse().getCode()))
                .values().stream()
                .map(group -> {
                    Course course = group.get(0).getCourse();
                    double[] grades = group.stream().map(gradeService::calculateFinalNumericGrade)
                            .filter(OptionalDouble::isPresent).mapToDouble(OptionalDouble::getAsDouble).toArray();
                    return grades.length == 0 ? null
                            : new CourseRanking(course, Arrays.stream(grades).average().orElseThrow());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(CourseRanking::averageFinalGrade).reversed()
                        .thenComparing(r -> r.course().getCode()))
                .toList();
    }

    /** Students with an available GPA, highest first; student ID breaks ties. */
    public List<GpaRanking> semesterGpaRanking() {
        return enrollments.stream().map(e -> e.getStudent().getId()).distinct()
                .map(gpaService::createReport)
                .filter(r -> r.gpa().isPresent())
                .map(r -> new GpaRanking(r.student(), r.gpa().getAsDouble(), r.status().orElseThrow()))
                .sorted(Comparator.comparingDouble(GpaRanking::gpa).reversed()
                        .thenComparing(r -> r.student().getId()))
                .toList();
    }

    /** Students mapped to assessments in their registered courses that have no result. */
    public Map<Student, List<Assessment>> missingResultsReport() {
        Map<Student, List<Assessment>> report = new LinkedHashMap<>();
        enrollments.stream().collect(Collectors.groupingBy(Enrollment::getStudent))
                .forEach((student, studentEnrollments) -> {
                    List<Assessment> missing = studentEnrollments.stream()
                            .flatMap(e -> assessmentService.getAssessmentsForCourse(e.getCourse()).stream()
                                    .filter(a -> !assessmentService.hasResult(e, a)))
                            .distinct().sorted(Comparator.comparing(Assessment::getDeadline)
                                    .thenComparing(Assessment::getId)).toList();
                    if (!missing.isEmpty()) report.put(student, missing);
                });
        return Collections.unmodifiableMap(report);
    }
}
