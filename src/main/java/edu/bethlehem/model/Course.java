
package edu.bethlehem.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Course {

    private final String code;
    private final String title;
    private final int creditHours;
    private final List<ClassMeeting> meetings;

    public Course(String code, String title, int creditHours) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Invalid course code");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Invalid course title");
        }

        if (creditHours <= 0) {
            throw new IllegalArgumentException(
                    "Credit hours must be positive"
            );
        }

        this.code = code;
        this.title = title;
        this.creditHours = creditHours;
        this.meetings = new ArrayList<>();
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public int getCreditHours() {
        return creditHours;
    }

    public List<ClassMeeting> getMeetings() {
        return Collections.unmodifiableList(meetings);
    }

    public void addMeeting(ClassMeeting meeting) {
        if (meeting == null) {
            throw new IllegalArgumentException("Meeting cannot be null");
        }

        meetings.add(meeting);
    }

    @Override
    public String toString() {
        return code + " - " + title +
                " (" + creditHours + " credits)";
    }
}
