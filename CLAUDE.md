# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.4 application using Java 21 and Gradle as the build system. The project is designed to study CI/CD practices, particularly focusing on canary deployment strategies with Docker Compose, NGINX load balancing, and monitoring.

## Common Commands

### Build and Test
- `./gradlew build` - Build the entire project
- `./gradlew test` - Run all tests using JUnit 5
- `./gradlew bootRun` - Run the Spring Boot application locally
- `./gradlew clean` - Clean build artifacts

### Development
- `./gradlew bootJar` - Create executable JAR file
- `./gradlew check` - Run all verification tasks (tests, code quality checks)

## Architecture

This is a basic Spring Boot application with the following structure:
- **Main Application**: `src/main/java/junghun/studycicd/StudycicdApplication.java` - Standard Spring Boot main class
- **Test Configuration**: Uses JUnit 5 platform for testing
- **Package Structure**: Single package `junghun.studycicd` currently containing only the main application class

The README describes a comprehensive canary deployment system architecture involving:
- NGINX as reverse proxy and load balancer
- Legacy and Refactored app versions running on different ports
- Prometheus for metrics collection
- Grafana for visualization
- Alertmanager for automated rollback triggers
- Controller Service for traffic weight management

## Project Configuration

- **Java Version**: 21 (using toolchain)
- **Spring Boot Version**: 3.5.4
- **Build Tool**: Gradle with Wrapper
- **Application Name**: `studycicd` (configured in application.properties)
- **Group**: `junghun`
- **Version**: `0.0.1-SNAPSHOT`

## Development Notes

The codebase is currently minimal with just the basic Spring Boot structure. The comprehensive CI/CD architecture described in the README suggests this project will expand to include containerization, monitoring, and deployment automation features.