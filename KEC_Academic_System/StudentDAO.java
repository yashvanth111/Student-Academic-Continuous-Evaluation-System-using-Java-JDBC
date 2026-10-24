import java.sql.*;

/**
 * StudentDAO - Manages Student Registration, Course Catalog, and Course Enrollment.
 */
public class StudentDAO {

    public boolean studentExists(String rollNo) throws SQLException {
        String sql = "SELECT 1 FROM students WHERE roll_no = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean courseExists(String courseCode) throws SQLException {
        String sql = "SELECT 1 FROM courses WHERE course_code = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void addStudent(String rollNo, String name, String dept, int semester) throws SQLException {
        if (rollNo == null || rollNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Roll Number cannot be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Student Name cannot be empty.");
        }
        if (dept == null || dept.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be empty.");
        }
        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException("Current Semester must be between 1 and 8.");
        }

        String sql = "INSERT INTO students (roll_no, student_name, department, current_semester) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setString(2, name.trim());
            ps.setString(3, dept.trim().toUpperCase());
            ps.setInt(4, semester);
            ps.executeUpdate();
            System.out.println(">> Student [" + rollNo.trim().toUpperCase() + " - " + name.trim() + "] registered successfully.");
        }
    }

    public void addCourse(String courseCode, String title, double credits, String type) throws SQLException {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course Code cannot be empty.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Course Title cannot be empty.");
        }
        if (credits <= 0) {
            throw new IllegalArgumentException("Credits must be greater than 0.");
        }
        String upperType = type.trim().toUpperCase();
        if (!upperType.equals("THEORY") && !upperType.equals("PRACTICAL") && !upperType.equals("EMBEDDED")) {
            throw new IllegalArgumentException("Course Type must be THEORY, PRACTICAL, or EMBEDDED.");
        }

        String sql = "INSERT INTO courses (course_code, title, credits, course_type) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode.trim().toUpperCase());
            ps.setString(2, title.trim());
            ps.setDouble(3, credits);
            ps.setString(4, upperType);
            ps.executeUpdate();
            System.out.println(">> Course [" + courseCode.trim().toUpperCase() + " - " + title.trim() + " (" + upperType + ", " + credits + " Cr)] added successfully.");
        }
    }

    public void enroll(String rollNo, String courseCode, int semester) throws SQLException {
        String roll = rollNo.trim().toUpperCase();
        String code = courseCode.trim().toUpperCase();

        if (!studentExists(roll)) {
            throw new SQLException("Student with Roll Number [" + roll + "] does not exist. Please register student first.");
        }
        if (!courseExists(code)) {
            throw new SQLException("Course with Code [" + code + "] does not exist. Please add course first.");
        }
        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }

        String enrollSql = "INSERT INTO enrollments (roll_no, course_code, semester) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(enrollSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roll);
            ps.setString(2, code);
            ps.setInt(3, semester);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long enrollmentId = keys.getLong(1);
                    String initMarksSql = "INSERT INTO assessment_marks (enrollment_id) VALUES (?)";
                    try (PreparedStatement marksPs = con.prepareStatement(initMarksSql)) {
                        marksPs.setLong(1, enrollmentId);
                        marksPs.executeUpdate();
                    }
                }
            }
            System.out.println(">> Student [" + roll + "] successfully enrolled in Course [" + code + "] for Semester " + semester + ".");
        }
    }

    public void showStudents() throws SQLException {
        String sql = "SELECT * FROM students ORDER BY department, roll_no";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n================================================================================");
            System.out.println("                           REGISTERED STUDENTS CATALOG                           ");
            System.out.println("================================================================================");
            System.out.printf("%-15s %-32s %-15s %-8s%n", "ROLL NO", "STUDENT NAME", "DEPARTMENT", "SEMESTER");
            System.out.println("--------------------------------------------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-15s %-32s %-15s %-8d%n",
                    rs.getString("roll_no"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    rs.getInt("current_semester"));
            }
            if (!found) {
                System.out.println("No registered students found.");
            }
            System.out.println("================================================================================");
        }
    }

    public void showCourses() throws SQLException {
        String sql = "SELECT * FROM courses ORDER BY course_code";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n================================================================================");
            System.out.println("                            OFFERED COURSES CATALOG                             ");
            System.out.println("================================================================================");
            System.out.printf("%-15s %-35s %-10s %-12s%n", "COURSE CODE", "COURSE TITLE", "CREDITS", "COURSE TYPE");
            System.out.println("--------------------------------------------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-15s %-35s %-10.1f %-12s%n",
                    rs.getString("course_code"),
                    rs.getString("title"),
                    rs.getDouble("credits"),
                    rs.getString("course_type"));
            }
            if (!found) {
                System.out.println("No courses cataloged.");
            }
            System.out.println("================================================================================");
        }
    }
}
