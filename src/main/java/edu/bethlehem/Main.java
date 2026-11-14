
package edu.bethlehem;
import edu.bethlehem.service.ScheduleService;
import java.time.DayOfWeek;

import edu.bethlehem.service.RegistrationService;
import edu.bethlehem.model.Enrollment;
import edu.bethlehem.model.ClassMeeting;
import edu.bethlehem.model.Semester;
import edu.bethlehem.model.Course;
import edu.bethlehem.loader.CsvDataLoader;
import edu.bethlehem.model.Student;

import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        CsvDataLoader loader =
                new CsvDataLoader(Path.of("data"));

        List<Student> students = loader.loadStudents();

        System.out.println("Students loaded: " + students.size());

        for (Student student : students) {
            System.out.println(student);
        }

        List<Course> courses = loader.loadCourses();
        var assessments = loader.loadAssessments(courses);
        System.out.println("Assessments loaded: " + assessments.size());
        loader.loadClassMeetings(courses);

        System.out.println("\nCourses loaded: " + courses.size());

        for (Course course : courses) {
            System.out.println(course);
        }

        Semester semester = loader.loadSemester();

        System.out.println("\nSemester loaded:");
        System.out.println(semester);
        System.out.println("Reference time: "
                + semester.getReferenceTime());

        System.out.println("\nCOURSE MEETINGS:");

        for (Course course : courses) {
            System.out.println("\n" + course.getCode()
                    + " - " + course.getTitle());

            for (ClassMeeting meeting : course.getMeetings()) {
                System.out.println("  " + meeting);
            }
        }

        List<Enrollment> enrollments =
                loader.loadEnrollments(students, courses, semester);
        var assessmentResults =
                loader.loadAssessmentResults(enrollments, assessments);

        System.out.println(
                "Assessment results loaded: " + assessmentResults.size());

        RegistrationService registrationService =
                new RegistrationService(semester);

        registrationService.loadExistingEnrollments(enrollments);

        System.out.println("\nRegistration Service:");
        System.out.println("Students: "
                + registrationService.getStudents().size());

        System.out.println("Courses: "
                + registrationService.getCourses().size());

        System.out.println("Enrollments: "
                + registrationService.getEnrollments().size());

        System.out.println("S001 registered credits: "
                + registrationService.getTotalCredits("S001"));

        // TEST 1: Duplicate registration
        System.out.println("\nTesting duplicate registration:");

        try {
            registrationService.registerStudent("S001", "SWER348");
            System.out.println("ERROR: Duplicate registration allowed!");
        } catch (IllegalArgumentException e) {
            System.out.println("PASS: " + e.getMessage());
        }

        // TEST 2: Schedule conflict
        System.out.println("\nTesting schedule conflict:");

        try {
            registrationService.registerStudent("S001", "SWER313");
            System.out.println("ERROR: Schedule conflict allowed!");
        } catch (IllegalArgumentException e) {
            System.out.println("PASS: " + e.getMessage());
        }

        // TEST 3: Successful registration
        System.out.println("\nTesting successful registration:");

        try {
            Student newStudent = new Student(
                    "S999",
                    "Test Student",
                    "test.student@example.com"
            );

            registrationService.addStudent(newStudent);

            int before = registrationService.getEnrollments().size();

            registrationService.registerStudent("S999", "SWER348");

            int after = registrationService.getEnrollments().size();

            if (after == before + 1) {
                System.out.println("PASS: Registration successful");
            } else {
                System.out.println("FAIL: Enrollment count incorrect");
            }

        } catch (Exception e) {
            System.out.println("FAIL: " + e.getMessage());
        }

        System.out.println("\nTotal enrollments after tests: "
                + registrationService.getEnrollments().size());

        System.out.println("\nEnrollments loaded: "
                + enrollments.size());

        System.out.println("\nFirst 5 enrollments:");

        enrollments.stream()
                .limit(5)
                .forEach(System.out::println);


        // TEST 4: Weekly schedule
        ScheduleService scheduleService =
                new ScheduleService(registrationService);

        System.out.println("\nTesting weekly schedule for S001:");

        List<ClassMeeting> weeklySchedule =
                scheduleService.getWeeklySchedule("S001");

        weeklySchedule.forEach(System.out::println);

        System.out.println("Total weekly meetings: "
                + weeklySchedule.size());

        // TEST 5: Monday schedule
        System.out.println("\nTesting Monday schedule for S001:");

        List<ClassMeeting> mondayMeetings =
                scheduleService.getMeetingsOnDay(
                        "S001", DayOfWeek.MONDAY);

        mondayMeetings.forEach(System.out::println);

        // TEST 6: Total weekly class time
        System.out.println("\nTesting weekly class duration:");

        System.out.println("Total duration: "
                + scheduleService.getTotalWeeklyClassTime("S001"));

        // TEST 7: Course conflict detection
        System.out.println("\nTesting course conflict detection:");

        Course course1 = registrationService
                .findCourse("SWER348").orElseThrow();

        Course course2 = registrationService
                .findCourse("SWER313").orElseThrow();

        boolean conflict =
                scheduleService.coursesConflict(course1, course2);

        if (conflict) {
            System.out.println("PASS: Course conflict detected");
        } else {
            System.out.println("FAIL: Conflict not detected");
        }

    }
}
