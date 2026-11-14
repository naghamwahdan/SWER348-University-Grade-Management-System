
package edu.bethlehem.service;

import edu.bethlehem.loader.CsvDataLoader;
import edu.bethlehem.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScheduleServiceTest {

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        CsvDataLoader loader =
                new CsvDataLoader(Path.of("data"));

        List<Student> students = loader.loadStudents();
        List<Course> courses = loader.loadCourses();

        loader.loadClassMeetings(courses);

        Semester semester = loader.loadSemester();

        List<Enrollment> enrollments =
                loader.loadEnrollments(students, courses, semester);

        RegistrationService registrationService =
                new RegistrationService(semester);

        registrationService.loadExistingEnrollments(enrollments);

        scheduleService =
                new ScheduleService(registrationService);
    }

    @Test
    void weeklyScheduleShouldHaveSixMeetings() {
        List<ClassMeeting> meetings =
                scheduleService.getWeeklySchedule("S001");

        assertEquals(6, meetings.size());
    }

    @Test
    void mondayScheduleShouldHaveTwoMeetings() {
        List<ClassMeeting> meetings =
                scheduleService.getMeetingsOnDay(
                        "S001", DayOfWeek.MONDAY);

        assertEquals(2, meetings.size());

        assertTrue(meetings.stream()
                .allMatch(m -> m.getDay() == DayOfWeek.MONDAY));
    }

    @Test
    void weeklyDurationShouldBeCorrect() {
        Duration duration =
                scheduleService.getTotalWeeklyClassTime("S001");

        assertEquals(Duration.ofMinutes(450), duration);
    }

    @Test
    void overlappingCoursesShouldConflict() {
        CsvDataLoader loader =
                new CsvDataLoader(Path.of("data"));

        List<Course> courses = loader.loadCourses();
        loader.loadClassMeetings(courses);

        Course first = courses.stream()
                .filter(c -> c.getCode().equals("SWER348"))
                .findFirst()
                .orElseThrow();

        Course second = courses.stream()
                .filter(c -> c.getCode().equals("SWER313"))
                .findFirst()
                .orElseThrow();

        assertTrue(scheduleService.coursesConflict(first, second));
    }

    @Test
    void nonOverlappingCoursesShouldNotConflict() {
        CsvDataLoader loader =
                new CsvDataLoader(Path.of("data"));

        List<Course> courses = loader.loadCourses();
        loader.loadClassMeetings(courses);

        Course first = courses.stream()
                .filter(c -> c.getCode().equals("SWER348"))
                .findFirst()
                .orElseThrow();

        Course second = courses.stream()
                .filter(c -> c.getCode().equals("SWER410"))
                .findFirst()
                .orElseThrow();

        assertFalse(scheduleService.coursesConflict(first, second));
    }
}
