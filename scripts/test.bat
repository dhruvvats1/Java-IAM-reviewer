@echo off
cd /d "%~dp0.."
call scripts\build.bat
if errorlevel 1 exit /b 1
if not exist target\test-classes mkdir target\test-classes
javac --release 21 -cp target\classes -d target\test-classes src\test\java\com\iamreview\*.java
if errorlevel 1 exit /b 1
java -cp target\classes;target\test-classes com.iamreview.AuditTests
