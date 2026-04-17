@rem Gradle startup script for Windows
@echo off

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo ERROR: gradle-wrapper.jar not found.
    echo Run: gradle wrapper --gradle-version 8.8
    exit /b 1
)

@rem Find java.exe
if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%/bin/java.exe
) else (
    set JAVA_EXE=java.exe
)

%JAVA_EXE% -Xmx64m -Xms64m -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
