package edu.bethlehem.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** A graded assessment belonging to one course. */
public class Assessment {
    private final String id;
    private final Course course;
    private final String title;
    private final double maximumMark;
    private final double weight;
    private final LocalDateTime deadline;

    public Assessment(String id, Course course, String title,
                      double maximumMark, double weight, LocalDateTime deadline) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Invalid assessment ID");
        this.course = Objects.requireNonNull(course, "Course cannot be null");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Invalid assessment title");
        if (!Double.isFinite(maximumMark) || maximumMark <= 0)
            throw new IllegalArgumentException("Maximum mark must be positive and finite");
        if (!Double.isFinite(weight) || weight <= 0 || weight > 100)
            throw new IllegalArgumentException("Weight must be greater than 0 and at most 100");
        this.deadline = Objects.requireNonNull(deadline, "Deadline cannot be null");
        this.id = id;
        this.title = title;
        this.maximumMark = maximumMark;
        this.weight = weight;
    }
    public String getId() { return id; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    public double getMaximumMark() { return maximumMark; }
    public double getWeight() { return weight; }
    public LocalDateTime getDeadline() { return deadline; }
    @Override public String toString() { return id + " - " + title + " (" + course.getCode() + ")"; }
}
