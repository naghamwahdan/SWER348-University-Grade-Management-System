
package edu.bethlehem.service;

import edu.bethlehem.model.*;
import java.util.*;

public class RegistrationService {

    private final Map<String, Student> students = new HashMap<>();
    private final Map<String, Course> courses = new HashMap<>();
    private final List<Enrollment> enrollments = new ArrayList<>();
    private final Semester semester;

    public RegistrationService(Semester semester) {
        this.semester = Objects.requireNonNull(semester);
    }

    public void addStudent(Student student) {
        Objects.requireNonNull(student);

        if (students.containsKey(student.getId())) {
            throw new IllegalArgumentException("Duplicate student ID");
        }

        students.put(student.getId(), student);
    }

    public void addCourse(Course course) {
        Objects.requireNonNull(course);

        if (courses.containsKey(course.getCode())) {
            throw new IllegalArgumentException("Duplicate course code");
        }

        courses.put(course.getCode(), course);
    }

    public Optional<Student> findStudent(String id) {
        return Optional.ofNullable(students.get(id));
    }

    public Optional<Course> findCourse(String code) {
        return Optional.ofNullable(courses.get(code));
    }

    public void registerStudent(String studentId, String courseCode) {

        Student student = findStudent(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown student"));

        Course course = findCourse(courseCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown course"));

        boolean alreadyRegistered = enrollments.stream()
                .anyMatch(e -> e.getStudent().getId().equals(studentId)
                        && e.getCourse().getCode().equals(courseCode));

        if (alreadyRegistered) {
            throw new IllegalArgumentException(
                    "Student already registered");
        }

        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStudent().getId().equals(studentId)) {
                Course existing = enrollment.getCourse();

                for (ClassMeeting m1 : existing.getMeetings()) {
                    for (ClassMeeting m2 : course.getMeetings()) {
                        if (m1.conflictsWith(m2)) {
                            throw new IllegalArgumentException(
                                    "Schedule conflict");
                        }
                    }
                }
            }
        }

        String enrollmentId = "E" + java.util.UUID.randomUUID();

        enrollments.add(
                new Enrollment(enrollmentId, student, course, semester));    }

    public List<Course> getStudentCourses(String studentId) {
        return enrollments.stream()
                .filter(e -> e.getStudent().getId().equals(studentId))
                .map(Enrollment::getCourse)
                .toList();
    }

    public List<Student> getCourseStudents(String courseCode) {
        return enrollments.stream()
                .filter(e -> e.getCourse().getCode().equals(courseCode))
                .map(Enrollment::getStudent)
                .toList();
    }

    public int getTotalCredits(String studentId) {
        return getStudentCourses(studentId).stream()
                .mapToInt(Course::getCreditHours)
                .sum();
    }

    public List<Enrollment> getEnrollments() {
        return List.copyOf(enrollments);
    }

    public Collection<Student> getStudents() {
        return Collections.unmodifiableCollection(students.values());
    }

    public Collection<Course> getCourses() {
        return Collections.unmodifiableCollection(courses.values());
    }

    public void loadExistingEnrollments(
            List<Enrollment> existingEnrollments) {

        Objects.requireNonNull(existingEnrollments);

        Map<String, Student> newStudents =
                new HashMap<>(students);
        Map<String, Course> newCourses =
                new HashMap<>(courses);
        List<Enrollment> newEnrollments =
                new ArrayList<>(enrollments);

        Set<String> enrollmentIds = new HashSet<>();

        for (Enrollment e : newEnrollments) {
            enrollmentIds.add(e.getId());
        }

        for (Enrollment enrollment : existingEnrollments) {
            Objects.requireNonNull(enrollment);

            Student student = enrollment.getStudent();
            Course course = enrollment.getCourse();

            if (!semester.getId().equals(
                    enrollment.getSemester().getId())) {
                throw new IllegalArgumentException(
                        "Incorrect semester");
            }

            if (!enrollmentIds.add(enrollment.getId())) {
                throw new IllegalArgumentException(
                        "Duplicate enrollment ID");
            }

            Student oldStudent =
                    newStudents.putIfAbsent(student.getId(), student);

            Course oldCourse =
                    newCourses.putIfAbsent(course.getCode(), course);

            if (oldStudent != null && oldStudent != student) {
                throw new IllegalArgumentException(
                        "Conflicting student ID");
            }

            if (oldCourse != null && oldCourse != course) {
                throw new IllegalArgumentException(
                        "Conflicting course code");
            }

            boolean duplicate = newEnrollments.stream()
                    .anyMatch(e ->
                            e.getStudent().getId().equals(student.getId())
                                    && e.getCourse().getCode().equals(course.getCode()));

            if (duplicate) {
                throw new IllegalArgumentException(
                        "Duplicate registration");
            }

            for (Enrollment existing : newEnrollments) {
                if (existing.getStudent().getId()
                        .equals(student.getId())) {

                    for (ClassMeeting m1 :
                            existing.getCourse().getMeetings()) {
                        for (ClassMeeting m2 : course.getMeetings()) {
                            if (m1.conflictsWith(m2)) {
                                throw new IllegalArgumentException(
                                        "Schedule conflict");
                            }
                        }
                    }
                }
            }

            newEnrollments.add(enrollment);
        }

        students.clear();
        students.putAll(newStudents);

        courses.clear();
        courses.putAll(newCourses);

        enrollments.clear();
        enrollments.addAll(newEnrollments);
    }

}
