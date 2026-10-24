import java.sql.*;
import java.util.*;

/**
 * AcademicDAO - Evaluation Engine and Academic Reporting according to KEC R2024.
 */
public class AcademicDAO {

    public static class EnrollmentInfo {
        public long enrollmentId;
        public String rollNo;
        public String studentName;
        public String department;
        public String courseCode;
        public String courseTitle;
        public double credits;
        public String courseType;
        public int semester;
        public int totalClasses;
        public int classesAttended;
        public boolean condonationApproved;
        public Double cia1;
        public Double cia2;
        public Double assignmentMark;
        public Double eseMark;
        public boolean eseAbsent;

        // Calculated evaluation metrics
        public double attendancePct;
        public boolean isAttendanceEligible;
        public String attendanceStatus; // ELIGIBLE, CONDONATION_APPROVED, CONDONATION_REQUIRED, SHORTAGE (SA)
        public double ciaRaw;
        public double ciaWeight;
        public double eseWeight;
        public double ciaWeighted;
        public Double eseWeighted;
        public Double totalMarks;
        public double eseThreshold;
        public String grade;
        public int gradePoint;
        public String resultStatus; // PASS, RA, SA, AB, PENDING
    }

    public long getEnrollmentId(String rollNo, String courseCode, int semester) throws SQLException {
        String sql = "SELECT enrollment_id FROM enrollments WHERE roll_no = ? AND course_code = ? AND semester = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setString(2, courseCode.trim().toUpperCase());
            ps.setInt(3, semester);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No enrollment record found for Roll: " + rollNo + ", Course: " + courseCode + ", Semester: " + semester);
                }
                return rs.getLong(1);
            }
        }
    }

    // ========================================================================
    // Attendance Tracking & Condonation
    // ========================================================================
    public void updateAttendance(String rollNo, String courseCode, int semester,
                                 int totalHeld, int attended, boolean condonationApproved) throws SQLException {
        if (totalHeld < 0 || attended < 0 || attended > totalHeld) {
            throw new IllegalArgumentException("Invalid attendance: Ensure 0 <= Attended (" + attended + ") <= Total Held (" + totalHeld + ").");
        }
        long enrollmentId = getEnrollmentId(rollNo, courseCode, semester);
        String sql = "UPDATE enrollments SET total_classes = ?, classes_attended = ?, medical_condonation_approved = ? WHERE enrollment_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, totalHeld);
            ps.setInt(2, attended);
            ps.setBoolean(3, condonationApproved);
            ps.setLong(4, enrollmentId);
            ps.executeUpdate();
        }
        double pct = (totalHeld == 0) ? 0.0 : (attended * 100.0 / totalHeld);
        System.out.printf(">> Attendance updated for %s in %s: %d/%d (%.2f%%). Condonation: %s%n",
            rollNo.toUpperCase(), courseCode.toUpperCase(), attended, totalHeld, pct, condonationApproved ? "Approved" : "Not Approved");
    }

    public void approveMedicalCondonation(String rollNo, String courseCode, int semester, boolean approve) throws SQLException {
        long enrollmentId = getEnrollmentId(rollNo, courseCode, semester);
        String sql = "UPDATE enrollments SET medical_condonation_approved = ? WHERE enrollment_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, approve);
            ps.setLong(2, enrollmentId);
            ps.executeUpdate();
        }
        System.out.println(">> Medical condonation approval status set to: " + approve + " for " + rollNo + " in " + courseCode);
    }

    // ========================================================================
    // Incremental Marks Entry ("Then and Now")
    // ========================================================================

    /**
     * Phase 1 (Mid-Semester): Record CIA Test 1 (0-100) and CIA Test 2 (0-100).
     */
    public void recordPhase1CIA(String rollNo, String courseCode, int semester, Double cia1, Double cia2) throws SQLException {
        if (cia1 != null && (cia1 < 0 || cia1 > 100)) throw new IllegalArgumentException("CIA 1 mark must be between 0 and 100.");
        if (cia2 != null && (cia2 < 0 || cia2 > 100)) throw new IllegalArgumentException("CIA 2 mark must be between 0 and 100.");
        long id = getEnrollmentId(rollNo, courseCode, semester);
        String sql = "UPDATE assessment_marks SET cia1 = ?, cia2 = ? WHERE enrollment_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (cia1 == null) ps.setNull(1, Types.DECIMAL); else ps.setDouble(1, cia1);
            if (cia2 == null) ps.setNull(2, Types.DECIMAL); else ps.setDouble(2, cia2);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
        System.out.printf(">> [Phase 1 Recorded] Roll: %s | Course: %s | CIA1: %s | CIA2: %s%n",
            rollNo.toUpperCase(), courseCode.toUpperCase(),
            cia1 == null ? "--" : String.format("%.2f", cia1),
            cia2 == null ? "--" : String.format("%.2f", cia2));
    }

    /**
     * Phase 2 (End of Semester - CIA): Record Assignments / Quizzes / Lab Evaluations (0-100).
     */
    public void recordPhase2Assignment(String rollNo, String courseCode, int semester, Double assignmentMark) throws SQLException {
        if (assignmentMark != null && (assignmentMark < 0 || assignmentMark > 100)) {
            throw new IllegalArgumentException("Assignment / Quiz / Lab mark must be between 0 and 100.");
        }
        long id = getEnrollmentId(rollNo, courseCode, semester);
        String sql = "UPDATE assessment_marks SET assignment_mark = ? WHERE enrollment_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (assignmentMark == null) ps.setNull(1, Types.DECIMAL); else ps.setDouble(1, assignmentMark);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
        System.out.printf(">> [Phase 2 Recorded] Roll: %s | Course: %s | Assignment/Quiz/Lab: %s%n",
            rollNo.toUpperCase(), courseCode.toUpperCase(),
            assignmentMark == null ? "--" : String.format("%.2f", assignmentMark));
    }

    /**
     * Phase 3 (Post Semester): Record End Semester Examination (ESE) Marks (0-100) or Mark Absence (AB).
     * Enforces KEC R2024 Attendance eligibility rules:
     * - >= 75%: Eligible
     * - 65% - 74.9%: Requires medical condonation approval
     * - < 65%: Barred from ESE (Award SA)
     */
    public void recordPhase3ESE(String rollNo, String courseCode, int semester, Double eseMark, boolean isAbsent) throws SQLException {
        EnrollmentInfo info = getEnrollmentDetails(rollNo, courseCode, semester);
        if (info == null) {
            throw new SQLException("Enrollment record not found.");
        }

        // Attendance eligibility verification
        if (info.totalClasses > 0) {
            if (info.attendancePct < 65.0) {
                throw new IllegalStateException("ELIGIBILITY VIOLATION: Student has " + String.format("%.2f", info.attendancePct) + 
                    "% attendance (< 65%). BARRED from appearing in ESE (SA Grade assigned).");
            } else if (info.attendancePct < 75.0 && !info.condonationApproved) {
                throw new IllegalStateException("ELIGIBILITY VIOLATION: Student has " + String.format("%.2f", info.attendancePct) + 
                    "% attendance (65%-74.9%) but Medical Condonation has NOT been approved. BARRED from ESE.");
            }
        }

        if (!isAbsent) {
            if (eseMark == null) {
                throw new IllegalArgumentException("Must specify ESE marks or mark as Absent.");
            }
            if (eseMark < 0 || eseMark > 100) {
                throw new IllegalArgumentException("ESE mark must be between 0 and 100.");
            }
        } else {
            eseMark = null;
        }

        String sql = "UPDATE assessment_marks SET ese_mark = ?, ese_absent = ? WHERE enrollment_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (eseMark == null) ps.setNull(1, Types.DECIMAL); else ps.setDouble(1, eseMark);
            ps.setBoolean(2, isAbsent);
            ps.setLong(3, info.enrollmentId);
            ps.executeUpdate();
        }
        System.out.printf(">> [Phase 3 Recorded] Roll: %s | Course: %s | ESE: %s | Absent: %s%n",
            rollNo.toUpperCase(), courseCode.toUpperCase(),
            eseMark == null ? "--" : String.format("%.2f", eseMark),
            isAbsent ? "YES (AB)" : "NO");
    }

    /**
     * Single Assessment Component Update (for CIA1, CIA2, ASSIGNMENT, or ESE)
     */
    public void updateSingleAssessment(String rollNo, String courseCode, int semester,
                                       String component, Double mark, boolean isAbsent) throws SQLException {
        String comp = component.trim().toUpperCase();
        switch (comp) {
            case "CIA1":
                if (mark != null && (mark < 0 || mark > 100)) throw new IllegalArgumentException("Marks must be between 0 and 100.");
                updateColumn(rollNo, courseCode, semester, "cia1", mark);
                break;
            case "CIA2":
                if (mark != null && (mark < 0 || mark > 100)) throw new IllegalArgumentException("Marks must be between 0 and 100.");
                updateColumn(rollNo, courseCode, semester, "cia2", mark);
                break;
            case "ASSIGNMENT":
            case "LAB":
            case "QUIZ":
                if (mark != null && (mark < 0 || mark > 100)) throw new IllegalArgumentException("Marks must be between 0 and 100.");
                updateColumn(rollNo, courseCode, semester, "assignment_mark", mark);
                break;
            case "ESE":
                recordPhase3ESE(rollNo, courseCode, semester, mark, isAbsent);
                break;
            default:
                throw new IllegalArgumentException("Invalid component. Choose CIA1, CIA2, ASSIGNMENT, or ESE.");
        }
    }

    private void updateColumn(String rollNo, String courseCode, int semester, String column, Double value) throws SQLException {
        long id = getEnrollmentId(rollNo, courseCode, semester);
        String sql = "UPDATE assessment_marks SET " + column + " = ? WHERE enrollment_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (value == null) ps.setNull(1, Types.DECIMAL); else ps.setDouble(1, value);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
        System.out.println(">> Assessment component [" + column + "] updated successfully.");
    }

    // ========================================================================
    // Evaluation Engine & Metrics Calculator (KEC R2024 Rules)
    // ========================================================================

    public EnrollmentInfo evaluateRecord(ResultSet rs) throws SQLException {
        EnrollmentInfo info = new EnrollmentInfo();
        info.enrollmentId = rs.getLong("enrollment_id");
        info.rollNo = rs.getString("roll_no");
        info.studentName = rs.getString("student_name");
        info.department = rs.getString("department");
        info.courseCode = rs.getString("course_code");
        info.courseTitle = rs.getString("title");
        info.credits = rs.getDouble("credits");
        info.courseType = rs.getString("course_type").toUpperCase();
        info.semester = rs.getInt("semester");
        info.totalClasses = rs.getInt("total_classes");
        info.classesAttended = rs.getInt("classes_attended");
        info.condonationApproved = rs.getBoolean("medical_condonation_approved");

        info.cia1 = getDoubleOrNull(rs, "cia1");
        info.cia2 = getDoubleOrNull(rs, "cia2");
        info.assignmentMark = getDoubleOrNull(rs, "assignment_mark");
        info.eseMark = getDoubleOrNull(rs, "ese_mark");
        info.eseAbsent = rs.getBoolean("ese_absent");

        // 1. Attendance Calculation
        info.attendancePct = (info.totalClasses == 0) ? 100.0 : (info.classesAttended * 100.0 / info.totalClasses);
        if (info.attendancePct >= 75.0) {
            info.isAttendanceEligible = true;
            info.attendanceStatus = "ELIGIBLE";
        } else if (info.attendancePct >= 65.0) {
            if (info.condonationApproved) {
                info.isAttendanceEligible = true;
                info.attendanceStatus = "CONDONED";
            } else {
                info.isAttendanceEligible = false;
                info.attendanceStatus = "CONDONATION_REQ";
            }
        } else {
            info.isAttendanceEligible = false;
            info.attendanceStatus = "SHORTAGE (SA)";
        }

        // 2. Weightages based on KEC R2024 course type
        switch (info.courseType) {
            case "THEORY":
                info.ciaWeight = 40.0;
                info.eseWeight = 60.0;
                info.eseThreshold = 27.0; // 45% of 60
                break;
            case "PRACTICAL":
                info.ciaWeight = 60.0;
                info.eseWeight = 40.0;
                info.eseThreshold = 20.0; // 50% of 40
                break;
            case "EMBEDDED":
            default:
                info.ciaWeight = 50.0;
                info.eseWeight = 50.0;
                info.eseThreshold = 22.5; // 45% of 50
                break;
        }

        // 3. CIA Raw Score (average of recorded CIA components)
        double ciaSum = 0;
        int ciaCount = 0;
        if (info.cia1 != null) { ciaSum += info.cia1; ciaCount++; }
        if (info.cia2 != null) { ciaSum += info.cia2; ciaCount++; }
        if (info.assignmentMark != null) { ciaSum += info.assignmentMark; ciaCount++; }
        info.ciaRaw = (ciaCount == 0) ? 0.0 : (ciaSum / ciaCount);
        info.ciaWeighted = (info.ciaRaw * info.ciaWeight) / 100.0;

        // 4. ESE and Overall Grade Determination
        if (!info.isAttendanceEligible) {
            // Shortage of attendance -> SA grade (0 GP)
            info.grade = "SA";
            info.gradePoint = 0;
            info.resultStatus = "SA";
            info.eseWeighted = 0.0;
            info.totalMarks = info.ciaWeighted;
        } else if (info.eseAbsent) {
            info.grade = "AB";
            info.gradePoint = 0;
            info.resultStatus = "AB";
            info.eseWeighted = 0.0;
            info.totalMarks = info.ciaWeighted;
        } else if (info.eseMark == null) {
            // ESE not yet entered
            info.grade = "--";
            info.gradePoint = 0;
            info.resultStatus = "PENDING";
            info.eseWeighted = null;
            info.totalMarks = null;
        } else {
            info.eseWeighted = (info.eseMark * info.eseWeight) / 100.0;
            double grandTotal = info.ciaWeighted + info.eseWeighted;
            info.totalMarks = grandTotal;

            // KEC R2024 Rule: ESE Minimum Threshold and Overall Pass Minimum = 50 Marks
            if (info.eseWeighted < info.eseThreshold || grandTotal < 50.0) {
                info.grade = "RA";
                info.gradePoint = 0;
                info.resultStatus = "FAIL (RA)";
            } else {
                info.resultStatus = "PASS";
                long rounded = Math.round(grandTotal);
                if (rounded >= 91) {
                    info.grade = "O";
                    info.gradePoint = 10;
                } else if (rounded >= 81) {
                    info.grade = "A+";
                    info.gradePoint = 9;
                } else if (rounded >= 71) {
                    info.grade = "A";
                    info.gradePoint = 8;
                } else if (rounded >= 61) {
                    info.grade = "B+";
                    info.gradePoint = 7;
                } else {
                    info.grade = "B";
                    info.gradePoint = 6;
                }
            }
        }
        return info;
    }

    public EnrollmentInfo getEnrollmentDetails(String rollNo, String courseCode, int semester) throws SQLException {
        String sql = "SELECT e.enrollment_id, e.roll_no, s.student_name, s.department, " +
                     "e.course_code, c.title, c.credits, c.course_type, e.semester, " +
                     "e.total_classes, e.classes_attended, e.medical_condonation_approved, " +
                     "m.cia1, m.cia2, m.assignment_mark, m.ese_mark, m.ese_absent " +
                     "FROM enrollments e " +
                     "JOIN students s ON s.roll_no = e.roll_no " +
                     "JOIN courses c ON c.course_code = e.course_code " +
                     "LEFT JOIN assessment_marks m ON m.enrollment_id = e.enrollment_id " +
                     "WHERE e.roll_no = ? AND e.course_code = ? AND e.semester = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setString(2, courseCode.trim().toUpperCase());
            ps.setInt(3, semester);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return evaluateRecord(rs);
                }
                return null;
            }
        }
    }

    // ========================================================================
    // REPORT 1: Student Semester Mark Sheet & SGPA
    // ========================================================================
    public void printStudentMarkSheet(String rollNo, int semester) throws SQLException {
        String sql = "SELECT e.enrollment_id, e.roll_no, s.student_name, s.department, " +
                     "e.course_code, c.title, c.credits, c.course_type, e.semester, " +
                     "e.total_classes, e.classes_attended, e.medical_condonation_approved, " +
                     "m.cia1, m.cia2, m.assignment_mark, m.ese_mark, m.ese_absent " +
                     "FROM enrollments e " +
                     "JOIN students s ON s.roll_no = e.roll_no " +
                     "JOIN courses c ON c.course_code = e.course_code " +
                     "LEFT JOIN assessment_marks m ON m.enrollment_id = e.enrollment_id " +
                     "WHERE e.roll_no = ? AND e.semester = ? " +
                     "ORDER BY c.course_code";

        List<EnrollmentInfo> list = new ArrayList<>();
        String studentName = "";
        String dept = "";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setInt(2, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EnrollmentInfo info = evaluateRecord(rs);
                    studentName = info.studentName;
                    dept = info.department;
                    list.add(info);
                }
            }
        }

        if (list.isEmpty()) {
            System.out.println(">> No enrollment records found for Roll No: " + rollNo + " in Semester " + semester);
            return;
        }

        double totalCreditsEnrolled = 0;
        double totalCreditsEarned = 0;
        double totalCreditPoints = 0;
        boolean hasPending = false;
        boolean hasFail = false;

        System.out.println("\n" + "=".repeat(105));
        System.out.println("                   KONGU ENGINEERING COLLEGE (AUTONOMOUS), PERUNDURAI, ERODE");
        System.out.println("                   OFFICE OF THE CONTROLLER OF EXAMINATIONS - REGULATION 2024");
        System.out.println("                               STUDENT SEMESTER GRADE REPORT");
        System.out.println("=".repeat(105));
        System.out.printf("  Roll Number   : %-25s Department       : %-25s%n", rollNo.toUpperCase(), dept);
        System.out.printf("  Student Name  : %-25s Current Semester : %-25d%n", studentName, semester);
        System.out.println("-".repeat(105));
        System.out.printf("%-10s %-24s %-9s %-4s %-7s %-7s %-7s %-7s %-7s %-6s %-4s %-10s%n",
            "Course", "Course Title", "Type", "Cr", "Attd%", "CIA-Raw", "CIA-Wtd", "ESE-Raw", "ESE-Wtd", "Total", "Grd", "Status");
        System.out.println("-".repeat(105));

        for (EnrollmentInfo e : list) {
            String title = e.courseTitle.length() > 23 ? e.courseTitle.substring(0, 20) + "..." : e.courseTitle;
            String eseRawStr = e.eseAbsent ? "AB" : (e.eseMark == null ? "--" : String.format("%.1f", e.eseMark));
            String eseWtdStr = e.eseAbsent ? "0.0" : (e.eseWeighted == null ? "--" : String.format("%.1f", e.eseWeighted));
            String totStr = (e.totalMarks == null) ? "--" : String.format("%.1f", e.totalMarks);
            String attdStr = String.format("%.1f%%", e.attendancePct);

            System.out.printf("%-10s %-24s %-9s %-4.1f %-7s %-7.1f %-7.1f %-7s %-7s %-7s %-6s %-4d %-10s%n",
                e.courseCode, title, e.courseType, e.credits, attdStr,
                e.ciaRaw, e.ciaWeighted, eseRawStr, eseWtdStr, totStr, e.grade, e.gradePoint, e.resultStatus);

            totalCreditsEnrolled += e.credits;
            if ("PASS".equals(e.resultStatus)) {
                totalCreditsEarned += e.credits;
                totalCreditPoints += (e.credits * e.gradePoint);
            } else if ("PENDING".equals(e.resultStatus)) {
                hasPending = true;
            } else {
                hasFail = true;
            }
        }

        double sgpa = (totalCreditsEnrolled == 0) ? 0.0 : (totalCreditPoints / totalCreditsEnrolled);
        double equivalentPct = sgpa * 10.0;
        String eqPctStr = String.format("%.2f%%", equivalentPct);

        System.out.println("-".repeat(105));
        System.out.printf("  Total Enrolled Credits : %-8.1f Total Earned Credits : %-8.1f%n", totalCreditsEnrolled, totalCreditsEarned);
        System.out.printf("  SGPA Earned            : %-8.2f Equivalent Percentage: %-10s%n", sgpa, eqPctStr);
        
        String standing;
        if (hasPending) {
            standing = "PROVISIONAL (Some course evaluations are PENDING)";
        } else if (hasFail) {
            standing = "RE-APPEAR / ARREARS DETECTED (Review RA / SA / AB grades)";
        } else if (sgpa >= 8.5) {
            standing = "FIRST CLASS WITH DISTINCTION";
        } else if (sgpa >= 7.0) {
            standing = "FIRST CLASS";
        } else {
            standing = "SECOND CLASS";
        }
        System.out.println("  Academic Standing      : " + standing);
        System.out.println("=".repeat(105));
        System.out.println(" * Note: KEC R2024 Evaluation Rules Applied:");
        System.out.println("   - Theory    : 40% CIA + 60% ESE (ESE Min: 27/60, Pass Min: 50)");
        System.out.println("   - Practical : 60% CIA + 40% ESE (ESE Min: 20/40, Pass Min: 50)");
        System.out.println("   - Embedded  : 50% CIA + 50% ESE (ESE Min: 22.5/50, Pass Min: 50)");
        System.out.println("   - Grades    : O(91-100:10), A+(81-90:9), A(71-80:8), B+(61-70:7), B(50-60:6), RA/SA/AB(0)");
        System.out.println("=".repeat(105));
    }

    // ========================================================================
    // REPORT 2: Attendance Defaulter Report (< 65%: SA)
    // ========================================================================
    public void printAttendanceDefaulters() throws SQLException {
        String sql = "SELECT e.enrollment_id, e.roll_no, s.student_name, s.department, " +
                     "e.course_code, c.title, c.credits, c.course_type, e.semester, " +
                     "e.total_classes, e.classes_attended, e.medical_condonation_approved, " +
                     "m.cia1, m.cia2, m.assignment_mark, m.ese_mark, m.ese_absent " +
                     "FROM enrollments e " +
                     "JOIN students s ON s.roll_no = e.roll_no " +
                     "JOIN courses c ON c.course_code = e.course_code " +
                     "LEFT JOIN assessment_marks m ON m.enrollment_id = e.enrollment_id " +
                     "WHERE e.total_classes > 0 AND (e.classes_attended * 100.0 / e.total_classes) < 75.0 " +
                     "ORDER BY s.department, s.roll_no, e.course_code";

        List<EnrollmentInfo> defaulters = new ArrayList<>();
        List<EnrollmentInfo> condonations = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                EnrollmentInfo info = evaluateRecord(rs);
                if (info.attendancePct < 65.0) {
                    defaulters.add(info);
                } else if (info.attendancePct < 75.0) {
                    condonations.add(info);
                }
            }
        }

        System.out.println("\n" + "=".repeat(105));
        System.out.println("                       KEC R2024 ATTENDANCE DEFAULTERS & CONDONATION REPORT");
        System.out.println("=".repeat(105));

        System.out.println("\n[SECTION A: CRITICAL DEFAULTERS (< 65% Attendance) - BARRED FROM ESE -> SA GRADE AWARDED]");
        System.out.println("-".repeat(105));
        System.out.printf("%-12s %-22s %-8s %-10s %-8s %-9s %-8s %-12s%n",
            "Roll No", "Student Name", "Dept", "Course", "Held", "Attended", "Attd %", "Final Grade");
        System.out.println("-".repeat(105));
        if (defaulters.isEmpty()) {
            System.out.println("  No students found with critical attendance shortage (< 65%).");
        } else {
            for (EnrollmentInfo e : defaulters) {
                String attdStr = String.format("%.2f%%", e.attendancePct);
                System.out.printf("%-12s %-22s %-8s %-10s %-8d %-9d %-9s %-12s%n",
                    e.rollNo, e.studentName, e.department, e.courseCode,
                    e.totalClasses, e.classesAttended, attdStr, "SA (Barred)");
            }
        }

        System.out.println("\n[SECTION B: CONDITIONAL ELIGIBILITY (65% to 74.9% Attendance) - REQUIRES MEDICAL CONDONATION]");
        System.out.println("-".repeat(105));
        System.out.printf("%-12s %-22s %-8s %-10s %-8s %-9s %-9s %-16s %-12s%n",
            "Roll No", "Student Name", "Dept", "Course", "Held", "Attended", "Attd %", "Condonation", "ESE Status");
        System.out.println("-".repeat(105));
        if (condonations.isEmpty()) {
            System.out.println("  No students in the 65% - 74.9% attendance bracket.");
        } else {
            for (EnrollmentInfo e : condonations) {
                String condStatus = e.condonationApproved ? "APPROVED" : "PENDING/REJECTED";
                String eseStatus = e.condonationApproved ? "ELIGIBLE" : "BARRED (SA)";
                String attdStr = String.format("%.2f%%", e.attendancePct);
                System.out.printf("%-12s %-22s %-8s %-10s %-8d %-9d %-9s %-16s %-12s%n",
                    e.rollNo, e.studentName, e.department, e.courseCode,
                    e.totalClasses, e.classesAttended, attdStr, condStatus, eseStatus);
            }
        }
        System.out.println("=".repeat(105));
    }

    // ========================================================================
    // REPORT 3: Class Performance & Result Summary (Dept-wise Pass/Fail & Class Avg SGPA)
    // ========================================================================
    public void printClassPerformanceSummary(int semester) throws SQLException {
        String sql = "SELECT e.enrollment_id, e.roll_no, s.student_name, s.department, " +
                     "e.course_code, c.title, c.credits, c.course_type, e.semester, " +
                     "e.total_classes, e.classes_attended, e.medical_condonation_approved, " +
                     "m.cia1, m.cia2, m.assignment_mark, m.ese_mark, m.ese_absent " +
                     "FROM enrollments e " +
                     "JOIN students s ON s.roll_no = e.roll_no " +
                     "JOIN courses c ON c.course_code = e.course_code " +
                     "LEFT JOIN assessment_marks m ON m.enrollment_id = e.enrollment_id " +
                     "WHERE e.semester = ? " +
                     "ORDER BY s.department, e.roll_no";

        Map<String, Map<String, List<EnrollmentInfo>>> deptStudentMap = new LinkedHashMap<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EnrollmentInfo info = evaluateRecord(rs);
                    deptStudentMap
                        .computeIfAbsent(info.department, k -> new LinkedHashMap<>())
                        .computeIfAbsent(info.rollNo, k -> new ArrayList<>())
                        .add(info);
                }
            }
        }

        System.out.println("\n" + "=".repeat(105));
        System.out.println("                 KEC R2024 CLASS PERFORMANCE & DEPARTMENT RESULT SUMMARY");
        System.out.println("                                      SEMESTER " + semester);
        System.out.println("=".repeat(105));
        System.out.printf("%-10s %-10s %-10s %-8s %-8s %-8s %-8s %-12s %-12s%n",
            "Dept", "Students", "Courses", "Passed", "RA Fail", "SA Bar", "AB", "Pass %", "Avg SGPA");
        System.out.println("-".repeat(105));

        if (deptStudentMap.isEmpty()) {
            System.out.println("  No enrollment records found for Semester " + semester);
            System.out.println("=".repeat(105));
            return;
        }

        int grandStudents = 0;
        int grandPassed = 0;
        int grandRA = 0;
        int grandSA = 0;
        int grandAB = 0;
        double grandSgpaSum = 0;

        for (Map.Entry<String, Map<String, List<EnrollmentInfo>>> deptEntry : deptStudentMap.entrySet()) {
            String dept = deptEntry.getKey();
            Map<String, List<EnrollmentInfo>> students = deptEntry.getValue();

            int deptTotalStudents = students.size();
            int deptTotalEnrollments = 0;
            int deptPassedStudents = 0;
            int deptRaStudents = 0;
            int deptSaStudents = 0;
            int deptAbStudents = 0;
            double deptSgpaSum = 0.0;

            for (Map.Entry<String, List<EnrollmentInfo>> studentEntry : students.entrySet()) {
                List<EnrollmentInfo> courses = studentEntry.getValue();
                deptTotalEnrollments += courses.size();

                boolean hasRA = false;
                boolean hasSA = false;
                boolean hasAB = false;
                double studentCredits = 0;
                double studentPoints = 0;

                for (EnrollmentInfo c : courses) {
                    studentCredits += c.credits;
                    if ("SA".equals(c.resultStatus)) {
                        hasSA = true;
                    } else if ("AB".equals(c.resultStatus)) {
                        hasAB = true;
                    } else if (c.resultStatus.startsWith("FAIL") || "RA".equals(c.grade)) {
                        hasRA = true;
                    } else if ("PASS".equals(c.resultStatus)) {
                        studentPoints += (c.credits * c.gradePoint);
                    }
                }

                double studentSgpa = (studentCredits == 0) ? 0.0 : (studentPoints / studentCredits);
                deptSgpaSum += studentSgpa;

                if (!hasRA && !hasSA && !hasAB) {
                    deptPassedStudents++;
                } else {
                    if (hasSA) deptSaStudents++;
                    if (hasAB) deptAbStudents++;
                    if (hasRA) deptRaStudents++;
                }
            }

            double deptPassPct = (deptTotalStudents == 0) ? 0.0 : (deptPassedStudents * 100.0 / deptTotalStudents);
            double deptAvgSgpa = (deptTotalStudents == 0) ? 0.0 : (deptSgpaSum / deptTotalStudents);

            grandStudents += deptTotalStudents;
            grandPassed += deptPassedStudents;
            grandRA += deptRaStudents;
            grandSA += deptSaStudents;
            grandAB += deptAbStudents;
            grandSgpaSum += deptSgpaSum;

            String passStr = String.format("%.2f%%", deptPassPct);
            System.out.printf("%-10s %-10d %-10d %-8d %-8d %-8d %-8d %-12s %-12.2f%n",
                dept, deptTotalStudents, deptTotalEnrollments, deptPassedStudents,
                deptRaStudents, deptSaStudents, deptAbStudents, passStr, deptAvgSgpa);
        }

        double collegePassPct = (grandStudents == 0) ? 0.0 : (grandPassed * 100.0 / grandStudents);
        double collegeAvgSgpa = (grandStudents == 0) ? 0.0 : (grandSgpaSum / grandStudents);

        System.out.println("-".repeat(105));
        String totalPassStr = String.format("%.2f%%", collegePassPct);
        System.out.printf("%-10s %-10d %-10s %-8d %-8d %-8d %-8d %-12s %-12.2f%n",
            "OVERALL", grandStudents, "--", grandPassed, grandRA, grandSA, grandAB, totalPassStr, collegeAvgSgpa);
        System.out.println("=".repeat(105));
    }

    // ========================================================================
    // Sample Data Seeder for Demonstrating All Scenarios
    // ========================================================================
    public void seedDemoData() throws SQLException {
        StudentDAO sdao = new StudentDAO();
        System.out.println(">> Seeding KEC R2024 Demo Students and Courses...");

        // 1. Students
        String[][] studentsData = {
            {"24ITR001", "Arun Kumar M", "IT", "3"},
            {"24ITR002", "Bhavani S", "IT", "3"},
            {"24ITR003", "Chandra Prakash R", "IT", "3"},
            {"24CSR010", "Deepak K", "CSE", "3"},
            {"24ECR025", "Elango V", "ECE", "3"}
        };
        for (String[] s : studentsData) {
            if (!sdao.studentExists(s[0])) {
                sdao.addStudent(s[0], s[1], s[2], Integer.parseInt(s[3]));
            }
        }

        // 2. Courses (Theory, Practical, Embedded)
        Object[][] coursesData = {
            {"24ITT31", "Data Structures", 3.0, "THEORY"},
            {"24ITP32", "Data Structures Laboratory", 1.5, "PRACTICAL"},
            {"24ITE33", "Microcontrollers & IoT", 4.0, "EMBEDDED"},
            {"24CST31", "Operating Systems", 3.0, "THEORY"}
        };
        for (Object[] c : coursesData) {
            if (!sdao.courseExists((String) c[0])) {
                sdao.addCourse((String) c[0], (String) c[1], (Double) c[2], (String) c[3]);
            }
        }

        // 3. Enrollments for Semester 3
        String[][] enrollmentsData = {
            {"24ITR001", "24ITT31", "3"},
            {"24ITR001", "24ITP32", "3"},
            {"24ITR001", "24ITE33", "3"},
            {"24ITR002", "24ITT31", "3"},
            {"24ITR002", "24ITP32", "3"},
            {"24ITR003", "24ITT31", "3"}, // Will test SA (<65%)
            {"24CSR010", "24CST31", "3"}
        };
        for (String[] e : enrollmentsData) {
            try {
                sdao.enroll(e[0], e[1], Integer.parseInt(e[2]));
            } catch (Exception ignored) {
                // Already enrolled
            }
        }

        // 4. Attendance
        // Arun: 40/45 (88.89% - Eligible)
        updateAttendance("24ITR001", "24ITT31", 3, 45, 40, false);
        updateAttendance("24ITR001", "24ITP32", 3, 30, 28, false);
        updateAttendance("24ITR001", "24ITE33", 3, 45, 42, false);

        // Bhavani: 68% attendance (Requires Condonation -> Approved)
        updateAttendance("24ITR002", "24ITT31", 3, 45, 31, true);
        updateAttendance("24ITR002", "24ITP32", 3, 30, 27, false);

        // Chandra: 55% attendance (< 65% -> Shortage SA)
        updateAttendance("24ITR003", "24ITT31", 3, 45, 25, false);

        // Deepak: 85% attendance
        updateAttendance("24CSR010", "24CST31", 3, 45, 39, false);

        // 5. Phased Marks
        // Arun: Phase 1 & 2
        recordPhase1CIA("24ITR001", "24ITT31", 3, 85.0, 88.0);
        recordPhase2Assignment("24ITR001", "24ITT31", 3, 92.0);
        // Phase 3 ESE: 86.0 -> Theory: CIA(88.33*0.4=35.33), ESE(86*0.6=51.6) -> Total 86.93 -> A+
        recordPhase3ESE("24ITR001", "24ITT31", 3, 86.0, false);

        // Practical
        recordPhase1CIA("24ITR001", "24ITP32", 3, 95.0, 92.0);
        recordPhase2Assignment("24ITR001", "24ITP32", 3, 96.0);
        recordPhase3ESE("24ITR001", "24ITP32", 3, 94.0, false);

        // Embedded
        recordPhase1CIA("24ITR001", "24ITE33", 3, 90.0, 91.0);
        recordPhase2Assignment("24ITR001", "24ITE33", 3, 95.0);
        recordPhase3ESE("24ITR001", "24ITE33", 3, 88.0, false);

        // Bhavani: Theory ESE: 72.0
        recordPhase1CIA("24ITR002", "24ITT31", 3, 70.0, 75.0);
        recordPhase2Assignment("24ITR002", "24ITT31", 3, 78.0);
        recordPhase3ESE("24ITR002", "24ITT31", 3, 72.0, false);

        // Deepak: Theory ESE: Absent
        recordPhase1CIA("24CSR010", "24CST31", 3, 65.0, 70.0);
        recordPhase2Assignment("24CSR010", "24CST31", 3, 75.0);
        recordPhase3ESE("24CSR010", "24CST31", 3, null, true);

        System.out.println(">> Demo dataset successfully seeded!");
    }

    private Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
        double d = rs.getDouble(col);
        return rs.wasNull() ? null : d;
    }
}
