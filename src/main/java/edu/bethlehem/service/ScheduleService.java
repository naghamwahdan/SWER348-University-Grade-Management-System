
package edu.bethlehem.service;

import edu.bethlehem.model.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScheduleService {

    private final RegistrationService registrationService;

    public ScheduleService(RegistrationService registrationService) {
        this.registrationService =
                Objects.requireNonNull(registrationService);
    }

    public List<ClassMeeting> getWeeklySchedule(String studentId) {
        return registrationService.getStudentCourses(studentId)
                .stream()
                .flatMap(course -> course.getMeetings().stream())
                .sorted(Comparator
                        .comparing(ClassMeeting::getDay)
                        .thenComparing(ClassMeeting::getStartTime))
                .toList();
    }

    public List<ClassMeeting> getMeetingsOnDay(
            String studentId, DayOfWeek day) {

        Objects.requireNonNull(day, "Day cannot be null");

        return getWeeklySchedule(studentId)
                .stream()
                .filter(meeting -> meeting.getDay() == day)
                .toList();
    }

    public Duration getTotalWeeklyClassTime(String studentId) {
        return getWeeklySchedule(studentId)
                .stream()
                .map(ClassMeeting::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    public boolean coursesConflict(Course first, Course second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        return first.getMeetings()
                .stream()
                .anyMatch(m1 -> second.getMeetings()
                        .stream()
                        .anyMatch(m1::conflictsWith));
    }
}
