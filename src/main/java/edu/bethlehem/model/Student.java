
package edu.bethlehem.model;

public class Student {

    private String id;
    private String name;
    private String email;

    public Student(String id, String name, String email) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Invalid student ID");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid name");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Invalid email");
        }

        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return id + " - " + name + " (" + email + ")";
    }
}
