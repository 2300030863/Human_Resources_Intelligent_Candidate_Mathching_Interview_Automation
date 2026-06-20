# Development Guide

## Development Setup

### 1. Install Development Dependencies

```bash
pip install -r requirements.txt
pip install black flake8 pytest
```

### 2. Code Formatting

```bash
# Format all Python files
black .

# Check code style
flake8 .
```

### 3. Running Tests

```bash
# Run all tests
python scripts/utils.py all

# Run specific tests
python scripts/utils.py test-ai
python scripts/utils.py test-db
python scripts/utils.py check-env
```

## Project Architecture

### Services Layer

- **AIService**: Question generation and answer evaluation using Groq LLM
- **STTService**: Speech-to-text using OpenAI Whisper
- **TTSService**: Text-to-speech using pyttsx3 or gTTS
- **AudioRecorder**: Microphone recording using sounddevice

### Backend Layer (FastAPI)

- RESTful API endpoints
- Request/response validation with Pydantic
- Database operations with SQLAlchemy
- File upload handling

### Frontend Layer (Streamlit)

- Interactive UI for interviews
- Real-time audio recording
- Chat-style interface
- Progress tracking

### Database Layer

- SQLAlchemy ORM
- MySQL/PostgreSQL support
- Migration-ready schema

## Adding New Features

### Adding a New Question Type

1. Update `ai_service.py`:
```python
def generate_coding_question(self, language, difficulty):
    # Implementation
    pass
```

2. Update `schemas.py`:
```python
class CodingQuestionRequest(BaseModel):
    language: str
    difficulty: str
```

3. Add endpoint in `backend/main.py`:
```python
@app.post("/api/generate-coding-question")
async def generate_coding_question(request: CodingQuestionRequest):
    # Implementation
    pass
```

### Adding a New Evaluation Criteria

1. Update `config/settings.py`:
```python
SCORES = {
    "technical": 5,
    "clarity": 3,
    "communication": 2,
    "creativity": 2,  # New criteria
    "total": 12
}
```

2. Update database schema
3. Update AI evaluation prompt
4. Update UI to display new score

### Adding Multi-language Support

1. Update TTS service:
```python
def speak(self, text, language='en'):
    tts = gTTS(text=text, lang=language)
    # Implementation
```

2. Update Whisper configuration
3. Add language selector in UI

## API Development

### Creating New Endpoints

```python
@app.post("/api/your-endpoint")
async def your_endpoint(
    request: YourRequestSchema,
    db: Session = Depends(get_db)
):
    try:
        # Your logic here
        return {"success": True, "data": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
```

### Testing Endpoints

```bash
# Start backend
python backend/main.py

# Visit API docs
http://localhost:8000/docs

# Test with curl
curl -X POST http://localhost:8000/api/generate-question \
  -H "Content-Type: application/json" \
  -d '{"job_role": "Developer", "skills": "Python", "difficulty": "Basic"}'
```

## Database Development

### Adding a New Table

1. Create model in `models/database.py`:
```python
class NewTable(Base):
    __tablename__ = "new_table"
    id = Column(Integer, primary_key=True)
    # Add columns
```

2. Run migration:
```python
from database import init_database
init_database()
```

### Querying Data

```python
from database import SessionLocal
from models.database import Interview

db = SessionLocal()
interviews = db.query(Interview).filter(
    Interview.total_score >= 7
).all()
db.close()
```

## Frontend Development

### Custom Components

Create reusable components in `frontend/components/`:

```python
# frontend/components/score_card.py
import streamlit as st

def display_score_card(evaluation):
    col1, col2, col3 = st.columns(3)
    col1.metric("Technical", f"{evaluation['technical']}/5")
    col2.metric("Clarity", f"{evaluation['clarity']}/3")
    col3.metric("Communication", f"{evaluation['communication']}/2")
```

### Custom Styling

Add CSS in Streamlit:

```python
st.markdown("""
<style>
    .custom-class {
        color: blue;
    }
</style>
""", unsafe_allow_html=True)
```

## Performance Optimization

### Caching AI Responses

```python
from functools import lru_cache

@lru_cache(maxsize=100)
def get_cached_question(job_role, skills, difficulty):
    return ai_service.generate_question(job_role, skills, difficulty)
```

### Database Connection Pooling

```python
from sqlalchemy.pool import QueuePool

engine = create_engine(
    DATABASE_URL,
    poolclass=QueuePool,
    pool_size=10,
    max_overflow=20
)
```

### Async Operations

```python
import asyncio

async def process_multiple_interviews(interviews):
    tasks = [evaluate_interview(i) for i in interviews]
    results = await asyncio.gather(*tasks)
    return results
```

## Deployment

### Docker Deployment

Create `Dockerfile`:

```dockerfile
FROM python:3.9-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt

COPY . .

CMD ["streamlit", "run", "frontend/app.py"]
```

### Environment Variables

Production `.env`:

```env
GROQ_API_KEY=prod_key_here
DATABASE_URL=mysql+pymysql://user:pass@db-host:3306/interview_system
DEBUG=False
TTS_ENGINE=gtts
WHISPER_MODEL=base
```

## Security Best Practices

1. Never commit `.env` file
2. Use environment variables for secrets
3. Validate all user inputs
4. Implement rate limiting
5. Use HTTPS in production
6. Encrypt audio files
7. Implement authentication
8. Regular security audits

## Troubleshooting Development Issues

### Import Errors

```bash
# Ensure you're in project root
cd c:\project\interview

# Activate virtual environment
venv\Scripts\activate

# Verify installation
pip list
```

### Database Issues

```python
# Reset database
from database import init_database
from models.database import Base
from database.connection import engine

Base.metadata.drop_all(bind=engine)
init_database()
```

### Audio Issues

```python
# List available devices
python scripts/utils.py list-devices

# Test audio
python scripts/utils.py test-audio
```

## Contributing Guidelines

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Make changes
4. Format code: `black .`
5. Run tests: `python scripts/utils.py all`
6. Commit: `git commit -m 'Add amazing feature'`
7. Push: `git push origin feature/amazing-feature`
8. Create Pull Request

## Resources

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Streamlit Documentation](https://docs.streamlit.io/)
- [SQLAlchemy Documentation](https://docs.sqlalchemy.org/)
- [Groq API Docs](https://console.groq.com/docs)
- [Whisper Documentation](https://github.com/openai/whisper)

---

Happy Coding! 🚀
