# AI-Powered Resume Skill Extraction - Integration Guide

## Overview

This system automatically extracts skills from uploaded resumes when candidates apply for jobs. The skills are detected using AI and automatically fill the candidate profile.

## Architecture

```
Frontend (React)  →  Backend (Java/Spring)  →  AI Service (Python/Flask)
     ↓                      ↓                         ↓
  Upload Resume    Create/Update Candidate     Extract Skills via AI
```

## Components

### 1. **Frontend (React/TypeScript)**
- **Location**: `frontend/src/pages/ApplyJobs.tsx`
- **Features**:
  - Resume upload with drag & drop
  - Automatic skill extraction on upload
  - Visual feedback for AI processing
  - Auto-fill candidate information
  - Shows candidate profile ID after creation

### 2. **Backend (Java/Spring Boot)**
- **Location**: `backend/src/main/java/com/lernathon/recruitment/`
- **Endpoints**:
  - `POST /candidates/ai-screening` - AI-powered resume screening
  - `POST /candidates/upload-resume` - Basic resume parsing
- **Features**:
  - Creates or updates candidate profile automatically
  - Extracts: name, email, phone, education, experience, skills
  - Falls back to keyword matching if AI service unavailable

### 3. **AI Service (Python/Flask)**
- **Location**: `AI-Resume-Analyzer/api_service.py`
- **Port**: 5000
- **Endpoints**:
  - `GET /health` - Health check
  - `POST /extract-skills` - Extract skills from resume text
  - `POST /analyze-resume` - Comprehensive analysis
- **AI Model**: Groq Llama 3.3 70B

## Setup Instructions

### Step 1: Configure Environment

1. Ensure `.env` file exists in `AI-Resume-Analyzer/` with your Groq API key:
   ```
   GROQ_API_KEY=your_api_key_here
   ```

### Step 2: Install Python Dependencies

```bash
cd AI-Resume-Analyzer
pip install -r requirements.txt
```

Or use the virtual environment:
```bash
.\venv-standard\Scripts\activate
pip install flask flask-cors groq python-dotenv
```

### Step 3: Start the AI Service

**Option A: Using the batch script**
```bash
cd AI-Resume-Analyzer
.\start-api.bat
```

**Option B: Manually**
```bash
cd AI-Resume-Analyzer
python api_service.py
```

The API will start on `http://localhost:5000`

### Step 4: Start the Backend

```bash
cd backend
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`

### Step 5: Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173` (or your configured port)

## How It Works

### Workflow

1. **Candidate uploads resume** on the job application form
2. **Frontend** sends file to backend `/candidates/ai-screening` endpoint
3. **Backend** extracts text from resume (PDF/DOC/TXT)
4. **Backend** calls AI service at `/extract-skills` with resume text
5. **AI Service** uses Groq LLM to intelligently extract skills
6. **Backend** creates/updates candidate profile with extracted data
7. **Frontend** displays extracted skills and auto-fills form
8. **Candidate** can review/edit and submit application

### What Gets Auto-Extracted

✅ **Name** - Full name from resume  
✅ **Email** - Email address  
✅ **Phone** - Phone number  
✅ **Skills** - Technical skills (AI-powered)  
✅ **Experience** - Years of experience  
✅ **Education** - Educational background  
✅ **Candidate Profile** - Auto-created in database  

## Testing the Integration

### Test 1: Health Check

```bash
curl http://localhost:5000/health
```

Expected response:
```json
{
  "status": "healthy",
  "service": "AI Resume Skill Extraction API"
}
```

### Test 2: Skill Extraction

```bash
curl -X POST http://localhost:5000/extract-skills \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Experienced software engineer with 5 years in Java, Spring Boot, React, and PostgreSQL. Proficient in Docker, Kubernetes, and AWS cloud services."
  }'
```

Expected response:
```json
{
  "skills": ["Java", "Spring Boot", "React", "PostgreSQL", "Docker", "Kubernetes", "AWS"],
  "count": 7
}
```

### Test 3: End-to-End Test

1. Open browser to `http://localhost:5173`
2. Navigate to "Apply Jobs" or job listings page
3. Click "Apply Now" on any job
4. Upload a resume (PDF, DOC, or TXT)
5. Watch for:
   - ✨ "AI is analyzing your resume..." spinner
   - ✅ Skills auto-filled in the form
   - ✅ "Profile Auto-Created! ✨" notification
   - ✅ "Profile auto-created (ID: X)" indicator

## Configuration

### Backend Configuration

File: `backend/src/main/resources/application.yml`

```yaml
ai:
  matching:
    service:
      url: http://localhost:5000
      enabled: true
```

- Set `enabled: false` to use keyword matching fallback
- Change `url` if AI service runs on different port

### AI Service Configuration

File: `AI-Resume-Analyzer/.env`

```
GROQ_API_KEY=your_groq_api_key_here
```

Get your API key from: https://console.groq.com

## Troubleshooting

### Issue: "AI Service Unavailable" message

**Solution**: 
- Check if AI service is running: `curl http://localhost:5000/health`
- Check backend configuration in `application.yml`
- Check Groq API key in `.env` file

### Issue: Skills not being detected

**Solution**:
- Check AI service logs for errors
- Verify resume text extraction is working
- Test with sample text: see Test 2 above
- Check if GROQ_API_KEY is valid

### Issue: Candidate profile not created

**Solution**:
- Check backend logs for errors
- Verify database connection
- Check if email already exists for existing candidate (will update instead)
- Ensure resume contains extractable information

### Issue: Port 5000 already in use

**Solution**:
```bash
# Windows
netstat -ano | findstr :5000
taskkill /PID <PID> /F

# Or change port in api_service.py:
app.run(host='0.0.0.0', port=5001, debug=True)
```

## API Reference

### POST /extract-skills

Extract skills from resume text using AI.

**Request:**
```json
{
  "text": "resume text content"
}
```

**Response:**
```json
{
  "skills": ["Java", "Python", "React"],
  "count": 3
}
```

### POST /candidates/ai-screening

Backend endpoint for comprehensive AI resume screening.

**Request:** Multipart form data with file

**Response:**
```json
{
  "candidateId": 123,
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "skills": ["Java", "React", "Spring Boot"],
  "experienceYears": 5,
  "education": "BS Computer Science",
  "status": "NEW",
  "isNewCandidate": true,
  "message": "New candidate profile created successfully with AI-detected skills"
}
```

## Features

✨ **AI-Powered Extraction** - Uses advanced LLM for intelligent skill detection  
🚀 **Auto-Create Profiles** - Candidate profiles created automatically  
⚡ **Real-time Processing** - Instant feedback during upload  
🔄 **Fallback Support** - Keyword matching if AI service unavailable  
📊 **Visual Feedback** - Clear indicators of AI processing status  
✅ **Smart Detection** - Names, emails, phones, education auto-extracted  

## Performance Tips

1. **PDF Extraction**: For better PDF text extraction, ensure proper PDF parsing (consider Apache PDFBox)
2. **Caching**: Consider caching skill extraction results for identical resumes
3. **Async Processing**: For large files, consider async processing with job queue
4. **Rate Limiting**: Add rate limiting to AI service for production

## Security Considerations

- Validate all uploaded files (type, size)
- Sanitize extracted text before sending to AI
- Don't expose internal errors to frontend
- Use HTTPS in production
- Implement proper authentication/authorization
- Store resumes securely with encryption

## Next Steps

1. ✅ AI service is now connected
2. ✅ Skills auto-extraction is working
3. ✅ Candidate profiles auto-created
4. 🔜 Add support for more file formats (DOCX parsing)
5. 🔜 Enhance education extraction
6. 🔜 Add experience timeline parsing
7. 🔜 Implement resume scoring/matching

## Support

For issues or questions:
1. Check logs: Backend console, AI service console
2. Review error messages in browser console
3. Test individual components separately
4. Verify all services are running

---

**Created**: February 2026  
**Version**: 1.0
