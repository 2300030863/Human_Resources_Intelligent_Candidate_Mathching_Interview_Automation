# AI Voice Interview System

An AI-powered voice interview system that generates interview questions, records candidate responses, transcribes speech to text, and automatically evaluates answers using advanced AI models.

## 🎯 Features

- **AI Question Generation**: Generate dynamic interview questions based on job role, skills, and difficulty level
- **Voice Recording**: Record candidate answers via microphone
- **Speech-to-Text**: Convert voice responses to text using OpenAI Whisper
- **Text-to-Speech**: AI speaks questions aloud
- **AI Evaluation**: Automatically evaluate answers with detailed feedback
- **Scoring System**: Scores based on technical accuracy, clarity, and communication
- **Database Storage**: Store interview results for analysis
- **Chat Interface**: Interactive interview experience
- **FastAPI Backend**: RESTful API for all operations
- **Streamlit Frontend**: User-friendly web interface

## 🏗️ Architecture

```
User Interface (Streamlit)
        ↓
Python Backend (FastAPI)
        ↓
AI Services Layer
   ├── Question Generator (Groq)
   ├── Evaluator (Groq)
   ├── Speech-to-Text (Whisper)
   └── Text-to-Speech (pyttsx3/gTTS)
        ↓
Database (MySQL/PostgreSQL)
        ↓
Storage (Audio Files)
```

## 📋 Prerequisites

- Python 3.9 or higher
- MySQL or PostgreSQL database
- Microphone for voice recording
- Groq API key (for AI services)

## 🚀 Installation

### 1. Clone the Repository

```bash
cd c:\project\interview
```

### 2. Create Virtual Environment

```bash
python -m venv venv
venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac
```

### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

### 4. Configure Environment Variables

Create a `.env` file in the root directory:

```bash
cp .env.example .env
```

Edit `.env` and add your configuration:

```env
# AI API Keys
GROQ_API_KEY=your_groq_api_key_here
OPENAI_API_KEY=your_openai_api_key_here

# Database Configuration
DATABASE_URL=mysql+pymysql://username:password@localhost:3306/interview_system

# Application Settings
DEBUG=True
```

### 5. Initialize Database

```bash
python -m database.connection
```

## 💻 Usage

### Option 1: Run Streamlit App (Recommended)

```bash
streamlit run frontend/app.py
```

The app will open in your browser at `http://localhost:8501`

### Option 2: Run FastAPI Backend Only

```bash
python backend/main.py
```

API will be available at `http://localhost:8000`
API Documentation: `http://localhost:8000/docs`

### Option 3: Run Simple Version

```bash
streamlit run frontend/app_simple.py
```

## 📖 How to Use

### Starting an Interview

1. Open the Streamlit app
2. Fill in the sidebar form:
   - Candidate Name
   - Job Role
   - Required Skills
   - Difficulty Level
   - Number of Questions
3. Click "Start Interview"

### Conducting the Interview

1. Click "Next Question" to generate a question
2. AI will speak the question
3. Click "Start Recording" to record your answer
4. Speak your answer clearly
5. Recording stops automatically
6. AI transcribes and evaluates your answer
7. View scores and feedback
8. Continue to next question

### Evaluation Criteria

- **Technical Accuracy** (0-5 points): Correctness and depth of knowledge
- **Clarity** (0-3 points): Clear and structured explanation
- **Communication** (0-2 points): Effective communication skills
- **Total Score** (0-10 points)

## 🗄️ Database Schema

### interviews Table

| Field | Type | Description |
|-------|------|-------------|
| id | int | Primary key |
| candidate_name | varchar | Candidate's name |
| job_role | varchar | Target job role |
| question | text | Interview question |
| answer_transcript | text | Transcribed answer |
| technical_score | int | Technical accuracy score |
| clarity_score | int | Clarity score |
| communication_score | int | Communication score |
| total_score | int | Total score |
| feedback | text | AI-generated feedback |
| audio_file_path | varchar | Path to audio recording |
| created_at | timestamp | Record creation time |

### interview_sessions Table

| Field | Type | Description |
|-------|------|-------------|
| id | int | Primary key |
| session_id | varchar | Unique session ID |
| candidate_name | varchar | Candidate's name |
| job_role | varchar | Target job role |
| skills | text | Required skills |
| difficulty_level | varchar | Question difficulty |
| total_questions | int | Total questions planned |
| questions_answered | int | Questions completed |
| average_score | float | Average score |
| status | varchar | Session status |
| started_at | timestamp | Session start time |
| completed_at | timestamp | Session completion time |

## 🔌 API Endpoints

### Generate Question
```
POST /api/generate-question
{
  "job_role": "Software Engineer",
  "skills": "Python, FastAPI, SQL",
  "difficulty": "Intermediate"
}
```

### Evaluate Answer
```
POST /api/evaluate-answer
{
  "question": "What is REST API?",
  "answer": "REST API is..."
}
```

### Transcribe Audio
```
POST /api/transcribe-audio
(multipart/form-data with audio file)
```

### Create Interview Record
```
POST /api/interviews
{
  "candidate_name": "John Doe",
  "job_role": "Developer",
  ...
}
```

### Get Interviews
```
GET /api/interviews?candidate_name=John&limit=10
```

## 📁 Project Structure

```
interview/
├── backend/              # FastAPI backend
│   ├── main.py          # API endpoints
│   └── schemas.py       # Pydantic models
├── frontend/            # Streamlit frontend
│   ├── app.py          # Main app
│   └── app_simple.py   # Simple version
├── services/            # Core services
│   ├── ai_service.py   # AI question & evaluation
│   ├── stt_service.py  # Speech-to-text
│   ├── tts_service.py  # Text-to-speech
│   └── audio_recorder.py
├── models/              # Database models
│   └── database.py
├── database/            # Database config
│   └── connection.py
├── config/              # Configuration
│   └── settings.py
├── audio_recordings/    # Audio storage
├── requirements.txt     # Dependencies
├── .env.example        # Environment template
└── README.md           # This file
```

## 🎤 Troubleshooting

### Audio Issues

**No microphone detected:**
```python
from services import get_audio_recorder
recorder = get_audio_recorder()
devices = recorder.get_audio_devices()
print(devices)
```

**Change microphone:**
```python
recorder.set_device(device_id)
```

### Database Issues

**Cannot connect to database:**
1. Ensure MySQL/PostgreSQL is running
2. Check DATABASE_URL in `.env`
3. Verify database credentials

**Create database manually:**
```sql
CREATE DATABASE interview_system;
```

### Whisper Model Issues

**Model download slow:**
- First run downloads Whisper model
- Use smaller model: `WHISPER_MODEL=tiny` in settings

**Out of memory:**
- Use smaller Whisper model (tiny, base)
- Or use OpenAI API instead

## 🔒 Security Notes

- Never commit `.env` file
- Keep API keys secure
- Encrypt audio files in production
- Use HTTPS in production
- Implement authentication for API

## 🚀 Advanced Features (Phase 2)

- Smart difficulty adaptation
- Anti-cheat detection
- Emotion analysis
- Multi-language support
- Video recording
- Resume-based questions
- Automatic candidate shortlisting

## 📊 Success Metrics

- Candidate completion rate
- Average score distribution
- AI evaluation accuracy
- User satisfaction rating

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## 📝 License

MIT License - feel free to use for personal or commercial projects

## 👥 Support

For issues or questions:
- Create GitHub issue
- Check documentation
- Review API docs at `/docs`

## 🎓 Credits

Built with:
- FastAPI
- Streamlit
- Groq AI
- OpenAI Whisper
- SQLAlchemy

---

**Version:** 1.0.0  
**Last Updated:** February 2026  
**Status:** Production Ready ✅
