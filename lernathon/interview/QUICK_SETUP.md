# Quick Setup - AI Interview System

## Get GROQ API Key (FREE)

1. Visit: https://console.groq.com/
2. Click "Sign Up" or "Sign In"
3. Once logged in, go to "API Keys" section
4. Click "Create API Key"
5. Copy the key (starts with `gsk_...`)

## Setup

1. **Create `.env` file** in the `interview` folder:
```env
GROQ_API_KEY=gsk_your_actual_key_here
```

2. **Install dependencies** (if not already done):
```bash
cd interview
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

3. **Start the system**:
```bash
cd ..
.\start-interview-system.bat
```

## Access

- **Frontend (Interview UI):** http://localhost:8501
- **Backend (API):** http://localhost:8000

## Integration

The interview system is integrated with the main recruitment platform:

1. Candidate passes exam (≥60%)
2. Interview automatically scheduled
3. Click "Start AI Interview" button
4. Interview opens with AI-powered questions
5. Results saved automatically

See [AI_INTERVIEW_INTEGRATION.md](../AI_INTERVIEW_INTEGRATION.md) for complete documentation.
