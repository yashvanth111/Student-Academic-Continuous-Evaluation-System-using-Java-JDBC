@echo off
title KEC Academic & Continuous Evaluation System (R2024)
echo ===============================================================================
echo   KONGU ENGINEERING COLLEGE (AUTONOMOUS) - REGULATION 2024
echo   STUDENT ACADEMIC & CONTINUOUS EVALUATION SYSTEM (JDBC)
echo ===============================================================================
echo [1/2] Compiling Java source files...
javac -cp ".;mysql-connector-j-26.7.0.jar" DBConnection.java StudentDAO.java AcademicDAO.java MainApp.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed! Check JDK installation and classpath.
    pause
    exit /b %errorlevel%
)
echo [2/2] Launching Menu-Driven Interface...
echo.
java -cp ".;mysql-connector-j-26.7.0.jar" MainApp
pause
