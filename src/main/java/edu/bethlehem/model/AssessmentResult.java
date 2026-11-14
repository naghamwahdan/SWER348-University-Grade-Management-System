package edu.bethlehem.model;

import java.util.Objects;

/** A recorded mark connecting an enrollment to an assessment. */
public class AssessmentResult {
    private final Enrollment enrollment;
    private final Assessment assessment;
    private final double mark;

    public AssessmentResult(Enrollment enrollment, Assessment assessment, double mark) {
        this.enrollment = Objects.requireNonNull(enrollment, "Enrollment cannot be null");
        this.assessment = Objects.requireNonNull(assessment, "Assessment cannot be null");
        if (!enrollment.getCourse().getCode().equals(assessment.getCourse().getCode()))
            throw new IllegalArgumentException("Assessment does not belong to enrollment course");
        if (!Double.isFinite(mark) || mark < 0 || mark > assessment.getMaximumMark())
            throw new IllegalArgumentException("Mark must be finite and between 0 and maximum mark");
        this.mark = mark;
    }
    public Enrollment getEnrollment() { return enrollment; }
    public Assessment getAssessment() { return assessment; }
    public double getMark() { return mark; }
}
