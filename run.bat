@echo off

REM Script to run the Multithreading Demo Java application on Windows

echo Building the Multithreading Demo...
mvn clean compile

if %errorlevel% equ 0 (
    echo Build successful. Starting the application...
    mvn javafx:run
) else (
    echo Build failed. Please check the errors above.
    pause
    exit /b 1
)
