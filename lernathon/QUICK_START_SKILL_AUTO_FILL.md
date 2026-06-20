# Quick Start: Test Auto Skill Extraction

## 1. Start AI Service (Terminal 1)

```bash
cd "AI-Resume-Analyzer"
.\start-api.bat
```

Wait for: `Running on http://127.0.0.1:5000`

## 2. Start Backend (Terminal 2)

```bash
cd backend
mvn spring-boot:run
```

Wait for: `Started Application in X seconds`

## 3. Start Frontend (Terminal 3)

```bash
cd frontend
npm run dev
```

Open: http://localhost:5173

## 4. Test It!

1. **Go to job listings** (Apply Jobs page)
2. **Click "Apply Now"** on any job
3. **Upload a resume** (PDF, DOC, or TXT)
4. **Watch the magic!** ✨
   - AI analyzes resume
   - Skills auto-populate
   - Candidate profile auto-created
   - See profile ID displayed

## Expected Behavior

✅ Spinner shows "AI is analyzing your resume..."  
✅ Skills field auto-fills with detected skills  
✅ Toast notification: "Profile Auto-Created! ✨"  
✅ Green badge shows: "Profile auto-created (ID: X)"  
✅ Phone and experience auto-filled if detected

## Test with Sample Resume

Create a file `test-resume.txt`:

```
John Doe
john.doe@email.com
+1 (555) 123-4567

EXPERIENCE
Senior Software Engineer - 8 years

SKILLS
- Java, Spring Boot, Hibernate
- React, TypeScript, Redux
- PostgreSQL, MongoDB, Redis
- Docker, Kubernetes, AWS
- Microservices, REST API

EDUCATION
Master of Computer Science
Stanford University, 2015
```

## Troubleshooting

**AI Service Not Running?**
```bash
curl http://localhost:5000/health
```

**Backend Not Running?**
```bash
curl http://localhost:8080/api/jobs
```

**Check Logs:**
- AI Service: Terminal 1 output
- Backend: Terminal 2 output  
- Frontend: Browser console (F12)

## Success Indicators

1. ✅ AI service health check returns `{"status": "healthy"}`
2. ✅ Resume upload shows loading spinner
3. ✅ Skills appear in the skills field
4. ✅ Toast notification appears
5. ✅ Candidate ID displayed under resume name

---

🎉 **That's it!** Your auto skill extraction is working!
