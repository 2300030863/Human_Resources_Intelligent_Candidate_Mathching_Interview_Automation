"""
Configuration settings for AI Voice Interview System
"""
import os
from pathlib import Path
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Base directory
BASE_DIR = Path(__file__).resolve().parent.parent

# API Keys
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")

# Database Configuration - Connect to recruit_db (same as Java backend)
DATABASE_URL = os.getenv(
    "DATABASE_URL", 
    "mysql+pymysql://root:12345@localhost:3306/recruit_db"
)

# Audio Configuration
AUDIO_SAMPLE_RATE = 16000
AUDIO_CHANNELS = 1
AUDIO_MAX_DURATION = 60  # seconds
AUDIO_STORAGE_PATH = BASE_DIR / "audio_recordings"

# Interview Configuration
MAX_QUESTIONS_PER_INTERVIEW = 10
QUESTION_DIFFICULTY_LEVELS = ["Basic", "Intermediate", "Advanced"]

# Scoring Configuration
SCORES = {
    "technical": 5,
    "clarity": 3,
    "communication": 2,
    "total": 10
}

# TTS Configuration
TTS_ENGINE = "pyttsx3"  # Options: pyttsx3, gtts, elevenlabs
TTS_RATE = 150  # Words per minute
TTS_VOLUME = 0.9

# STT Configuration
WHISPER_MODEL = "base"  # Options: tiny, base, small, medium, large

# Application Settings
APP_NAME = "AI Voice Interview System"
APP_VERSION = "1.0.0"
DEBUG = os.getenv("DEBUG", "True").lower() == "true"

# Create necessary directories
AUDIO_STORAGE_PATH.mkdir(parents=True, exist_ok=True)
