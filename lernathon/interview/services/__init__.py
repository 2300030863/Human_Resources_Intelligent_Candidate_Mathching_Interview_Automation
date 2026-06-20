from .ai_service import AIService, get_ai_service
from .candidate_service import CandidateService, get_candidate_service

# Optional audio services - handle gracefully if dependencies are missing
try:
    from .stt_service import STTService, get_stt_service
except ImportError:
    STTService = None
    get_stt_service = lambda: None

try:
    from .tts_service import TTSService, get_tts_service
except ImportError:
    TTSService = None
    get_tts_service = lambda: None

try:
    from .audio_recorder import AudioRecorder, get_audio_recorder
except ImportError:
    AudioRecorder = None
    get_audio_recorder = lambda: None

