
package edu.bethlehem.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Semester {

    private final String id;
    private final String name;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDateTime referenceTime;

    public Semester(String id, String name,
                    LocalDate startDate, LocalDate endDate,
                    LocalDateTime referenceTime) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid semester ID");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid semester name");
        }

        Objects.requireNonNull(startDate,
                "Start date cannot be null");
        Objects.requireNonNull(endDate,
                "End date cannot be null");
        Objects.requireNonNull(referenceTime,
                "Reference time cannot be null");

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date");
        }

        if (referenceTime.toLocalDate().isBefore(startDate)
                || referenceTime.toLocalDate().isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Reference time must be within semester");
        }

        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.referenceTime = referenceTime;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDateTime getReferenceTime() {
        return referenceTime;
    }

    @Override
    public String toString() {
        return name + " (" + startDate + " to " + endDate + ")";
    }
}
