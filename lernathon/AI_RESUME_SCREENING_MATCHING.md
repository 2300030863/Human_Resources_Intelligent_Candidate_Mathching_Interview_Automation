# AI Resume Screening & Matching System

## Overview

This document describes the AI-powered resume screening and matching system that automatically detects candidate skills from uploaded resumes and updates their profiles for intelligent job matching.

## Architecture

### Components

1. **AI Matching Service (Python/Flask)** - Port 5000
   - Skill extraction using SkillNER and NLP
   - Semantic skill matching using Sentence Transformers
   - Resume analysis and parsing

2. **Backend API (Java/Spring Boot)** - Port 8089
   - Resume upload and parsing
   - Candidate management
   - Job matching orchestration

3. **Frontend (React/TypeScript)** - Port 5173
   - Resume upload interface
   - Candidate dashboard
   - Real-time AI screening results

## Workflow

### 1. Resume Upload & AI Screening

```
User uploads resume → Backend receives file → AI Service extracts skills → 
Candidate profile created/updated → Status changed to SCREENING
```

#### Frontend Flow:
1. User navigates to **Candidates** page
2. Uses **AI Resume Screening** component to upload resume (.pdf, .doc, .docx, .txt)
3. Frontend calls `/api/candidates/ai-screening` endpoint

#### Backend Processing:
```java
POST /api/candidates/ai-screening
- Parse resume file
- Extract text content
- Call AI service to detect skills
- Parse name, email, phone, experience, education
- Create new candidate or update existing (by email)
- Return AIResumeScreeningResponse
```

#### AI Service Processing:
```python
POST /extract-skills
- Receives resume text
- Uses SkillNER for advanced skill extraction
- Fallback to keyword matching if needed
- Returns list of detected skills
```

### 2. Candidate Profile Management

**New Candidate:**
- Status: `NEW`
- All detected fields populated
- Skills stored as comma-separated string

**Existing Candidate:**
- Status updated to `SCREENING`
- Fields updated with new information
- Skills merged/updated

### 3. AI Matching (Job Applications)

When a candidate applies for a job or an application is created:

```
Application created → Calculate match score → 
AI analyzes skills → Match percentage calculated → 
Application updated with score
```

#### Matching Algorithm:
```java
Match Score = (Skill Match × 60%) + (Experience Match × 30%) + (Location Match × 10%)
```

**Skill Matching:**
- AI Service: Semantic matching using embeddings (similarity threshold: 0.6)
- Fallback: Keyword-based matching
- Returns: match percentage, matched skills, missing skills

**Experience Matching:**
- Exact match: 100%
- Overqualified (1-2 years): 90%
- Underqualified: Gradual decrease

## API Endpoints

### Resume Screening

#### Upload Resume with AI Screening
```http
POST /api/candidates/ai-screening
Content-Type: multipart/form-data

Parameters:
- file: Resume file (PDF, DOC, DOCX, TXT)

Response:
{
  "candidateId": 123,
  "name": "John Doe",
  "email": "john.doe@email.com",
  "phone": "+1234567890",
  "experienceYears": 5,
  "education": "Bachelor's in Computer Science",
  "skills": ["Java", "Python", "Spring Boot", "React"],
  "status": "SCREENING",
  "message": "Candidate profile created successfully with AI-detected skills",
  "isNewCandidate": true
}
```

#### Parse Resume (Preview Only)
```http
POST /api/candidates/upload-resume
Content-Type: multipart/form-data

Parameters:
- file: Resume file

Response:
{
  "name": "John Doe",
  "email": "john.doe@email.com",
  "skills": ["Java", "Spring Boot"],
  "message": "Resume parsed successfully"
}
```

### AI Matching

#### Match Candidate Skills to Job
```http
POST /api/applications/{applicationId}/calculate-match

Response:
{
  "matchScore": 85.5,
  "matchedSkills": ["Java", "Spring Boot", "React"],
  "missingSkills": ["Kubernetes", "AWS"]
}
```

## AI Service Endpoints

### Extract Skills
```http
POST http://localhost:5000/extract-skills
Content-Type: application/json

{
  "text": "Experienced Java developer with Spring Boot and React skills..."
}

Response:
{
  "skills": ["Java", "Spring Boot", "React", "REST API"]
}
```

### Match Skills
```http
POST http://localhost:5000/match-skills
Content-Type: application/json

{
  "job_skills": ["Java", "Spring Boot", "Kubernetes"],
  "candidate_skills": ["Java", "Spring Boot", "Docker", "React"]
}

Response:
{
  "match_percentage": 75.5,
  "matched_skills": ["Java", "Spring Boot"],
  "missing_skills": ["Kubernetes"]
}
```

### Analyze Resume
```http
POST http://localhost:5000/analyze-resume
Content-Type: application/json

{
  "resume_text": "Full resume content...",
  "job_description": "Job requirements..."
}

Response:
{
  "candidate_skills": ["Java", "React", "Spring Boot"],
  "job_skills": ["Java", "Spring Boot", "Kubernetes"],
  "match_percentage": 66.67,
  "matched_skills": ["Java", "Spring Boot"],
  "missing_skills": ["Kubernetes"]
}
```

## Setup Instructions

### 1. Start AI Matching Service

```bash
cd ai-matching-service
python -m venv venv
venv\Scripts\activate  # Windows
source venv/bin/activate  # Linux/Mac
pip install -r requirements.txt
python app.py
```

The service will start on `http://localhost:5000`

### 2. Configure Backend

Ensure `application.yml` has AI service enabled:
```yaml
ai:
  matching:
    service:
      url: http://localhost:5000
      enabled: true
```

### 3. Start Backend

```bash
cd backend
mvn clean install
java -jar target/recruitment-backend-1.0.0.jar
```

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

## Usage Guide

### For Recruiters

1. **Upload Resume:**
   - Go to Candidates page
   - Use "AI Resume Screening" section
   - Select resume file
   - Click "Screen with AI"

2. **Review Results:**
   - View extracted information
   - Check AI-detected skills
   - Candidate automatically created/updated
   - Skills highlighted with AI badge

3. **Match to Jobs:**
   - Navigate to Jobs page
   - View matching candidates
   - AI automatically calculates match scores
   - See matched and missing skills

### For Candidates

1. **Apply with Resume:**
   - Browse available jobs
   - Upload resume when applying
   - AI extracts your skills automatically
   - Profile updated with detected information

2. **View Profile:**
   - Check extracted skills
   - Verify contact information
   - See application status

## Features

### AI-Powered Skill Detection
- ✅ Advanced NLP using SkillNER
- ✅ Semantic understanding of skills
- ✅ Industry-standard skill recognition
- ✅ Fallback keyword matching

### Intelligent Matching
- ✅ Semantic similarity scoring
- ✅ Experience level matching
- ✅ Location compatibility
- ✅ Weighted scoring algorithm

### Automated Processing
- ✅ Auto-create candidate profiles
- ✅ Auto-update existing candidates
- ✅ Auto-calculate match scores
- ✅ Status management

### User Experience
- ✅ Real-time processing feedback
- ✅ Visual skill badges
- ✅ Match percentage display
- ✅ Success/error notifications

## Technology Stack

### AI/ML
- **SkillNER**: Named Entity Recognition for skills
- **spaCy**: Natural Language Processing
- **Sentence Transformers**: Semantic similarity
- **all-MiniLM-L6-v2**: Embedding model

### Backend
- **Spring Boot**: REST API
- **MySQL**: Database
- **RestTemplate**: HTTP client for AI service

### Frontend
- **React**: UI framework
- **TypeScript**: Type safety
- **Tailwind CSS**: Styling
- **shadcn/ui**: Component library

## Troubleshooting

### AI Service Not Available
If the AI service is down, the system automatically falls back to keyword-based matching.

**Check AI Service:**
```bash
curl http://localhost:5000/health
```

**Expected Response:**
```json
{
  "status": "healthy",
  "service": "AI Matching Service"
}
```

### Skills Not Detected
1. Check resume format (PDF recommended)
2. Ensure skills are clearly mentioned in resume
3. Verify AI service is running
4. Check backend logs for errors

### Match Score Issues
1. Ensure job requirements specify skills clearly
2. Verify candidate skills are populated
3. Check AI service configuration
4. Review matching threshold settings

## Future Enhancements

- [ ] PDF text extraction improvements
- [ ] Multi-language support
- [ ] Custom skill taxonomy
- [ ] Machine learning model training
- [ ] Batch resume processing
- [ ] Advanced resume parsing (work history, certifications)
- [ ] Interview scheduling based on match scores
- [ ] Automated email screening notifications

## Support

For issues or questions:
1. Check application logs
2. Verify all services are running
3. Review API responses
4. Test AI service independently

## Conclusion

The AI Resume Screening and Matching system provides an automated, intelligent way to process candidate resumes and match them to jobs. By leveraging advanced NLP and semantic understanding, it significantly reduces manual screening time while improving matching accuracy.
