# Database Setup Guide

## Step 1: Create MySQL Database

You need to create the MySQL database before running the backend application.

### Option 1: Using MySQL Command Line

1. Open MySQL command line or any MySQL client:
   ```bash
   mysql -u root -p
   ```

2. Enter your MySQL password when prompted

3. Run the following SQL command:
   ```sql
   CREATE DATABASE recruitment_db;
   ```

4. Verify the database was created:
   ```sql
   SHOW DATABASES;
   ```

5. Exit MySQL:
   ```sql
   EXIT;
   ```

### Option 2: Using MySQL Workbench

1. Open MySQL Workbench
2. Connect to your MySQL server
3. Click on "Create a new schema" button (database icon)
4. Enter schema name: `recruitment_db`
5. Click "Apply"

### Option 3: Using the SQL Script

1. Navigate to the backend folder:
   ```bash
   cd backend
   ```

2. Run the SQL script:
   ```bash
   mysql -u root -p < create-database.sql
   ```

## Step 2: Update Database Credentials

Edit `backend/src/main/resources/application.yml` and update the database credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/recruitment_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root          # Change to your MySQL username
    password: 12345         # Change to your MySQL password
```

## Step 3: Start the Backend

After creating the database, start the Spring Boot backend:

```bash
cd backend
mvn spring-boot:run
```

Spring Boot will automatically:
- Connect to the MySQL database
- Create all necessary tables (users, candidates, jobs, applications, interviews)
- Set up the schema based on your entity classes

## Database Tables

Once the backend starts successfully, the following tables will be automatically created:

- **users** - System users (recruiters, admins, hiring managers)
- **candidates** - Job candidates
- **jobs** - Job postings
- **applications** - Candidate applications to jobs
- **interviews** - Scheduled interviews

## Verify Everything is Working

1. Backend should start on **http://localhost:8089/api**
2. Check health endpoint: http://localhost:8089/api/health
3. Frontend should connect on **http://localhost:5174/**
4. Try registering a new user - data will be stored in MySQL!

## Troubleshooting

### Error: "Access denied for user 'root'@'localhost'"
- Your MySQL password is incorrect
- Update the password in `application.yml`

### Error: "Unknown database 'recruitment_db'"
- The database hasn't been created
- Follow Step 1 above to create the database

### Error: "Communications link failure"
- MySQL server is not running
- Start MySQL service:
  ```bash
  net start MySQL80
  ```

## Default User Role

When users register through the frontend, they are assigned the **RECRUITER** role by default. You can modify this in the `AuthController.java` if needed.
