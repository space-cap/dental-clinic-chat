# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application for a real-time dental clinic customer consultation chat system (실시간 기반 치과 병원 고객 상담 채팅 시스템). The project is a Java-based web application using Spring Boot 3.5.4 with Java 21.

## Architecture

- **Framework**: Spring Boot 3.5.4
- **Java Version**: 21
- **Build Tool**: Maven
- **Template Engine**: Thymeleaf
- **Package Structure**: `com.ezlevup.dentalchat`
- **Application Entry Point**: `DentalchatApplication.java`

## Development Commands

### Building and Running
- **Build the project**: `./mvnw clean compile`
- **Run the application**: `./mvnw spring-boot:run`
- **Package the application**: `./mvnw clean package`
- **Run tests**: `./mvnw test`
- **Run specific test**: `./mvnw test -Dtest=TestClassName`

### Development Tools
- Spring Boot DevTools is included for automatic restart during development
- Application runs on default port 8080
- Static resources are served from `src/main/resources/static/`
- Thymeleaf templates are located in `src/main/resources/templates/`

## Project Structure

```
src/
├── main/
│   ├── java/com/ezlevup/dentalchat/
│   │   └── DentalchatApplication.java    # Main Spring Boot application
│   └── resources/
│       ├── application.properties        # Application configuration
│       ├── static/                      # Static web resources
│       └── templates/                   # Thymeleaf templates
└── test/
    └── java/com/ezlevup/dentalchat/
        └── DentalchatApplicationTests.java # Basic application tests
```

## Key Dependencies

- `spring-boot-starter-web`: Web application support
- `spring-boot-starter-thymeleaf`: Template engine
- `spring-boot-devtools`: Development tools for automatic restart
- `spring-boot-starter-test`: Testing framework including JUnit 5

## Configuration

- Application name: `dentalchat` (configured in application.properties)
- No additional database or security configurations are currently present
- Uses standard Spring Boot auto-configuration

## Testing

- Tests use JUnit 5 and Spring Boot Test
- Basic context loading test is included
- Run all tests with `./mvnw test`