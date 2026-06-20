"""
Text-to-Speech service using pyttsx3 (offline) and gTTS (online)
"""
import pyttsx3
from gtts import gTTS
from pathlib import Path
import tempfile
from config.settings import TTS_ENGINE, TTS_RATE, TTS_VOLUME


class TTSService:
    """Text-to-Speech service"""
    
    def __init__(self, engine_type: str = TTS_ENGINE):
        """
        Initialize TTS engine
        
        Args:
            engine_type: TTS engine to use (pyttsx3 or gtts)
        """
        self.engine_type = engine_type
        
        if engine_type == "pyttsx3":
            self.engine = pyttsx3.init()
            self.engine.setProperty('rate', TTS_RATE)
            self.engine.setProperty('volume', TTS_VOLUME)
            
            # Set voice (try to use a clear voice)
            voices = self.engine.getProperty('voices')
            if voices:
                # Prefer female voice if available (usually clearer)
                for voice in voices:
                    if 'female' in voice.name.lower() or 'zira' in voice.name.lower():
                        self.engine.setProperty('voice', voice.id)
                        break
    
    def speak(self, text: str) -> bool:
        """
        Speak text using TTS
        
        Args:
            text: Text to speak
            
        Returns:
            Success status
        """
        try:
            if self.engine_type == "pyttsx3":
                self.engine.say(text)
                self.engine.runAndWait()
                return True
            
            elif self.engine_type == "gtts":
                # Generate speech and play it
                tts = gTTS(text=text, lang='en', slow=False)
                
                # Save to temporary file and play
                with tempfile.NamedTemporaryFile(delete=False, suffix='.mp3') as fp:
                    temp_file = fp.name
                    tts.save(temp_file)
                
                # Play the audio (platform-specific)
                import os
                import platform
                
                if platform.system() == 'Windows':
                    os.system(f'start {temp_file}')
                elif platform.system() == 'Darwin':  # macOS
                    os.system(f'afplay {temp_file}')
                else:  # Linux
                    os.system(f'mpg123 {temp_file}')
                
                return True
            
            return False
            
        except Exception as e:
            print(f"Error in TTS: {e}")
            return False
    
    def save_audio(self, text: str, output_path: str) -> bool:
        """
        Save speech to audio file
        
        Args:
            text: Text to convert
            output_path: Path to save audio file
            
        Returns:
            Success status
        """
        try:
            if self.engine_type == "pyttsx3":
                self.engine.save_to_file(text, output_path)
                self.engine.runAndWait()
                return True
            
            elif self.engine_type == "gtts":
                tts = gTTS(text=text, lang='en', slow=False)
                tts.save(output_path)
                return True
            
            return False
            
        except Exception as e:
            print(f"Error saving TTS audio: {e}")
            return False


# Singleton instance
_tts_service = None

def get_tts_service() -> TTSService:
    """Get TTS service singleton instance"""
    global _tts_service
    if _tts_service is None:
        _tts_service = TTSService()
    return _tts_service
