
package edu.bethlehem.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Duration;

public class ClassMeeting {

    private final DayOfWeek day;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String room;

    public ClassMeeting(DayOfWeek day, LocalTime startTime,
                        LocalTime endTime, String room) {

        if (day == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Day and times cannot be null"
            );
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }

        if (room == null || room.isBlank()) {
            throw new IllegalArgumentException("Invalid room");
        }

        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getRoom() {
        return room;
    }

    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    public boolean conflictsWith(ClassMeeting other) {
        if (other == null) {
            throw new IllegalArgumentException(
                    "Other meeting cannot be null"
            );
        }

        return day == other.day
                && startTime.isBefore(other.endTime)
                && other.startTime.isBefore(endTime);
    }

    @Override
    public String toString() {
        return day + " " + startTime + "-" +
                endTime + " (" + room + ")";
    }
}
