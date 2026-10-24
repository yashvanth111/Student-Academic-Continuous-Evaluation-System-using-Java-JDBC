# Kongu Engineering College (Autonomous) - Regulation 2024
## Student Academic & Continuous Evaluation System using Java JDBC

A database-driven Java application utilizing JDBC to manage student records, perform incremental marks entry (Continuous Internal Assessments and End Semester Examinations), enforce attendance eligibility and medical condonation rules, and generate formal academic performance reports according to **Kongu Engineering College (Autonomous) - Regulation 2024**.

### System Overview & Working Methodology
The system initiates by establishing a secure JDBC connection between the Java application layer and the MySQL relational database (`kec_academic`) using the MySQL Connector/J driver. Through Data Access Objects (`StudentDAO` and `AcademicDAO`), parameterized SQL queries securely handle student registration, course catalogs, enrollment records, attendance figures, and phased assessment marks. 

The evaluation algorithm first enforces attendance eligibility per registered course: students with attendance $\ge 75\%$ qualify directly for examinations, those between $65\%$ and $74.9\%$ require verified Medical Condonation approval, and those below $65\%$ are awarded Shortage of Attendance (Grade SA with 0 Grade Points) and strictly barred from the End Semester Examination (ESE). 

Next, continuous internal assessment (CIA) and ESE marks are converted into normalized weightages according to course type—**Theory** courses allocate 40% for CIA (CIA-1, CIA-2, and Assignments) and 60% for ESE (with a mandatory 27/60 threshold); **Practical** courses allocate 60% for continuous lab evaluation and 40% for practical ESE (with a mandatory 20/40 threshold); and **Embedded** courses allocate 50% for CIA and 50% for ESE (with a mandatory 22.5/50 threshold). 

If the student satisfies both the course-specific ESE minimum and the aggregate pass threshold of 50 marks, the total score is mapped to the KEC R2024 10-point scale Letter Grade (O, A+, A, B+, B); otherwise, a Re-Appearance grade (RA) is awarded. Absent students are designated with Grade AB. Finally, the Semester Grade Point Average (SGPA) is computed by dividing the sum of credit-weighted grade points by the total enrolled credits ($\text{Equivalent Percentage} = \text{SGPA} \times 10$). Complete institutional mark sheets, attendance defaulter logs, and department-wise class performance summaries are rendered dynamically on both the Command Prompt Console and the Web Dashboard.

---

### Project Architecture & Folder Structure

```
KEC_Academic_System/
│
├── DBConnection.java          # JDBC connection manager (MySQL Connector/J)
├── StudentDAO.java            # Student registration, course catalog & enrollments
├── AcademicDAO.java           # Incremental marks entry, attendance filter & evaluation engine
├── MainApp.java               # Interactive menu-driven console user interface
├── SystemVerificationTest.java # Automated end-to-end verification suite
├── kec_schema.sql             # SQL DDL script creating all tables and constraints
├── mysql-connector-j-26.7.0.jar # MySQL JDBC Driver JAR
├── run.bat                    # 1-click Windows compilation & launch batch script
├── README.md                  # Project manual & documentation
└── EXECUTION_DOCUMENTATION.md # Comprehensive terminal execution proof & logs
```

---

### 1. System Requirements & Specifications

#### 1.1 Student & Course Management
- **Student Registration**: Unique Roll Number (e.g., `24ITR001`), Student Name, Department (`IT`, `CSE`, `ECE`, `MECH`), and Current Semester (1–8).
- **Course Catalog**: Unique Course Code (e.g., `24ITT31`, `24ITP32`), Title, Credits, and Course Type:
  - `THEORY`
  - `PRACTICAL`
  - `EMBEDDED`
- **Course Enrollment & Attendance Tracking**: Enrolls students into courses per semester and continuously records total classes held vs. classes attended.

#### 1.2 Incremental Marks Entry ("Then and Now")
- **Phase 1 (Mid-Semester)**: CIA Test 1 (0–100) and CIA Test 2 (0–100).
- **Phase 2 (End of Semester - CIA)**: Assignments / Quizzes / Lab Evaluations (0–100).
- **Phase 3 (Post Semester)**: End Semester Examination (ESE) Marks (0–100) or Mark Absence (`AB`).

#### 1.3 Evaluation Rules & Weightage Engine (KEC R2024)
| Course Type | CIA Weightage | ESE Weightage | ESE Minimum Threshold | Overall Pass Minimum |
| :--- | :---: | :---: | :---: | :---: |
| **THEORY** | 40% | 60% | 45% (27.0 / 60) | 50.0 Marks |
| **PRACTICAL** | 60% | 40% | 50% (20.0 / 40) | 50.0 Marks |
| **EMBEDDED** | 50% | 50% | 45% (22.5 / 50) | 50.0 Marks |

- **CIA Raw Calculation**: Average of recorded CIA assessments (`(CIA1 + CIA2 + Assignment) / 3`).
- **CIA Weighted**: `CIA Raw × (CIA Weight / 100)`.
- **ESE Weighted**: `ESE Raw × (ESE Weight / 100)`.
- **Total Marks**: `CIA Weighted + ESE Weighted`.

#### 1.4 Attendance & Grading Rules
- **Attendance Filtering**:
  - $\ge 75\%$: **Eligible** to appear for ESE.
  - $65\% \text{ to } 74.9\%$: **Conditionally Eligible** (requires approved Medical Condonation).
  - $< 65\%$: Award **SA** (Shortage of Attendance) grade (0 Grade Points). **Barred from ESE**.
- **10-Point Grading System**:
  - `O` (91 – 100) $\rightarrow$ Grade Point: **10**
  - `A+` (81 – 90) $\rightarrow$ Grade Point: **9**
  - `A` (71 – 80) $\rightarrow$ Grade Point: **8**
  - `B+` (61 – 70) $\rightarrow$ Grade Point: **7**
  - `B` (50 – 60) $\rightarrow$ Grade Point: **6**
  - `RA` (Re-Appearance / Fail if $< 50$ aggregate or failed ESE minimum) $\rightarrow$ Grade Point: **0**
  - `SA` (Shortage of Attendance) $\rightarrow$ Grade Point: **0**
  - `AB` (Absent in ESE) $\rightarrow$ Grade Point: **0**

#### 1.5 SGPA & Percentage Formulas
$$\text{SGPA} = \frac{\sum (\text{Course Credits} \times \text{Grade Points Earned})}{\sum (\text{Total Enrolled Credits})}$$
$$\text{Equivalent Percentage} = \text{SGPA} \times 10$$

---

### 2. Setup & Execution Guide

#### Prerequisites
- **Java Development Kit (JDK 17+)** installed (`java -version`, `javac -version`).
- **MySQL / XAMPP Server** running on `localhost:3306`.
- **MySQL Connector/J JAR** (`mysql-connector-j-26.7.0.jar` included).

#### Database Initialization
Run the database schema script in MySQL Workbench, phpMyAdmin, or MySQL CLI:
```bash
mysql -u root -p < kec_schema.sql
```
*(By default on XAMPP, root password is empty. DBConnection defaults to `root` with no password).*

#### Quick 1-Click Launch (Windows)
Double-click `run.bat` or run:
```cmd
run.bat
```

#### Manual Compilation & Run
```cmd
javac -cp ".;mysql-connector-j-26.7.0.jar" DBConnection.java StudentDAO.java AcademicDAO.java MainApp.java
java -cp ".;mysql-connector-j-26.7.0.jar" MainApp
```

#### Running Automated Verification Suite
To test all edge cases and produce execution proofs across all 3 course types:
```cmd
javac -cp ".;mysql-connector-j-26.7.0.jar" SystemVerificationTest.java
java -cp ".;mysql-connector-j-26.7.0.jar" SystemVerificationTest
```

---

### 3. Menu Options Walkthrough
1. **Student Registration**: Add Roll Number, Name, Department, Current Semester.
2. **Course Catalog Management**: Add Course Code, Title, Credits, and Type (`THEORY`, `PRACTICAL`, `EMBEDDED`).
3. **Course Enrollment**: Enroll students into courses for the semester (automatically initializes assessment records).
4. **Attendance Tracking & Medical Condonation**: Continuously update total classes held vs. attended, and record medical condonation status.
5. **Incremental Marks Entry ("Then and Now")**:
   - `Phase 1`: CIA1 & CIA2
   - `Phase 2`: Assignments / Quizzes / Lab
   - `Phase 3`: ESE Marks or Mark AB (with automatic attendance eligibility enforcement!)
   - `Component Update`: Update individual assessments
   - `View Marks`: View current live scores and calculated raw CIA average
6. **Student Semester Mark Sheet**: Generates complete official transcript with SGPA and equivalent percentage.
7. **Attendance Defaulter Report**: Lists critical defaulters ($<65\%$ SA) and conditional condonation cases.
8. **Class Performance & Result Summary**: Department-wise pass/fail counts and class average SGPA.
9. **View Catalogs**: Inspect registered students and courses.
10. **Populate Sample Demo Dataset**: Seeds comprehensive realistic data for instant demonstration.
