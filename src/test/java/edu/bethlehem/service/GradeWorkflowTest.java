package edu.bethlehem.service;

import edu.bethlehem.model.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GradeWorkflowTest {
    private final Semester semester = new Semester("SEM1", "Fall",
            LocalDate.of(2026,9,7), LocalDate.of(2027,1,15),
            LocalDateTime.of(2027,1,10,12,0));
    private final Student student = new Student("S1","Test Student","s1@test.edu");
    private final Course course = new Course("C1","Course One",3);
    private final Enrollment enrollment = new Enrollment("E1",student,course,semester);

    private AssessmentService service(Assessment... assessments) {
        return new AssessmentService(semester, List.of(enrollment), List.of(assessments));
    }

    @Test void assessmentValidatesAndSortsAndFindsInclusiveRange() {
        Assessment later = new Assessment("A2",course,"Final",100,50,LocalDateTime.of(2027,1,5,10,0));
        Assessment earlier = new Assessment("A1",course,"Quiz",20,50,LocalDateTime.of(2026,10,1,10,0));
        AssessmentService s = service(later, earlier);
        assertEquals(List.of(earlier,later), s.getAssessments());
        assertEquals(2, s.findAssessmentsBetween(earlier.getDeadline(),later.getDeadline()).size());
        assertThrows(IllegalArgumentException.class, () -> s.findAssessmentsBetween(later.getDeadline(),earlier.getDeadline()));
    }

    @Test void assessmentWeightCannotExceedOneHundredAndDeadlineMustBeInSemester() {
        Assessment a1 = new Assessment("A1",course,"Quiz",20,60,LocalDateTime.of(2026,10,1,10,0));
        AssessmentService s = service(a1);
        assertThrows(IllegalArgumentException.class, () -> s.addAssessment(
                new Assessment("A2",course,"Final",100,41,LocalDateTime.of(2027,1,5,10,0))));
        assertThrows(IllegalArgumentException.class, () -> s.addAssessment(
                new Assessment("A3",course,"Outside",100,10,LocalDateTime.of(2027,2,1,10,0))));
    }

    @Test void gradeCalculationDistinguishesMissingFromZeroAndAllowsUpdating() {
        Assessment a1 = new Assessment("A1",course,"Quiz",20,20,LocalDateTime.of(2026,10,1,10,0));
        Assessment a2 = new Assessment("A2",course,"Project",50,30,LocalDateTime.of(2026,12,1,10,0));
        Assessment a3 = new Assessment("A3",course,"Final",100,50,LocalDateTime.of(2027,1,5,10,0));
        AssessmentService s = service(a1,a2,a3);
        GradeService grades = new GradeService(s);
        assertTrue(grades.calculateFinalNumericGrade(enrollment).isEmpty());
        grades.recordMark(enrollment,a1,0.0);
        grades.recordMark(enrollment,a2,42);
        grades.recordMark(enrollment,a3,70);
        assertEquals(60.2, grades.calculateFinalNumericGrade(enrollment).orElseThrow(), 1e-9);
        grades.recordMark(enrollment,a1,16);
        assertEquals(76.2, grades.calculateFinalNumericGrade(enrollment).orElseThrow(), 1e-9);
        assertEquals(GradeService.CourseOutcome.PASSED, grades.classifyOutcome(enrollment));
    }

    @Test void rejectsInvalidMarkAndWrongCourseEnrollment() {
        Assessment a = new Assessment("A1",course,"Quiz",20,100,LocalDateTime.of(2026,10,1,10,0));
        AssessmentService s = service(a);
        GradeService grades = new GradeService(s);
        assertThrows(IllegalArgumentException.class, () -> grades.recordMark(enrollment,a,Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> grades.recordMark(enrollment,a,21));
        Course other = new Course("C2","Other",3);
        Enrollment otherEnrollment = new Enrollment("E2",student,other,semester);
        assertThrows(IllegalArgumentException.class, () -> grades.recordMark(otherEnrollment,a,10));
    }

    @Test void letterGradeBoundariesAreCorrect() {
        assertEquals(LetterGrade.B, LetterGrade.fromNumericGrade(84.99));
        assertEquals(LetterGrade.B_PLUS, LetterGrade.fromNumericGrade(85));
        assertEquals(LetterGrade.F, LetterGrade.fromNumericGrade(59.99));
        assertEquals(LetterGrade.A, LetterGrade.fromNumericGrade(90));
        assertThrows(IllegalArgumentException.class, () -> LetterGrade.fromNumericGrade(100.1));
    }

    @Test void gpaIsCreditWeightedAndProvisionalWhenAnyCourseIncomplete() {
        Assessment a = new Assessment("A1",course,"Final",100,100,LocalDateTime.of(2027,1,5,10,0));
        Course second = new Course("C2","Course Two",1);
        Enrollment e2 = new Enrollment("E2",student,second,semester);
        Assessment b = new Assessment("B1",second,"Final",100,100,LocalDateTime.of(2027,1,5,10,0));
        AssessmentService s = new AssessmentService(semester,List.of(enrollment,e2),List.of(a,b));
        GradeService grades = new GradeService(s);
        grades.recordMark(enrollment,a,90); // A = 4.0, 3 credits
        GpaService gpa = new GpaService(grades,List.of(enrollment,e2));
        assertEquals(4.0,gpa.calculateGpa("S1").orElseThrow(),1e-9);
        assertEquals(GpaService.GpaStatus.PROVISIONAL,gpa.createReport("S1").status().orElseThrow());
        grades.recordMark(e2,b,60); // D = 1.0, 1 credit
        assertEquals(3.25,gpa.calculateGpa("S1").orElseThrow(),1e-9);
        assertEquals(GpaService.GpaStatus.FINAL,gpa.createReport("S1").status().orElseThrow());
        assertEquals("3.25",gpa.formatGpa(gpa.calculateGpa("S1")));
    }

    @Test void allFailedCoursesProduceZeroGpaNotEmpty() {
        Assessment a = new Assessment("A1",course,"Final",100,100,LocalDateTime.of(2027,1,5,10,0));
        AssessmentService s = service(a);
        GradeService grades = new GradeService(s);
        grades.recordMark(enrollment,a,20);
        GpaService gpa = new GpaService(grades,List.of(enrollment));
        assertEquals(0.0,gpa.calculateGpa("S1").orElseThrow(),1e-9);
    }

    @Test void upcomingAndOverdueQueriesUseProvidedReferenceTime() {
        Assessment upcoming = new Assessment("A1",course,"Final",100,50,LocalDateTime.of(2027,1,10,12,0));
        Assessment overdue = new Assessment("A2",course,"Quiz",20,50,LocalDateTime.of(2026,10,1,10,0));
        AssessmentService s = service(upcoming,overdue);
        assertEquals(List.of(upcoming),s.findUpcomingForStudent("S1",LocalDateTime.of(2027,1,10,12,0)));
        assertEquals(List.of(overdue),s.findOverdueWithoutMark("S1",LocalDateTime.of(2027,1,10,12,0)));
        assertEquals(upcoming,s.findNextUpcoming("S1",LocalDateTime.of(2027,1,10,12,0)).orElseThrow());
    }

    @Test void sixStreamReportsHandleEmptyAndAvailableValues() {
        Assessment a = new Assessment("A1",course,"Final",100,100,LocalDateTime.of(2027,1,5,10,0));
        AssessmentService s = service(a);
        GradeService grades = new GradeService(s);
        grades.recordMark(enrollment,a,95);
        GpaService gpa = new GpaService(grades,List.of(enrollment));
        StreamReportService reports = new StreamReportService(List.of(enrollment),s,grades,gpa);
        assertEquals(List.of(enrollment),reports.topStudentsInCourse("C1",1));
        assertThrows(IllegalArgumentException.class,() -> reports.topStudentsInCourse("C1",0));
        assertEquals(1,reports.courseCompletionSummary("C1").passed());
        assertEquals(1L,reports.letterGradeDistribution("C1").get(LetterGrade.A));
        assertEquals(1,reports.courseRanking().size());
        assertEquals(1,reports.semesterGpaRanking().size());
        assertTrue(reports.missingResultsReport().isEmpty());
    }
}
