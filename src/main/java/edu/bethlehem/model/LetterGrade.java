package edu.bethlehem.model;

import java.util.Arrays;

/** Project grade scale. Thresholds are inclusive lower bounds. */
public enum LetterGrade {
    A(90.0, 4.0), B_PLUS(85.0, 3.5), B(80.0, 3.0),
    C_PLUS(75.0, 2.5), C(70.0, 2.0), D_PLUS(65.0, 1.5),
    D(60.0, 1.0), F(0.0, 0.0);

    private final double minimumNumericGrade;
    private final double gradePoints;

    LetterGrade(double minimumNumericGrade, double gradePoints) {
        this.minimumNumericGrade = minimumNumericGrade;
        this.gradePoints = gradePoints;
    }
    public double getMinimumNumericGrade() { return minimumNumericGrade; }
    public double getGradePoints() { return gradePoints; }

    public static LetterGrade fromNumericGrade(double grade) {
        if (!Double.isFinite(grade) || grade < 0 || grade > 100)
            throw new IllegalArgumentException("Numeric grade must be between 0 and 100");
        return Arrays.stream(values())
                .filter(g -> grade >= g.minimumNumericGrade)
                .findFirst().orElseThrow();
    }
}
