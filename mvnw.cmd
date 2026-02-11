@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script, Windows version
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MVNW_VERBOSE=false
if not "%MVNW_VERBOSE%"=="true" (
  @echo off
)

set MAVEN_PROJECTBASEDIR=%~dp0
if "%MAVEN_PROJECTBASEDIR%"=="" set MAVEN_PROJECTBASEDIR=.
set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

if not exist %WRAPPER_JAR% (
  echo Maven wrapper jar not found at %WRAPPER_JAR%
  exit /b 1
)
if not exist %WRAPPER_PROPERTIES% (
  echo Maven wrapper properties not found at %WRAPPER_PROPERTIES%
  exit /b 1
)

set JAVA_EXE=java.exe
if not "%JAVA_HOME%"=="" (
  set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
)

%JAVA_EXE% -classpath %WRAPPER_JAR% -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%

