# 🎯 Getting Started Checklist

Follow this checklist to get your AI Voice Interview System up and running!

## ☐ Prerequisites

- [ ] Python 3.9 or higher installed
- [ ] MySQL or PostgreSQL database (or use SQLite for testing)
- [ ] Microphone connected (for voice interviews)
- [ ] Speakers/headphones (to hear AI questions)
- [ ] Internet connection (for Groq API)

## ☐ Step 1: Get API Keys

### Groq API (Required)
- [ ] Visit: https://console.groq.com/
- [ ] Create account or sign in
- [ ] Navigate to API Keys section
- [ ] Create new API key
- [ ] Copy the API key (you'll need it soon)

### OpenAI API (Optional - for advanced features)
- [ ] Visit: https://platform.openai.com/
- [ ] Create account if using OpenAI Whisper API
- [ ] Get API key (optional, can use local Whisper)

## ☐ Step 2: Installation

### On Windows:
```bash
# Navigate to project directory
cd c:\project\interview

# Run setup script
setup.bat
```

### On Linux/Mac:
```bash
# Navigate to project directory
cd /path/to/interview

# Make scripts executable
chmod +x setup.sh run.sh

# Run setup script
./setup.sh
```

**Setup script will:**
- [ ] Create virtual environment
- [ ] Install all dependencies
- [ ] Create .env file from template

## ☐ Step 3: Configure Environment

- [ ] Open `.env` file in a text editor
- [ ] Add your Groq API key:
  ```
  GROQ_API_KEY=your_actual_api_key_here
  ```
- [ ] Configure database URL (or leave default for SQLite):
  ```
  DATABASE_URL=sqlite:///./interview.db
  ```
  Or for MySQL:
  ```
  DATABASE_URL=mysql+pymysql://username:password@localhost:3306/interview_system
  ```
- [ ] Save the file

## ☐ Step 4: Initialize Database

### Create Database (if using MySQL/PostgreSQL)
```sql
CREATE DATABASE interview_system;
```

### Initialize Tables
```bash
# Activate virtual environment first
venv\Scripts\activate  # Windows
source venv/bin/activate  # Linux/Mac

# Run initialization
python -m database.connection
```

You should see: "Database tables created successfully!"

## ☐ Step 5: Verify Installation

Run environment check:
```bash
python scripts/utils.py check-env
```

Expected output:
```
✅ .env file found
✅ GROQ_API_KEY configured
✅ DATABASE_URL configured
✅ Audio storage directory exists
```

## ☐ Step 6: Test Components

### Test AI Service
```bash
python scripts/utils.py test-ai
```
Should generate a question and evaluate a sample answer.

### Test Database
```bash
python scripts/utils.py test-db
```
Should create and query test records.

### List Audio Devices (Optional)
```bash
python scripts/utils.py list-devices
```
Shows available microphones.

### Run All Tests
```bash
python scripts/utils.py all
```
Comprehensive test of all components.

## ☐ Step 7: Start the Application

### Method 1: Using Run Script (Recommended)

**Windows:**
```bash
run.bat
```

**Linux/Mac:**
```bash
./run.sh
```

### Method 2: Manual Start

```bash
# Activate virtual environment
venv\Scripts\activate  # Windows
source venv/bin/activate  # Linux/Mac

# Run Streamlit app
streamlit run frontend/app.py
```

### Method 3: Start API Backend Only

```bash
python backend/main.py
```

Then visit: http://localhost:8000/docs

## ☐ Step 8: First Interview

Once the app is running (default: http://localhost:8501):

1. **Fill in sidebar form:**
   - [ ] Candidate Name: "Test Candidate"
   - [ ] Job Role: "Software Engineer"
   - [ ] Required Skills: "Python, API, Database"
   - [ ] Difficulty Level: "Intermediate"
   - [ ] Number of Questions: 3 (start small)

2. **Start Interview:**
   - [ ] Click "🚀 Start Interview"

3. **Generate Question:**
   - [ ] Click "📝 Next Question"
   - [ ] AI will generate and speak a question

4. **Record Answer:**
   - [ ] Click "🔴 Start Recording"
   - [ ] Speak your answer clearly
   - [ ] Wait for recording to complete

5. **View Results:**
   - [ ] See transcription
   - [ ] View AI evaluation
   - [ ] Check your score
   - [ ] Read feedback

6. **Continue:**
   - [ ] Click "Next Question" for more questions
   - [ ] Complete all questions
   - [ ] View final summary

## ☐ Step 9: Verify Database

Check that interviews are being saved:

```python
python

>>> from database import SessionLocal
>>> from models.database import Interview
>>> db = SessionLocal()
>>> interviews = db.query(Interview).all()
>>> for i in interviews:
...     print(f"{i.candidate_name}: {i.total_score}/10")
>>> db.close()
>>> exit()
```

## ☐ Step 10: Explore Features

### Try the Simple Version (No Audio)
```bash
streamlit run frontend/app_simple.py
```

### Test API Endpoints
Visit: http://localhost:8000/docs
- [ ] Try "Generate Question" endpoint
- [ ] Try "Evaluate Answer" endpoint
- [ ] Browse all API documentation

### Review Results
- [ ] Check `audio_recordings/` folder for saved audio
- [ ] Query database for interview records
- [ ] View session history

## ☐ Troubleshooting

### Issue: "GROQ_API_KEY not found"
**Solution:** 
- Verify .env file exists
- Check API key is set correctly
- Restart application after editing .env

### Issue: "Database connection failed"
**Solution:**
- For SQLite: Use `DATABASE_URL=sqlite:///./interview.db`
- For MySQL: Ensure database is created and running
- Verify username/password in DATABASE_URL

### Issue: "No audio devices found"
**Solution:**
- Check microphone is connected
- Run: `python scripts/utils.py list-devices`
- Try different USB port
- Check system audio settings

### Issue: "Whisper model download slow"
**Solution:**
- First run downloads model (~75MB for base model)
- Use smaller model: Set `WHISPER_MODEL=tiny` in settings.py
- Wait for download to complete
- Check internet connection

### Issue: "Module not found"
**Solution:**
- Activate virtual environment
- Re-run: `pip install -r requirements.txt`
- Check you're in correct directory

## ☐ Optional Enhancements

- [ ] Set up MySQL/PostgreSQL for production
- [ ] Configure custom audio settings
- [ ] Adjust TTS voice and speed
- [ ] Customize scoring criteria
- [ ] Add custom job roles and skills
- [ ] Create interview templates
- [ ] Set up analytics dashboard

## ☐ Production Deployment (Optional)

- [ ] Set DEBUG=False in .env
- [ ] Use production database
- [ ] Set up HTTPS
- [ ] Configure firewall
- [ ] Set up monitoring
- [ ] Regular backups
- [ ] Load balancing (if needed)

## 🎉 Success Criteria

You're ready when:
- ✅ Application starts without errors
- ✅ Can generate questions
- ✅ Can record audio
- ✅ Can transcribe speech
- ✅ Can evaluate answers
- ✅ Results save to database
- ✅ Can view interview history

## 📚 Next Steps

1. **Read Documentation:**
   - [ ] README.md - Overview
   - [ ] QUICKSTART.md - Quick guide
   - [ ] DEVELOPMENT.md - Advanced development
   - [ ] PROJECT_OVERVIEW.md - Architecture

2. **Customize:**
   - [ ] Adjust evaluation criteria
   - [ ] Add custom question types
   - [ ] Modify UI styling
   - [ ] Create custom reports

3. **Scale:**
   - [ ] Set up production database
   - [ ] Configure for multiple users
   - [ ] Add authentication
   - [ ] Deploy to cloud

## 🆘 Need Help?

- Check documentation files
- Review code comments
- Test individual components with utils.py
- Check API docs at /docs endpoint
- Review error logs

## ✨ Tips for Best Experience

1. **Audio Quality:**
   - Use good quality microphone
   - Minimize background noise
   - Speak clearly and at normal pace

2. **Question Quality:**
   - Be specific with skills
   - Choose appropriate difficulty
   - Match questions to actual job requirements

3. **Evaluation:**
   - Review AI feedback carefully
   - Adjust difficulty based on scores
   - Use consistent evaluation criteria

4. **Database:**
   - Regular backups
   - Monitor storage space
   - Clean old recordings periodically

---

**Congratulations!** 🎉 You now have a fully functional AI-powered voice interview system!

Start conducting intelligent interviews today! 🚀
