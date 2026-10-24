# Kongu Engineering College (Autonomous) - Regulation 2024
## Student Academic & Continuous Evaluation System using Java JDBC
### Formal Execution Documentation & Verification Proofs

This document provides complete, verbatim execution proofs and transcripts verifying all system requirements and specifications defined under KEC R2024.

---

### Proof 1: Adding a Student and Course

#### 1.1 Student Registration Output
```
--- [1] STUDENT REGISTRATION ---
Roll Number (e.g., 24ITR001): 24ITR001
Student Full Name: Arun Kumar M
Department (e.g., IT, CSE, ECE, MECH): IT
Current Semester (1 - 8): 3
>> Student [24ITR001 - Arun Kumar M] registered successfully.
```

#### 1.2 Course Catalog Creation (Theory, Practical, Embedded)
```
--- [2] COURSE CATALOG MANAGEMENT ---
Course Code (e.g., 24ITR31 / 24ITT31): 24ITT31
Course Title: Data Structures
Credits (e.g., 3.0, 4.0, 1.5): 3.0
Select Course Type:
  1. THEORY     (40% CIA, 60% ESE, Min ESE Threshold: 27/60)
  2. PRACTICAL  (60% CIA, 40% ESE, Min ESE Threshold: 20/40)
  3. EMBEDDED   (50% CIA, 50% ESE, Min ESE Threshold: 22.5/50)
Choice (1-3): 1
>> Course [24ITT31 - Data Structures (THEORY, 3.0 Cr)] added successfully.
```

#### 1.3 Course Enrollment & Automatic Assessment Record Creation
```
--- [3] COURSE ENROLLMENT ---
Roll Number: 24ITR001
Course Code: 24ITT31
Semester: 3
>> Student [24ITR001] successfully enrolled in Course [24ITT31] for Semester 3.
```

---

### Proof 2: Incremental Marks Entry ("Then and Now")

The system supports phased/incremental evaluation as assessments occur across the semester:

#### 2.1 Phase 1 (Mid-Semester Evaluation)
Recording CIA Test 1 (0–100) and CIA Test 2 (0–100):
```
>> [Phase 1 Entry: Mid-Semester CIA Tests]
Roll Number: 24ITR001
Course Code: 24ITT31
Semester: 3
CIA Test 1 Mark (0-100, or press Enter to skip): 85.0
CIA Test 2 Mark (0-100, or press Enter to skip): 88.0
>> [Phase 1 Recorded] Roll: 24ITR001 | Course: 24ITT31 | CIA1: 85.00 | CIA2: 88.00
```

#### 2.2 Phase 2 (End of Semester - CIA Evaluation)
Recording Assignments / Quizzes / Lab Evaluations (0–100):
```
>> [Phase 2 Entry: End-Semester CIA Assignment/Quiz/Lab Evaluation]
Roll Number: 24ITR001
Course Code: 24ITT31
Semester: 3
Assignments / Quizzes / Lab Mark (0-100): 92.0
>> [Phase 2 Recorded] Roll: 24ITR001 | Course: 24ITT31 | Assignment/Quiz/Lab: 92.00
```

#### 2.3 Phase 3 (Post Semester - ESE Evaluation)
Recording End Semester Examination (ESE) Marks (0–100):
```
>> [Phase 3 Entry: Post-Semester End Semester Examination (ESE)]
 * Note: System will check attendance eligibility rules (<65% SA barred, 65-74.9% condonation).
Roll Number: 24ITR001
Course Code: 24ITT31
Semester: 3
Was student Absent in ESE? (y/n): n
ESE Marks Obtained (0-100): 86.0
>> [Phase 3 Recorded] Roll: 24ITR001 | Course: 24ITT31 | ESE: 86.00 | Absent: NO
```

#### 2.4 Attendance Gate Enforcement Proof (Student with < 65% Barred from ESE)
When attempting to enter ESE marks for student `24ITR003` whose attendance is 53.33%:
```
Attempting to record ESE for student with attendance < 65%:
>> SUCCESSFUL ATTENDANCE GATE ENFORCEMENT: ELIGIBILITY VIOLATION: Student has 53.33% attendance (< 65%). BARRED from appearing in ESE (SA Grade assigned).
```

---

### Proof 3: Generated Student Semester Mark Sheet with Calculated SGPA

#### Student 1: Arun Kumar M (`24ITR001`) - Full Course Set (Theory, Practical, Embedded)

```
=========================================================================================================
                   KONGU ENGINEERING COLLEGE (AUTONOMOUS), PERUNDURAI, ERODE
                   OFFICE OF THE CONTROLLER OF EXAMINATIONS - REGULATION 2024
                               STUDENT SEMESTER GRADE REPORT
=========================================================================================================
  Roll Number   : 24ITR001                  Department       : IT                       
  Student Name  : Arun Kumar M              Current Semester : 3                        
---------------------------------------------------------------------------------------------------------
Course     Course Title             Type      Cr   Attd%   CIA-Raw CIA-Wtd ESE-Raw ESE-Wtd Total  Grd  Status    
---------------------------------------------------------------------------------------------------------
24ITE33    Microcontrollers & IoT   EMBEDDED  4.0  93.3%   92.0    46.0    88.0    44.0    90.0    A+     9    PASS      
24ITP32    Data Structures Labo...  PRACTICAL 1.5  93.3%   95.0    57.0    92.0    36.8    93.8    O      10   PASS      
24ITT31    Data Structures          THEORY    3.0  88.9%   88.3    35.3    86.0    51.6    86.9    A+     9    PASS      
---------------------------------------------------------------------------------------------------------
  Total Enrolled Credits : 8.5      Total Earned Credits : 8.5     
  SGPA Earned            : 9.18     Equivalent Percentage: 91.76%    
  Academic Standing      : FIRST CLASS WITH DISTINCTION
=========================================================================================================
 * Note: KEC R2024 Evaluation Rules Applied:
   - Theory    : 40% CIA + 60% ESE (ESE Min: 27/60, Pass Min: 50)
   - Practical : 60% CIA + 40% ESE (ESE Min: 20/40, Pass Min: 50)
   - Embedded  : 50% CIA + 50% ESE (ESE Min: 22.5/50, Pass Min: 50)
   - Grades    : O(91-100:10), A+(81-90:9), A(71-80:8), B+(61-70:7), B(50-60:6), RA/SA/AB(0)
=========================================================================================================
```

**Step-by-step Mathematical Verification**:
1. **24ITT31 (Theory, 3.0 Cr)**:
   - CIA Raw = $(85 + 88 + 92) / 3 = 88.33$
   - CIA Weighted (40%) = $88.33 \times 0.40 = 35.33$
   - ESE Raw = $86.0$
   - ESE Weighted (60%) = $86.0 \times 0.60 = 51.60$ (Threshold $\ge 27.0$: Met)
   - Total = $35.33 + 51.60 = 86.93 \rightarrow 87 \rightarrow$ **Grade: A+ (9 GP)**
   - Points = $3.0 \times 9 = 27.0$

2. **24ITP32 (Practical, 1.5 Cr)**:
   - CIA Raw = $(94 + 96 + 95) / 3 = 95.00$
   - CIA Weighted (60%) = $95.00 \times 0.60 = 57.00$
   - ESE Raw = $92.0$
   - ESE Weighted (40%) = $92.0 \times 0.40 = 36.80$ (Threshold $\ge 20.0$: Met)
   - Total = $57.00 + 36.80 = 93.80 \rightarrow 94 \rightarrow$ **Grade: O (10 GP)**
   - Points = $1.5 \times 10 = 15.0$

3. **24ITE33 (Embedded, 4.0 Cr)**:
   - CIA Raw = $(90 + 92 + 94) / 3 = 92.00$
   - CIA Weighted (50%) = $92.00 \times 0.50 = 46.00$
   - ESE Raw = $88.0$
   - ESE Weighted (50%) = $88.0 \times 0.50 = 44.00$ (Threshold $\ge 22.5$: Met)
   - Total = $46.00 + 44.00 = 90.00 \rightarrow$ **Grade: A+ (9 GP)**
   - Points = $4.0 \times 9 = 36.0$

- Total Registered Credits = $3.0 + 1.5 + 4.0 = 8.5$
- Total Grade Points = $27.0 + 15.0 + 36.0 = 78.0$
- **SGPA** = $78.0 / 8.5 = \mathbf{9.18}$
- **Equivalent Percentage** = $9.18 \times 10 = \mathbf{91.76\%}$

---

### Proof 4: Attendance Defaulter Report & Medical Condonation

```
=========================================================================================================
                       KEC R2024 ATTENDANCE DEFAULTERS & CONDONATION REPORT
=========================================================================================================

[SECTION A: CRITICAL DEFAULTERS (< 65% Attendance) - BARRED FROM ESE -> SA GRADE AWARDED]
---------------------------------------------------------------------------------------------------------
Roll No      Student Name           Dept     Course     Held     Attended  Attd %   Final Grade 
---------------------------------------------------------------------------------------------------------
24ITR003     Chandra Prakash R      IT       24ITT31    45       24        53.33%    SA (Barred) 

[SECTION B: CONDITIONAL ELIGIBILITY (65% to 74.9% Attendance) - REQUIRES MEDICAL CONDONATION]
---------------------------------------------------------------------------------------------------------
Roll No      Student Name           Dept     Course     Held     Attended  Attd %    Condonation      ESE Status  
---------------------------------------------------------------------------------------------------------
24ITR002     Bhavani S              IT       24ITT31    45       31        68.89%    APPROVED         ELIGIBLE    
=========================================================================================================
```

---

### Proof 5: Class Performance & Department Result Summary

```
=========================================================================================================
                 KEC R2024 CLASS PERFORMANCE & DEPARTMENT RESULT SUMMARY
                                      SEMESTER 3
=========================================================================================================
Dept       Students   Courses    Passed   RA Fail  SA Bar   AB       Pass %       Avg SGPA    
---------------------------------------------------------------------------------------------------------
CSE        1          1          0        0        0        1        0.00%        0.00        
IT         4          7          3        0        1        0        75.00%       6.63        
---------------------------------------------------------------------------------------------------------
OVERALL    5          --         3        0        1        1        60.00%       5.30        
=========================================================================================================
```
