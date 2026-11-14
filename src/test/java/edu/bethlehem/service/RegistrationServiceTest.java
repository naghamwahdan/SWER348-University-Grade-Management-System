
package edu.bethlehem.service;

import edu.bethlehem.loader.CsvDataLoader;
import edu.bethlehem.model.Course;
import edu.bethlehem.model.Enrollment;
import edu.bethlehem.model.Semester;
import edu.bethlehem.model.Student;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RegistrationServiceTest {

    @Test
    void duplicateRegistrationShouldFail() {

        CsvDataLoader loader =
                new CsvDataLoader(Path.of("data"));

        List<Student> students = loader.loadStudents();
        List<Course> courses = loader.loadCourses();

        loader.loadClassMeetings(courses);

        Semester semester = loader.loadSemester();

        List<Enrollment> enrollments =
                loader.loadEnrollments(students, courses, semester);

        RegistrationService service =
                new RegistrationService(semester);

        service.loadExistingEnrollments(enrollments);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.registerStudent(
                                "S001", "SWER348")
                );

        assertEquals(
                "Student already registered",
                exception.getMessage()
        );
    }

    @Test
    void scheduleConflictShouldFail() {

        CsvDataLoader loader = new CsvDataLoader(Path.of("data"));

        List<Student> students = loader.loadStudents();
        List<Course> courses = loader.loadCourses();
        loader.loadClassMeetings(courses);

        Semester semester = loader.loadSemester();

        List<Enrollment> enrollments =
                loader.loadEnrollments(students, courses, semester);

        RegistrationService service =
                new RegistrationService(semester);

        service.loadExistingEnrollments(enrollments);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.registerStudent("S001", "SWER313")
        );

        assertEquals("Schedule conflict", exception.getMessage());
    }

    @Test
    void validRegistrationShouldSucceed() {

        CsvDataLoader loader = new CsvDataLoader(Path.of("data"));

        List<Student> students = loader.loadStudents();
        List<Course> courses = loader.loadCourses();
        loader.loadClassMeetings(courses);

        Semester semester = loader.loadSemester();

        List<Enrollment> enrollments =
                loader.loadEnrollments(students, courses, semester);

        RegistrationService service =
                new RegistrationService(semester);

        service.loadExistingEnrollments(enrollments);

        Student newStudent = new Student(
                "S999",
                "Test Student",
                "test.student@example.com"
        );

        service.addStudent(newStudent);

        int before = service.getEnrollments().size();

        service.registerStudent("S999", "SWER348");

        assertEquals(before + 1, service.getEnrollments().size());

        assertEquals(1, service.getStudentCourses("S999").size());
    }

}
