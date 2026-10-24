-- ============================================================================
-- Kongu Engineering College (Autonomous) - Regulation 2024
-- Student Academic & Continuous Evaluation System
-- Database Schema Script: kec_schema.sql
-- ============================================================================

CREATE DATABASE IF NOT EXISTS kec_academic;
USE kec_academic;

-- Drop tables in proper dependency order
DROP TABLE IF EXISTS assessment_marks;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;

-- 1. Students Table
CREATE TABLE students (
    roll_no VARCHAR(20) PRIMARY KEY,
    student_name VARCHAR(100) NOT NULL,
    department VARCHAR(30) NOT NULL,
    current_semester INT NOT NULL CHECK (current_semester BETWEEN 1 AND 8)
);

-- 2. Courses Table
CREATE TABLE courses (
    course_code VARCHAR(20) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    credits DECIMAL(4,1) NOT NULL CHECK (credits > 0),
    course_type ENUM('THEORY', 'PRACTICAL', 'EMBEDDED') NOT NULL
);

-- 3. Enrollments & Attendance Table
CREATE TABLE enrollments (
    enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    roll_no VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    semester INT NOT NULL CHECK (semester BETWEEN 1 AND 8),
    total_classes INT NOT NULL DEFAULT 0 CHECK (total_classes >= 0),
    classes_attended INT NOT NULL DEFAULT 0 CHECK (classes_attended >= 0),
    medical_condonation_approved BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uq_enrollment (roll_no, course_code, semester),
    FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE,
    FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE,
    CHECK (classes_attended <= total_classes)
);

-- 4. Assessment Marks Table (Phased Incremental Evaluation)
CREATE TABLE assessment_marks (
    enrollment_id BIGINT PRIMARY KEY,
    cia1 DECIMAL(5,2) NULL CHECK (cia1 IS NULL OR (cia1 BETWEEN 0 AND 100)),
    cia2 DECIMAL(5,2) NULL CHECK (cia2 IS NULL OR (cia2 BETWEEN 0 AND 100)),
    assignment_mark DECIMAL(5,2) NULL CHECK (assignment_mark IS NULL OR (assignment_mark BETWEEN 0 AND 100)),
    ese_mark DECIMAL(5,2) NULL CHECK (ese_mark IS NULL OR (ese_mark BETWEEN 0 AND 100)),
    ese_absent BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE,
    CHECK (NOT (ese_absent = TRUE AND ese_mark IS NOT NULL))
);
