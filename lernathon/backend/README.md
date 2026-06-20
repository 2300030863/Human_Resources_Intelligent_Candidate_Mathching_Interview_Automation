# Lernathon Recruitment Backend

Spring Boot backend service for the recruitment application.

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.2**
- **Spring Security** with JWT authentication
- **Spring Data JPA** with Hibernate
- **MySQL** database
- **Maven** build tool
- **Lombok** for reducing boilerplate code

## Features

- RESTful API endpoints for recruitment management
- JWT-based authentication and authorization
- Role-based access control (ADMIN, RECRUITER, HIRING_MANAGER)
- Comprehensive entity models for candidates, jobs, applications, and interviews
- Candidate search and filtering
- Application tracking and matching
- Interview scheduling and management
- CORS configuration for frontend integration

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+

## Setup

1. **Install MySQL** and create a database:
   ```sql
   CREATE DATABASE recruitment_db;
   ```

2. **Update database credentials** in `src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/recruitment_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
       username: your_username
       password: your_password
   ```

3. **Update JWT secret** in `application.yml` (use a secure key in production):
   ```yaml
   jwt:
     secret: your-secret-key-change-this-in-production-min-256-bits-long
   ```

4. **Install dependencies**:
   ```bash
   mvn clean install
   ```

5. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

The server will start on `http://localhost:8080/api`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token

### Candidates
- `GET /api/candidates` - Get all candidates
- `GET /api/candidates/{id}` - Get candidate by ID
- `POST /api/candidates` - Create new candidate
- `PUT /api/candidates/{id}` - Update candidate
- `DELETE /api/candidates/{id}` - Delete candidate
- `GET /api/candidates/search?keyword={keyword}` - Search candidates
- `GET /api/candidates/status/{status}` - Get candidates by status

### Jobs
- `GET /api/jobs` - Get all jobs
- `GET /api/jobs/{id}` - Get job by ID
- `POST /api/jobs` - Create new job
- `PUT /api/jobs/{id}` - Update job
- `DELETE /api/jobs/{id}` - Delete job
- `GET /api/jobs/status/{status}` - Get jobs by status
- `GET /api/jobs/department/{department}` - Get jobs by department

### Applications
- `GET /api/applications` - Get all applications
- `GET /api/applications/{id}` - Get application by ID
- `POST /api/applications?candidateId={id}&jobId={id}` - Create application
- `PUT /api/applications/{id}` - Update application
- `DELETE /api/applications/{id}` - Delete application
- `GET /api/applications/candidate/{candidateId}` - Get applications by candidate
- `GET /api/applications/job/{jobId}` - Get applications by job
- `GET /api/applications/status/{status}` - Get applications by status

### Interviews
- `GET /api/interviews` - Get all interviews
- `GET /api/interviews/{id}` - Get interview by ID
- `POST /api/interviews?applicationId={id}` - Create interview
- `PUT /api/interviews/{id}` - Update interview
- `DELETE /api/interviews/{id}` - Delete interview
- `GET /api/interviews/application/{applicationId}` - Get interviews by application
- `GET /api/interviews/interviewer/{interviewerId}` - Get interviews by interviewer
- `GET /api/interviews/date-range?start={datetime}&end={datetime}` - Get interviews by date range

### Health Check
- `GET /api/health` - Service health check

## Authentication

All endpoints except `/api/auth/**` and `/api/health` require authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## Database Schema

The application automatically creates/updates the database schema on startup using Hibernate DDL auto-update.

Main entities:
- **User** - System users with roles
- **Candidate** - Job candidates
- **Job** - Job postings
- **Application** - Candidate applications to jobs
- **Interview** - Scheduled interviews for applications

## Development

To run in development mode with hot reload:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Building for Production

```bash
mvn clean package
java -jar target/recruitment-backend-1.0.0.jar
```

## License

MIT License
