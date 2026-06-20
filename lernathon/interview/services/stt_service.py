"""
Speech-to-Text service using Groq Whisper API (free tier)
"""
import os
import tempfile
import numpy as np
from pathlib import Path
from openai import OpenAI
from config.settings import GROQ_API_KEY, OPENAI_API_KEY


class STTService:
    """Speech-to-Text service using Groq Whisper API"""
    
    def __init__(self):
        """Initialize Groq client (OpenAI-compatible)"""
        groq_key = GROQ_API_KEY or os.getenv("GROQ_API_KEY", "")
        openai_key = OPENAI_API_KEY or os.getenv("OPENAI_API_KEY", "")
        
        if groq_key:
            print("Initializing STT service with Groq Whisper API...")
            self.client = OpenAI(
                api_key=groq_key,
                base_url="https://api.groq.com/openai/v1"
            )
            self.model = "whisper-large-v3-turbo"
        else:
            print("Initializing STT service with OpenAI Whisper API...")
            self.client = OpenAI(api_key=openai_key)
            self.model = "whisper-1"
        print("STT service ready!")
    
    def transcribe_audio(self, audio_path: str) -> dict:
        """
        Transcribe audio file to text
        
        Args:
            audio_path: Path to audio file (WAV, WebM, MP3, Ogg, etc.)
            
        Returns:
            Dictionary with transcript and metadata
        """
        try:
            audio_path = Path(audio_path)
            
            if not audio_path.exists():
                return {
                    "success": False,
                    "transcript": "",
                    "error": "Audio file not found"
                }
            
            with open(audio_path, "rb") as f:
                response = self.client.audio.transcriptions.create(
                    model=self.model,
                    file=(audio_path.name, f, "audio/webm"),
                    response_format="json"
                )
            
            return {
                "success": True,
                "transcript": response.text.strip(),
                "language": "en",
                "error": None
            }
            
        except Exception as e:
            print(f"Error transcribing audio: {e}")
            return {
                "success": False,
                "transcript": "",
                "error": str(e)
            }
    
    def transcribe_from_array(self, audio_data: np.ndarray, sample_rate: int = 16000) -> dict:
        """
        Transcribe audio from numpy array
        
        Args:
            audio_data: Audio data as numpy array (float32, range [-1, 1])
            sample_rate: Sample rate of audio
            
        Returns:
            Dictionary with transcript and metadata
        """
        try:
            import wave
            
            # Ensure 1D float32 array
            if audio_data.ndim > 1:
                audio_data = audio_data.flatten()
            if audio_data.dtype != np.float32:
                audio_data = audio_data.astype(np.float32)
            
            # Normalize
            max_val = np.abs(audio_data).max()
            if max_val > 1.0:
                audio_data = audio_data / max_val
            audio_data = np.clip(audio_data, -1.0, 1.0)
            
            # Convert float32 -> int16 PCM
            pcm_data = (audio_data * 32767).astype(np.int16)
            
            # Write to temp WAV file
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
                tmp_path = tmp.name
            
            with wave.open(tmp_path, "wb") as wf:
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(sample_rate)
                wf.writeframes(pcm_data.tobytes())
            
            result = self.transcribe_audio(tmp_path)
            
            try:
                os.unlink(tmp_path)
            except Exception:
                pass
            
            return result
            
        except Exception as e:
            print(f"Error transcribing audio array: {e}")
            return {
                "success": False,
                "transcript": "",
                "error": str(e)
            }


# Singleton instance
_stt_service = None

def get_stt_service() -> STTService:
    """Get STT service singleton instance"""
    global _stt_service
    if _stt_service is None:
        _stt_service = STTService()
    return _stt_service
