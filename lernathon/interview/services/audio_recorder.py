"""
Audio recording service using sounddevice
"""
import sounddevice as sd
import soundfile as sf
import numpy as np
from pathlib import Path
from datetime import datetime
from config.settings import AUDIO_SAMPLE_RATE, AUDIO_CHANNELS, AUDIO_MAX_DURATION, AUDIO_STORAGE_PATH


class AudioRecorder:
    """Audio recording service"""
    
    def __init__(self):
        self.sample_rate = AUDIO_SAMPLE_RATE
        self.channels = AUDIO_CHANNELS
        self.max_duration = AUDIO_MAX_DURATION
        self.recording = None
        self.is_recording = False
    
    def record_audio(self, duration: int = None) -> np.ndarray:
        """
        Record audio for specified duration
        
        Args:
            duration: Recording duration in seconds (default: max duration)
            
        Returns:
            Recorded audio as numpy array
        """
        if duration is None:
            duration = self.max_duration
        
        try:
            print(f"Recording for {duration} seconds...")
            self.is_recording = True
            
            # Record audio
            recording = sd.rec(
                int(duration * self.sample_rate),
                samplerate=self.sample_rate,
                channels=self.channels,
                dtype='float32'
            )
            
            sd.wait()  # Wait until recording is finished
            self.is_recording = False
            
            print("Recording finished!")
            return recording
            
        except Exception as e:
            print(f"Error recording audio: {e}")
            self.is_recording = False
            return None
    
    def save_recording(self, audio_data: np.ndarray, filename: str = None) -> str:
        """
        Save audio recording to file
        
        Args:
            audio_data: Audio data as numpy array
            filename: Output filename (auto-generated if None)
            
        Returns:
            Path to saved file
        """
        try:
            if filename is None:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                filename = f"recording_{timestamp}.wav"
            
            # Ensure .wav extension
            if not filename.endswith('.wav'):
                filename += '.wav'
            
            output_path = AUDIO_STORAGE_PATH / filename
            
            # Save audio file
            sf.write(
                str(output_path),
                audio_data,
                self.sample_rate
            )
            
            print(f"Audio saved to: {output_path}")
            return str(output_path)
            
        except Exception as e:
            print(f"Error saving audio: {e}")
            return None
    
    def record_and_save(self, duration: int = None, filename: str = None) -> str:
        """
        Record audio and save to file
        
        Args:
            duration: Recording duration in seconds
            filename: Output filename
            
        Returns:
            Path to saved file
        """
        audio_data = self.record_audio(duration)
        
        if audio_data is not None:
            return self.save_recording(audio_data, filename)
        
        return None
    
    def get_audio_devices(self) -> list:
        """Get list of available audio input devices"""
        try:
            devices = sd.query_devices()
            input_devices = []
            
            for i, device in enumerate(devices):
                if device['max_input_channels'] > 0:
                    input_devices.append({
                        'id': i,
                        'name': device['name'],
                        'channels': device['max_input_channels'],
                        'sample_rate': device['default_samplerate']
                    })
            
            return input_devices
            
        except Exception as e:
            print(f"Error getting audio devices: {e}")
            return []
    
    def set_device(self, device_id: int):
        """Set audio input device"""
        try:
            sd.default.device = device_id
            print(f"Audio input device set to: {device_id}")
            
        except Exception as e:
            print(f"Error setting audio device: {e}")


# Singleton instance
_audio_recorder = None

def get_audio_recorder() -> AudioRecorder:
    """Get audio recorder singleton instance"""
    global _audio_recorder
    if _audio_recorder is None:
        _audio_recorder = AudioRecorder()
    return _audio_recorder
