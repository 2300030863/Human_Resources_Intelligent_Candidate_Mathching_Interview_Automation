# Quick Start: AI Resume Screening & Matching

This guide helps you quickly set up and use the AI-powered resume screening and matching system.

## Prerequisites

- **Python 3.8+** (for AI service)
- **Java 11+** (for backend)
- **Node.js 16+** (for frontend)
- **MySQL 8.0+** (for database)

## Step 1: Start AI Matching Service

The AI service must be running first for skill detection to work.

### Windows:
```bash
cd ai-matching-service
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

### Linux/Mac:
```bash
cd ai-matching-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python app.py
```

✅ **Verify it's running:**
```bash
curl http://localhost:5000/health
```

Expected response:
```json
{"status": "healthy", "service": "AI Matching Service"}
```

## Step 2: Setup Database

```bash
cd backend
mysql -u root -p < create-database.sql
```

Or use the batch file:
```bash
create-database.bat
```

## Step 3: Start Backend

```bash
cd backend
mvn clean install
java -jar target/recruitment-backend-1.0.0.jar
```

✅ **Verify it's running:**
```bash
curl http://localhost:8089/api/health
```

## Step 4: Start Frontend

```bash
cd frontend
npm install
npm run dev
```

✅ **Access the application:**
Open your browser to `http://localhost:5173`

## Step 5: Test AI Resume Screening

### Option 1: Web Interface (Recommended)

1. Login to the application
2. Navigate to **Candidates** page
3. Find the **"AI Resume Screening"** section (blue gradient card)
4. Click "Choose File" and select a resume (PDF, DOC, DOCX, or TXT)
5. Click **"Screen with AI"**
6. Wait for processing (AI icon will animate)
7. Review results:
   - ✅ Candidate created/updated
   - ✅ Skills detected automatically
   - ✅ Profile populated with resume data

### Option 2: API Testing

```bash
# Test skill extraction
curl -X POST http://localhost:5000/extract-skills \
  -H "Content-Type: application/json" \
  -d '{"text": "Experienced Java developer with Spring Boot, React, and AWS skills. 5 years of experience."}'

# Expected response:
{
  "skills": ["Java", "Spring Boot", "React", "AWS"]
}
```

```bash
# Test resume screening (replace with your token)
curl -X POST http://localhost:8089/api/candidates/ai-screening \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/resume.pdf"

# Expected response:
{
  "candidateId": 1,
  "name": "John Doe",
  "email": "john.doe@email.com",
  "skills": ["Java", "Spring Boot", "React"],
  "status": "NEW",
  "message": "New candidate profile created successfully with AI-detected skills",
  "isNewCandidate": true
}
```

## Step 6: Test Job Matching

1. Create or view a Job posting
2. The system automatically calculates match scores for all candidates
3. View the **Matching** page to see:
   - Match percentage
   - Matched skills (green)
   - Missing skills (red)
   - Sorted by best match

## Features You Just Enabled

### ✅ Automatic Skill Detection
- Upload any resume format
- AI extracts skills using NLP
- No manual entry needed

### ✅ Intelligent Matching
- Semantic skill matching (not just keywords)
- Experience level consideration
- Weighted scoring algorithm

### ✅ Profile Management
- Auto-create new candidates
- Auto-update existing profiles
- Email-based deduplication

### ✅ Real-time Processing
- Instant feedback
- Visual progress indicators
- Success notifications

## Troubleshooting

### AI Service Won't Start

**Error:** `ModuleNotFoundError: No module named 'skillner'`

**Solution:**
```bash
pip install skillner spacy flask flask-cors sentence-transformers
python -m spacy download en_core_web_sm
```

### Backend Connection Error

**Error:** `Connection refused to localhost:5000`

**Solution:** Ensure AI service is running first. Check with:
```bash
curl http://localhost:5000/health
```

### Skills Not Detected

**Cause:** AI service not running or not enabled

**Solution:** Check `backend/src/main/resources/application.yml`:
```yaml
ai:
  matching:
    service:
      url: http://localhost:5000
      enabled: true  # Must be true
```

### Resume Upload Fails

**Cause:** File size too large or unsupported format

**Solution:** 
- Use PDF, DOC, DOCX, or TXT files
- Keep file size under 10MB
- Ensure text is machine-readable (not scanned images)

## Sample Resume for Testing

Create a file `test_resume.txt`:

```
John Doe
john.doe@email.com
+1-555-123-4567

PROFESSIONAL SUMMARY
Experienced Full Stack Developer with 5 years of experience building web applications 
using modern technologies.

TECHNICAL SKILLS
- Programming: Java, Python, JavaScript, TypeScript
- Frameworks: Spring Boot, React, Node.js, Express
- Databases: MySQL, PostgreSQL, MongoDB
- Cloud: AWS, Docker, Kubernetes
- Tools: Git, Jenkins, Maven, npm

EXPERIENCE
Senior Software Engineer | Tech Corp | 2020-Present
- Developed microservices using Spring Boot and Java
- Built responsive UIs with React and TypeScript
- Deployed applications on AWS using Docker containers

EDUCATION
Bachelor of Science in Computer Science
University of Technology | 2018
```

Upload this file to test the AI screening.

## Next Steps

1. **Customize Skills**: Update skill lists in AI service (`ai-matching-service/app.py`)
2. **Adjust Matching**: Modify weights in `MatchingService.java`
3. **Add More Jobs**: Create job postings with required skills
4. **Upload Resumes**: Process multiple candidates
5. **Review Matches**: Check candidate-job matches on Matching page

## Configuration Files

### AI Service Enabled
📄 `backend/src/main/resources/application.yml`
```yaml
ai:
  matching:
    service:
      url: http://localhost:5000
      enabled: true
```

### CORS for Frontend
📄 `backend/src/main/resources/application.yml`
```yaml
cors:
  allowed-origins: http://localhost:5173
```

## Important Endpoints

| Service | Endpoint | Port | Purpose |
|---------|----------|------|---------|
| Frontend | http://localhost:5173 | 5173 | User interface |
| Backend API | http://localhost:8089/api | 8089 | REST API |
| AI Service | http://localhost:5000 | 5000 | Skill extraction |
| Database | localhost:3306 | 3306 | MySQL |

## Support

- 📖 Full documentation: `AI_RESUME_SCREENING_MATCHING.md`
- 🐛 Check logs in terminal windows
- 🔍 Use browser DevTools for frontend errors
- 📝 Backend logs in console output

## Success Checklist

- [ ] AI service responding at port 5000
- [ ] Backend responding at port 8089
- [ ] Frontend accessible at port 5173
- [ ] Database connection successful
- [ ] AI Resume Screening component visible
- [ ] Test resume uploaded successfully
- [ ] Skills detected and displayed
- [ ] Candidate profile created
- [ ] Match scores calculated

## You're All Set! 🎉

You now have a fully functional AI-powered resume screening and matching system. Upload resumes and watch the AI automatically extract skills and create candidate profiles!
