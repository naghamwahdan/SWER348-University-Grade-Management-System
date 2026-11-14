package edu.bethlehem.service;

import edu.bethlehem.model.*;
import java.util.*;

/** Records assessment marks and derives final course grades. */
public class GradeService {
    public enum CourseOutcome { PASSED, FAILED, INCOMPLETE }
    private static final double WEIGHT_TOLERANCE = 1e-7;
    private static final double PASS_THRESHOLD = 60.0;
    private final AssessmentService assessmentService;

    public GradeService(AssessmentService assessmentService) {
        this.assessmentService = Objects.requireNonNull(assessmentService);
    }

    public void recordMark(Enrollment enrollment, Assessment assessment, double mark) {
        assessmentService.recordOrUpdateResult(enrollment, assessment, mark);
    }
    public OptionalDouble getMark(Enrollment enrollment, Assessment assessment) {
        return assessmentService.findResult(enrollment, assessment)
                .map(r -> OptionalDouble.of(r.getMark())).orElseGet(OptionalDouble::empty);
    }
    public List<Assessment> getMissingAssessments(Enrollment enrollment) {
        return assessmentService.getAssessmentsForCourse(enrollment.getCourse()).stream()
                .filter(a -> !assessmentService.hasResult(enrollment, a)).toList();
    }
    public OptionalDouble calculateFinalNumericGrade(Enrollment enrollment) {
        List<Assessment> courseAssessments =
                assessmentService.getAssessmentsForCourse(enrollment.getCourse());
        if (courseAssessments.isEmpty()) return OptionalDouble.empty();
        double totalWeight = courseAssessments.stream().mapToDouble(Assessment::getWeight).sum();
        if (Math.abs(totalWeight - 100.0) > WEIGHT_TOLERANCE) return OptionalDouble.empty();
        if (courseAssessments.stream().anyMatch(a -> !assessmentService.hasResult(enrollment, a)))
            return OptionalDouble.empty();
        double grade = courseAssessments.stream().mapToDouble(a -> {
            double mark = getMark(enrollment, a).orElseThrow();
            return (mark / a.getMaximumMark()) * a.getWeight();
        }).sum();
        return OptionalDouble.of(grade);
    }
    public CourseOutcome classifyOutcome(Enrollment enrollment) {
        OptionalDouble grade = calculateFinalNumericGrade(enrollment);
        if (grade.isEmpty()) return CourseOutcome.INCOMPLETE;
        return grade.getAsDouble() >= PASS_THRESHOLD ? CourseOutcome.PASSED : CourseOutcome.FAILED;
    }
    public Optional<LetterGrade> getLetterGrade(Enrollment enrollment) {
        return calculateFinalNumericGrade(enrollment).stream()
                .mapToObj(LetterGrade::fromNumericGrade).findFirst();
    }
    public OptionalDouble getGradePoints(Enrollment enrollment) {
        return getLetterGrade(enrollment).isPresent()
                ? OptionalDouble.of(getLetterGrade(enrollment).orElseThrow().getGradePoints())
                : OptionalDouble.empty();
    }
    public AssessmentService getAssessmentService() { return assessmentService; }
}
