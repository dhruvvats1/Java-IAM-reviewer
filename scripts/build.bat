@echo off
cd /d "%~dp0.."
if not exist target\classes mkdir target\classes
javac --release 21 -d target\classes src\main\java\com\iamreview\*.java
if errorlevel 1 exit /b 1
jar --create --file target\iam-review.jar --main-class com.iamreview.Main -C target\classes .
if errorlevel 1 exit /b 1
echo Built target\iam-review.jar
