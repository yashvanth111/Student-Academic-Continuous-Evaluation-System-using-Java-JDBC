import java.sql.SQLException;
import java.util.Scanner;

/**
 * MainApp - Menu-driven interface for Kongu Engineering College (R2024)
 * Student Academic & Continuous Evaluation System using Java JDBC.
 */
public class MainApp {
    private static final Scanner sc = new Scanner(System.in);
    private static final StudentDAO studentDAO = new StudentDAO();
    private static final AcademicDAO academicDAO = new AcademicDAO();

    public static void main(String[] args) {
        printBanner();

        // Check JDBC connection
        if (!DBConnection.testConnection()) {
            System.err.println("\n[ERROR] Unable to connect to MySQL database.");
            System.err.println("Please check:");
            System.err.println("1. MySQL service is running (e.g. XAMPP / MariaDB / MySQL on port 3306).");
            System.err.println("2. Schema 'kec_academic' exists (run kec_schema.sql).");
            System.err.println("3. DB credentials in DBConnection.java (Default: root / no password).");
            return;
        }
        System.out.println(">> Database Connected Successfully to MySQL [kec_academic].\n");

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInteger("Enter Choice (0-10): ");
            try {
                switch (choice) {
                    case 1:
                        handleStudentRegistration();
                        break;
                    case 2:
                        handleCourseCatalog();
                        break;
                    case 3:
                        handleEnrollment();
                        break;
                    case 4:
                        handleAttendance();
                        break;
                    case 5:
                        handleIncrementalMarksMenu();
                        break;
                    case 6:
                        handleMarkSheet();
                        break;
                    case 7:
                        handleDefaultersReport();
                        break;
                    case 8:
                        handleClassSummary();
                        break;
                    case 9:
                        handleViewCatalogs();
                        break;
                    case 10:
                        handleDataSeeder();
                        break;
                    case 0:
                        System.out.println("\nThank you for using KEC Academic & Continuous Evaluation System. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println(">> Invalid option! Please enter a number between 0 and 10.");
                }
            } catch (Exception e) {
                System.err.println("\n>> Error: " + e.getMessage());
            }
            if (running) {
                pause();
            }
        }
    }

    private static void printBanner() {
        System.out.println("================================================================================");
        System.out.println("            KONGU ENGINEERING COLLEGE (AUTONOMOUS) - REGULATION 2024             ");
        System.out.println("              STUDENT ACADEMIC & CONTINUOUS EVALUATION SYSTEM (JDBC)             ");
        System.out.println("================================================================================");
    }

    private static void printMainMenu() {
        System.out.println("\n============================= MAIN OPERATION MENU ==============================");
        System.out.println("  1. Student Registration (Add New Student)");
        System.out.println("  2. Course Catalog Management (Add Theory / Practical / Embedded Course)");
        System.out.println("  3. Course Enrollment (Enroll Student into Course for Semester)");
        System.out.println("  4. Attendance Tracking & Medical Condonation (Record Held vs Attended)");
        System.out.println("  5. Incremental Marks Entry (\"Then and Now\" Phased Evaluation)");
        System.out.println("  6. Student Semester Mark Sheet (Formal Grade Report & SGPA Calculation)");
        System.out.println("  7. Attendance Defaulter Report (< 65% Shortage SA / Condonation Status)");
        System.out.println("  8. Class Performance & Result Summary (Dept Pass/Fail & Class Avg SGPA)");
        System.out.println("  9. View Registered Students & Courses");
        System.out.println(" 10. Populate Sample Demo Dataset (Instant 1-Click Verification)");
        System.out.println("  0. Exit System");
        System.out.println("================================================================================");
    }

    private static void handleStudentRegistration() throws SQLException {
        System.out.println("\n--- [1] STUDENT REGISTRATION ---");
        String roll = readString("Roll Number (e.g., 24ITR001): ").toUpperCase();
        String name = readString("Student Full Name: ");
        String dept = readString("Department (e.g., IT, CSE, ECE, MECH): ").toUpperCase();
        int sem = readInteger("Current Semester (1 - 8): ");
        studentDAO.addStudent(roll, name, dept, sem);
    }

    private static void handleCourseCatalog() throws SQLException {
        System.out.println("\n--- [2] COURSE CATALOG MANAGEMENT ---");
        String code = readString("Course Code (e.g., 24ITR31 / 24ITT31): ").toUpperCase();
        String title = readString("Course Title: ");
        double credits = readDouble("Credits (e.g., 3.0, 4.0, 1.5): ");
        System.out.println("Select Course Type:");
        System.out.println("  1. THEORY     (40% CIA, 60% ESE, Min ESE Threshold: 27/60)");
        System.out.println("  2. PRACTICAL  (60% CIA, 40% ESE, Min ESE Threshold: 20/40)");
        System.out.println("  3. EMBEDDED   (50% CIA, 50% ESE, Min ESE Threshold: 22.5/50)");
        int tChoice = readInteger("Choice (1-3): ");
        String type;
        switch (tChoice) {
            case 1: type = "THEORY"; break;
            case 2: type = "PRACTICAL"; break;
            case 3: type = "EMBEDDED"; break;
            default:
                System.out.println("Invalid selection, defaulting to THEORY.");
                type = "THEORY";
        }
        studentDAO.addCourse(code, title, credits, type);
    }

    private static void handleEnrollment() throws SQLException {
        System.out.println("\n--- [3] COURSE ENROLLMENT ---");
        String roll = readString("Roll Number: ").toUpperCase();
        String code = readString("Course Code: ").toUpperCase();
        int sem = readInteger("Semester: ");
        studentDAO.enroll(roll, code, sem);
    }

    private static void handleAttendance() throws SQLException {
        System.out.println("\n--- [4] ATTENDANCE TRACKING & MEDICAL CONDONATION ---");
        String roll = readString("Roll Number: ").toUpperCase();
        String code = readString("Course Code: ").toUpperCase();
        int sem = readInteger("Semester: ");
        int held = readInteger("Total Classes Held: ");
        int attended = readInteger("Classes Attended: ");
        boolean condonation = readYesNo("Is Medical Condonation Approved? (y/n): ");
        academicDAO.updateAttendance(roll, code, sem, held, attended, condonation);
    }

    private static void handleIncrementalMarksMenu() throws SQLException {
        System.out.println("\n----------------- [5] INCREMENTAL MARKS ENTRY (\"THEN AND NOW\") -----------------");
        System.out.println("  A. Phase 1 (Mid-Semester)  : Record CIA Test 1 (0-100) & CIA Test 2 (0-100)");
        System.out.println("  B. Phase 2 (End-Semester)  : Record Assignments / Quizzes / Lab Evaluations (0-100)");
        System.out.println("  C. Phase 3 (Post-Semester) : Record ESE Marks (0-100) or Mark Absence (AB)");
        System.out.println("  D. Single Component Update : Update any specific CIA or ESE mark");
        System.out.println("  E. View Current Marks      : Check marks entered so far for an enrollment");
        System.out.println("--------------------------------------------------------------------------------");
        String opt = readString("Select Phase (A/B/C/D/E): ").trim().toUpperCase();

        if (opt.equals("A")) {
            System.out.println("\n>> [Phase 1 Entry: Mid-Semester CIA Tests]");
            String roll = readString("Roll Number: ").toUpperCase();
            String code = readString("Course Code: ").toUpperCase();
            int sem = readInteger("Semester: ");
            Double c1 = readDoubleOrNull("CIA Test 1 Mark (0-100, or press Enter to skip): ");
            Double c2 = readDoubleOrNull("CIA Test 2 Mark (0-100, or press Enter to skip): ");
            academicDAO.recordPhase1CIA(roll, code, sem, c1, c2);
        } else if (opt.equals("B")) {
            System.out.println("\n>> [Phase 2 Entry: End-Semester CIA Assignment/Quiz/Lab Evaluation]");
            String roll = readString("Roll Number: ").toUpperCase();
            String code = readString("Course Code: ").toUpperCase();
            int sem = readInteger("Semester: ");
            Double asgn = readDoubleOrNull("Assignments / Quizzes / Lab Mark (0-100): ");
            academicDAO.recordPhase2Assignment(roll, code, sem, asgn);
        } else if (opt.equals("C")) {
            System.out.println("\n>> [Phase 3 Entry: Post-Semester End Semester Examination (ESE)]");
            System.out.println(" * Note: System will check attendance eligibility rules (<65% SA barred, 65-74.9% condonation).");
            String roll = readString("Roll Number: ").toUpperCase();
            String code = readString("Course Code: ").toUpperCase();
            int sem = readInteger("Semester: ");
            boolean isAbsent = readYesNo("Was student Absent in ESE? (y/n): ");
            Double eseMark = null;
            if (!isAbsent) {
                eseMark = readDouble("ESE Marks Obtained (0-100): ");
            }
            academicDAO.recordPhase3ESE(roll, code, sem, eseMark, isAbsent);
        } else if (opt.equals("D")) {
            System.out.println("\n>> [Single Component Update]");
            String roll = readString("Roll Number: ").toUpperCase();
            String code = readString("Course Code: ").toUpperCase();
            int sem = readInteger("Semester: ");
            String comp = readString("Component (CIA1 / CIA2 / ASSIGNMENT / ESE): ").toUpperCase();
            boolean isAbsent = comp.equals("ESE") && readYesNo("Mark student as Absent in ESE? (y/n): ");
            Double mark = isAbsent ? null : readDoubleOrNull("Mark (0-100): ");
            academicDAO.updateSingleAssessment(roll, code, sem, comp, mark, isAbsent);
        } else if (opt.equals("E")) {
            String roll = readString("Roll Number: ").toUpperCase();
            String code = readString("Course Code: ").toUpperCase();
            int sem = readInteger("Semester: ");
            AcademicDAO.EnrollmentInfo info = academicDAO.getEnrollmentDetails(roll, code, sem);
            if (info == null) {
                System.out.println("No enrollment record found.");
            } else {
                System.out.println("\n---------------- CURRENT ENROLLMENT & MARKS STATUS ----------------");
                System.out.printf("Student : %s (%s) | Dept: %s | Sem: %d%n", info.studentName, info.rollNo, info.department, info.semester);
                System.out.printf("Course  : %s - %s (%s, %.1f Credits)%n", info.courseCode, info.courseTitle, info.courseType, info.credits);
                System.out.printf("Attd    : %d / %d (%.2f%%) -> Status: %s%n", info.classesAttended, info.totalClasses, info.attendancePct, info.attendanceStatus);
                System.out.printf("CIA 1   : %s | CIA 2: %s | Asgn/Lab: %s | CIA Raw Avg: %.2f%n",
                    info.cia1 == null ? "--" : String.format("%.2f", info.cia1),
                    info.cia2 == null ? "--" : String.format("%.2f", info.cia2),
                    info.assignmentMark == null ? "--" : String.format("%.2f", info.assignmentMark),
                    info.ciaRaw);
                System.out.printf("ESE Mark: %s (Absent: %s)%n",
                    info.eseMark == null ? "--" : String.format("%.2f", info.eseMark),
                    info.eseAbsent ? "YES" : "NO");
                System.out.printf("Grade   : %s (GP: %d) | Result: %s%n", info.grade, info.gradePoint, info.resultStatus);
                System.out.println("-------------------------------------------------------------------");
            }
        } else {
            System.out.println("Invalid selection.");
        }
    }

    private static void handleMarkSheet() throws SQLException {
        System.out.println("\n--- [6] STUDENT SEMESTER MARK SHEET & SGPA ---");
        String roll = readString("Roll Number: ").toUpperCase();
        int sem = readInteger("Semester: ");
        academicDAO.printStudentMarkSheet(roll, sem);
    }

    private static void handleDefaultersReport() throws SQLException {
        academicDAO.printAttendanceDefaulters();
    }

    private static void handleClassSummary() throws SQLException {
        System.out.println("\n--- [8] CLASS PERFORMANCE & RESULT SUMMARY ---");
        int sem = readInteger("Enter Semester to Summarize (1 - 8): ");
        academicDAO.printClassPerformanceSummary(sem);
    }

    private static void handleViewCatalogs() throws SQLException {
        studentDAO.showStudents();
        studentDAO.showCourses();
    }

    private static void handleDataSeeder() throws SQLException {
        System.out.println("\n--- [10] POPULATE DEMO DATASET ---");
        if (readYesNo("This will insert sample KEC students, courses, enrollments and marks. Proceed? (y/n): ")) {
            academicDAO.seedDemoData();
        }
    }

    // Helper input methods
    private static String readString(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static int readInteger(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = sc.nextLine().trim();
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = sc.nextLine().trim();
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid numeric value.");
            }
        }
    }

    private static Double readDoubleOrNull(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            if (line.isEmpty()) return null;
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number or press Enter to skip.");
            }
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim().toLowerCase();
            if (line.equals("y") || line.equals("yes")) return true;
            if (line.equals("n") || line.equals("no")) return false;
            System.out.println("Please enter 'y' for Yes or 'n' for No.");
        }
    }

    private static void pause() {
        System.out.print("\nPress Enter to return to main menu...");
        sc.nextLine();
    }
}
