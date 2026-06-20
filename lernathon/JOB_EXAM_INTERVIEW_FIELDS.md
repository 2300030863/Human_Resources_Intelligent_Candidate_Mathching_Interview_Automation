# Job Posting Enhancements - Exam & Interview Configuration

## Summary
Added new fields to the Job entity to configure exam and interview requirements for job postings.

## New Fields Added to Job Entity

### 1. Exam Pass Rate
- **Field Name**: `examPassRate`
- **Type**: `Double`
- **Description**: Minimum pass rate required for candidates to pass the exam (0-100)
- **Default Value**: 60.0
- **Database Column**: `exam_pass_rate`

### 2. Interview Pass Rate
- **Field Name**: `interviewPassRate`
- **Type**: `Double`
- **Description**: Minimum pass rate required for candidates to pass the interview (0-100)
- **Default Value**: 70.0
- **Database Column**: `interview_pass_rate`

### 3. Interview Question Mode
- **Field Name**: `interviewQuestionMode`
- **Type**: `Enum (InterviewQuestionMode)`
- **Description**: Difficulty level for interview questions
- **Database Column**: `interview_question_mode`
- **Possible Values**:
  - `BASIC` - Basic level questions
  - `INTERMEDIATE` - Intermediate level questions (default)
  - `ADVANCED` - Advanced level questions

## Files Modified

### 1. Job Entity
- **File**: `backend/src/main/java/com/lernathon/recruitment/entity/Job.java`
- **Changes**:
  - Added `examPassRate` field
  - Added `interviewPassRate` field
  - Added `interviewQuestionMode` field
  - Created `InterviewQuestionMode` enum

### 2. Database Migration Script
- **File**: `backend/add-job-exam-interview-fields.sql`
- **Purpose**: Adds the three new columns to the `jobs` table with default values

## API Usage

### Creating a Job with New Fields

```json
POST /api/jobs
{
  "title": "Senior Java Developer",
  "description": "We are looking for...",
  "department": "Engineering",
  "location": "Remote",
  "employmentType": "FULL_TIME",
  "salaryRange": "$100,000 - $150,000",
  "experienceRequired": 5,
  "skillsRequired": "Java, Spring Boot, MySQL",
  "status": "OPEN",
  "openings": 2,
  "examPassRate": 70.0,
  "interviewPassRate": 75.0,
  "interviewQuestionMode": "ADVANCED"
}
```

### Updating Job Configuration

```json
PUT /api/jobs/{id}
{
  "examPassRate": 65.0,
  "interviewPassRate": 80.0,
  "interviewQuestionMode": "INTERMEDIATE"
}
```

### Response Example

```json
{
  "id": 1,
  "title": "Senior Java Developer",
  "description": "We are looking for...",
  "department": "Engineering",
  "location": "Remote",
  "employmentType": "FULL_TIME",
  "salaryRange": "$100,000 - $150,000",
  "experienceRequired": 5,
  "skillsRequired": "Java, Spring Boot, MySQL",
  "status": "OPEN",
  "openings": 2,
  "examPassRate": 70.0,
  "interviewPassRate": 75.0,
  "interviewQuestionMode": "ADVANCED",
  "createdAt": "2026-02-19T16:30:00Z",
  "updatedAt": "2026-02-19T16:30:00Z"
}
```

## Database Schema Changes

The following columns were added to the `jobs` table:

```sql
ALTER TABLE jobs 
ADD COLUMN exam_pass_rate DOUBLE DEFAULT 60.0;

ALTER TABLE jobs 
ADD COLUMN interview_pass_rate DOUBLE DEFAULT 70.0;

ALTER TABLE jobs 
ADD COLUMN interview_question_mode VARCHAR(20) DEFAULT 'INTERMEDIATE';
```

## Integration Points

These new fields can be used by:

1. **Exam Service**: Check `examPassRate` when evaluating exam results
2. **Interview Service**: Check `interviewPassRate` when evaluating interview performance
3. **Interview Question Generator**: Use `interviewQuestionMode` to select appropriate difficulty level questions
4. **Frontend Job Form**: Display new fields for job posting creation/editing

## Frontend Updates Needed

To fully utilize these new fields, update the job posting form in the frontend to include:

1. **Exam Pass Rate Input**
   - Number input field (0-100)
   - Label: "Minimum Exam Pass Rate (%)"
   - Default: 60

2. **Interview Pass Rate Input**
   - Number input field (0-100)
   - Label: "Minimum Interview Pass Rate (%)"
   - Default: 70

3. **Interview Question Mode Dropdown**
   - Select field with options: Basic, Intermediate, Advanced
   - Label: "Interview Question Difficulty"
   - Default: Intermediate

## Testing

After the backend restarts:

1. **Create a new job** with all three new fields
2. **Verify the fields** are saved correctly in the database
3. **Update a job** to change the pass rates and question mode
4. **Retrieve a job** and verify the fields are returned in the response

## Notes

- The database schema will be automatically updated when the backend starts (using Hibernate's `ddl-auto: update`)
- Existing jobs will have default values applied
- All fields are optional and can be null
- Frontend should provide sensible defaults for user experience
