
package edu.bethlehem.model;

import java.util.Objects;

public class Enrollment {

    private final String id;
    private final Student student;
    private final Course course;
    private final Semester semester;

    public Enrollment(String id, Student student,
                      Course course, Semester semester) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid enrollment ID");
        }

        this.id = id;
        this.student = Objects.requireNonNull(student);
        this.course = Objects.requireNonNull(course);
        this.semester = Objects.requireNonNull(semester);
    }

    public String getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public Course getCourse() {
        return course;
    }

    public Semester getSemester() {
        return semester;
    }

    @Override
    public String toString() {
        return id + ": " + student.getName()
                + " registered in " + course.getTitle();
    }
}
