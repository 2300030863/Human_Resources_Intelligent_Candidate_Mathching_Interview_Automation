# Quick Start Guide

## Getting Started in 5 Minutes

### 1. Get Your API Key

Sign up for Groq API: https://console.groq.com/
Copy your API key

### 2. Setup

```bash
# Install dependencies
pip install -r requirements.txt

# Create .env file
copy .env.example .env

# Edit .env and add your GROQ_API_KEY
```

### 3. Initialize Database

```bash
python -m database.connection
```

### 4. Run the App

```bash
streamlit run frontend/app.py
```

### 5. Start Interviewing!

1. Fill in candidate details
2. Click "Start Interview"
3. Answer questions with your voice
4. Get instant AI feedback

## Testing Without Voice

Use the simple version to test AI features without audio:

```bash
streamlit run frontend/app_simple.py
```

## Common Commands

### Run Backend API

```bash
python backend/main.py
```

### Test API

Visit: http://localhost:8000/docs

### Check Database

```bash
python
>>> from database import SessionLocal
>>> from models.database import Interview
>>> db = SessionLocal()
>>> interviews = db.query(Interview).all()
>>> for i in interviews:
...     print(f"{i.candidate_name}: {i.total_score}/10")
```

## Example Interview

**Job Role:** Full Stack Developer
**Skills:** Python, React, PostgreSQL
**Difficulty:** Intermediate

**Sample Question:** 
"Explain how you would design a RESTful API for a blog platform with authentication and authorization."

**Good Answer:**
"I would use FastAPI for the backend with JWT authentication. The API would have endpoints for users (/users), posts (/posts), and comments (/comments). I'd implement OAuth2 with password flow for authentication, store hashed passwords using bcrypt, and use role-based access control for authorization..."

**Evaluation:**
- Technical: 4/5
- Clarity: 3/3  
- Communication: 2/2
- **Total: 9/10**

## Tips for Best Results

### For Candidates

1. Speak clearly and at moderate pace
2. Structure your answers (intro, main points, conclusion)
3. Use technical terms correctly
4. Give practical examples
5. Keep answers concise but complete

### For Interviewers

1. Choose appropriate difficulty level
2. Select relevant skills
3. Review evaluation feedback
4. Adjust difficulty based on performance
5. Use consistent criteria

## Troubleshooting

### "GROQ_API_KEY not found"
Edit your `.env` file and add the API key

### "No module named 'groq'"
Run: `pip install -r requirements.txt`

### "Database connection failed"
Check DATABASE_URL in `.env` or use SQLite:
```
DATABASE_URL=sqlite:///./interview.db
```

### "Audio recording not working"
1. Check microphone permissions
2. Try different browser
3. Test with app_simple.py first

### "Whisper model download stuck"
Use smaller model in config/settings.py:
```python
WHISPER_MODEL = "tiny"  # or "base"
```

## Next Steps

1. ✅ Complete your first interview
2. ✅ Review results in database
3. ✅ Customize questions for your role
4. ✅ Try different difficulty levels
5. ✅ Explore API endpoints
6. ✅ Build custom integrations

## Resources

- FastAPI Docs: https://fastapi.tiangolo.com/
- Streamlit Docs: https://docs.streamlit.io/
- Groq API: https://console.groq.com/docs
- Whisper: https://github.com/openai/whisper

Happy Interviewing! 🎤✨
