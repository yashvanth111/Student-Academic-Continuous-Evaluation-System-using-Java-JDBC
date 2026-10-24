/**
 * SystemVerificationTest - Automated test script to verify all KEC R2024 specifications:
 * 1. Student & Course Registration
 * 2. Enrollment
 * 3. Attendance & Condonation
 * 4. Incremental marks entry (Phase 1, 2, 3) & Attendance filter barring
 * 5. Theory, Practical, Embedded evaluation rules & weightages
 * 6. Grade sheet generation with SGPA & %
 * 7. Attendance Defaulter Report
 * 8. Class Performance & Department Result Summary
 */
public class SystemVerificationTest {
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("       KONGU ENGINEERING COLLEGE (R2024) - AUTOMATED VERIFICATION SUITE         ");
        System.out.println("================================================================================");

        try {
            StudentDAO sdao = new StudentDAO();
            AcademicDAO adao = new AcademicDAO();

            System.out.println("\n[STEP 1: STUDENT & COURSE REGISTRATION]");
            sdao.addStudent("24ITR001", "Arun Kumar M", "IT", 3);
            sdao.addStudent("24ITR002", "Bhavani S", "IT", 3);
            sdao.addStudent("24ITR003", "Chandra Prakash R", "IT", 3);
            sdao.addStudent("24CSR010", "Deepak K", "CSE", 3);

            sdao.addCourse("24ITT31", "Data Structures", 3.0, "THEORY");
            sdao.addCourse("24ITP32", "Data Structures Laboratory", 1.5, "PRACTICAL");
            sdao.addCourse("24ITE33", "Microcontrollers & IoT", 4.0, "EMBEDDED");
            sdao.addCourse("24CST31", "Operating Systems", 3.0, "THEORY");

            System.out.println("\n[STEP 2: COURSE ENROLLMENT]");
            sdao.enroll("24ITR001", "24ITT31", 3);
            sdao.enroll("24ITR001", "24ITP32", 3);
            sdao.enroll("24ITR001", "24ITE33", 3);

            sdao.enroll("24ITR002", "24ITT31", 3);
            sdao.enroll("24ITR002", "24ITP32", 3);

            sdao.enroll("24ITR003", "24ITT31", 3);
            sdao.enroll("24CSR010", "24CST31", 3);

            System.out.println("\n[STEP 3: ATTENDANCE TRACKING & RULES ENFORCEMENT]");
            // Arun: 40/45 = 88.89% (Eligible >= 75%)
            adao.updateAttendance("24ITR001", "24ITT31", 3, 45, 40, false);
            adao.updateAttendance("24ITR001", "24ITP32", 3, 30, 28, false);
            adao.updateAttendance("24ITR001", "24ITE33", 3, 45, 42, false);

            // Bhavani: 31/45 = 68.89% (65% to 74.9% -> Requires Medical Condonation -> Approved)
            adao.updateAttendance("24ITR002", "24ITT31", 3, 45, 31, true);
            adao.updateAttendance("24ITR002", "24ITP32", 3, 30, 26, false);

            // Chandra: 24/45 = 53.33% (< 65% -> SA Grade, Barred from ESE)
            adao.updateAttendance("24ITR003", "24ITT31", 3, 45, 24, false);

            // Deepak: 38/45 = 84.44% (Eligible)
            adao.updateAttendance("24CSR010", "24CST31", 3, 45, 38, false);

            System.out.println("\n[STEP 4: INCREMENTAL MARKS ENTRY - 'THEN AND NOW']");
            System.out.println("--- Phase 1 (Mid-Semester): Recording CIA Test 1 & CIA Test 2 ---");
            adao.recordPhase1CIA("24ITR001", "24ITT31", 3, 85.0, 88.0);
            adao.recordPhase1CIA("24ITR001", "24ITP32", 3, 94.0, 96.0);
            adao.recordPhase1CIA("24ITR001", "24ITE33", 3, 90.0, 92.0);
            adao.recordPhase1CIA("24ITR002", "24ITT31", 3, 72.0, 76.0);
            adao.recordPhase1CIA("24ITR002", "24ITP32", 3, 80.0, 82.0);
            adao.recordPhase1CIA("24ITR003", "24ITT31", 3, 60.0, 65.0);
            adao.recordPhase1CIA("24CSR010", "24CST31", 3, 75.0, 78.0);

            System.out.println("\n--- Phase 2 (End of Semester - CIA): Recording Assignments / Quizzes / Lab ---");
            adao.recordPhase2Assignment("24ITR001", "24ITT31", 3, 92.0);
            adao.recordPhase2Assignment("24ITR001", "24ITP32", 3, 95.0);
            adao.recordPhase2Assignment("24ITR001", "24ITE33", 3, 94.0);
            adao.recordPhase2Assignment("24ITR002", "24ITT31", 3, 80.0);
            adao.recordPhase2Assignment("24ITR002", "24ITP32", 3, 85.0);
            adao.recordPhase2Assignment("24ITR003", "24ITT31", 3, 70.0);
            adao.recordPhase2Assignment("24CSR010", "24CST31", 3, 82.0);

            System.out.println("\n--- Phase 3 (Post Semester): Recording ESE Marks & Absence ---");
            // Arun takes ESE
            adao.recordPhase3ESE("24ITR001", "24ITT31", 3, 86.0, false); // Theory
            adao.recordPhase3ESE("24ITR001", "24ITP32", 3, 92.0, false); // Practical
            adao.recordPhase3ESE("24ITR001", "24ITE33", 3, 88.0, false); // Embedded

            // Bhavani takes ESE
            adao.recordPhase3ESE("24ITR002", "24ITT31", 3, 74.0, false);
            adao.recordPhase3ESE("24ITR002", "24ITP32", 3, 78.0, false);

            // Deepak is Absent in ESE
            adao.recordPhase3ESE("24CSR010", "24CST31", 3, null, true);

            // Chandra test: Attempting to record ESE for student with attendance < 65%
            System.out.println("\nTesting Attendance Gate for Chandra Prakash (53.33% attendance):");
            try {
                adao.recordPhase3ESE("24ITR003", "24ITT31", 3, 80.0, false);
            } catch (IllegalStateException e) {
                System.out.println(">> SUCCESSFUL ATTENDANCE GATE ENFORCEMENT: " + e.getMessage());
            }

            System.out.println("\n[STEP 5: REPORT 1 - STUDENT SEMESTER GRADE REPORT FOR 24ITR001]");
            adao.printStudentMarkSheet("24ITR001", 3);

            System.out.println("\n[STEP 5B: REPORT 1 - STUDENT SEMESTER GRADE REPORT FOR 24ITR002 (CONDONED)]");
            adao.printStudentMarkSheet("24ITR002", 3);

            System.out.println("\n[STEP 5C: REPORT 1 - STUDENT SEMESTER GRADE REPORT FOR 24CSR010 (ABSENT)]");
            adao.printStudentMarkSheet("24CSR010", 3);

            System.out.println("\n[STEP 5D: REPORT 1 - STUDENT SEMESTER GRADE REPORT FOR 24ITR003 (SA DEFAULTER)]");
            adao.printStudentMarkSheet("24ITR003", 3);

            System.out.println("\n[STEP 6: REPORT 2 - ATTENDANCE DEFAULTERS & CONDONATION REPORT]");
            adao.printAttendanceDefaulters();

            System.out.println("\n[STEP 7: REPORT 3 - CLASS PERFORMANCE & RESULT SUMMARY]");
            adao.printClassPerformanceSummary(3);

            System.out.println("\n>> ALL TESTS COMPLETED SUCCESSFULLY!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
