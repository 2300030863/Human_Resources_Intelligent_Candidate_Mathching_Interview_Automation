"""
Utility scripts for the AI Voice Interview System
"""
import sys
from pathlib import Path

# Add project root to path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))


def test_ai_service():
    """Test AI service for question generation and evaluation"""
    from services import get_ai_service
    
    print("Testing AI Service...")
    ai_service = get_ai_service()
    
    # Test question generation
    print("\n1. Generating question...")
    question = ai_service.generate_question(
        job_role="Software Engineer",
        skills="Python, FastAPI, PostgreSQL",
        difficulty="Intermediate"
    )
    print(f"Question: {question}")
    
    # Test answer evaluation
    print("\n2. Evaluating answer...")
    answer = "FastAPI is a modern Python web framework for building APIs. It's fast, supports async/await, has automatic API documentation, and includes built-in validation using Pydantic."
    
    evaluation = ai_service.evaluate_answer(question, answer)
    print(f"Technical: {evaluation['technical']}/5")
    print(f"Clarity: {evaluation['clarity']}/3")
    print(f"Communication: {evaluation['communication']}/2")
    print(f"Total: {evaluation['total']}/10")
    print(f"Feedback: {evaluation['feedback']}")
    
    print("\n✅ AI Service test completed!")


def test_database():
    """Test database connection and operations"""
    from database import SessionLocal, init_database
    from models.database import Interview
    import uuid
    
    print("Testing Database...")
    
    # Initialize database
    print("\n1. Initializing database...")
    init_database()
    
    # Create test record
    print("\n2. Creating test interview record...")
    db = SessionLocal()
    
    test_interview = Interview(
        candidate_name=f"Test_User_{uuid.uuid4().hex[:6]}",
        job_role="Software Engineer",
        question="What is your experience with Python?",
        answer_transcript="I have 5 years of experience with Python...",
        technical_score=4,
        clarity_score=3,
        communication_score=2,
        total_score=9,
        feedback="Great answer with good examples!"
    )
    
    db.add(test_interview)
    db.commit()
    db.refresh(test_interview)
    
    print(f"Created interview record with ID: {test_interview.id}")
    
    # Query records
    print("\n3. Querying interview records...")
    interviews = db.query(Interview).order_by(Interview.created_at.desc()).limit(5).all()
    
    print(f"\nLast 5 interviews:")
    for interview in interviews:
        print(f"  - {interview.candidate_name}: {interview.total_score}/10 ({interview.job_role})")
    
    db.close()
    print("\n✅ Database test completed!")


def test_audio():
    """Test audio recording and TTS"""
    from services import get_tts_service
    
    print("Testing Audio Services...")
    
    # Test TTS
    print("\n1. Testing Text-to-Speech...")
    tts_service = get_tts_service()
    
    test_text = "Hello! This is a test of the AI Voice Interview System."
    print(f"Speaking: '{test_text}'")
    
    success = tts_service.speak(test_text)
    
    if success:
        print("✅ TTS test passed!")
    else:
        print("❌ TTS test failed!")
    
    print("\n✅ Audio test completed!")


def list_audio_devices():
    """List available audio input devices"""
    from services import get_audio_recorder
    
    print("Available Audio Input Devices:")
    recorder = get_audio_recorder()
    devices = recorder.get_audio_devices()
    
    for device in devices:
        print(f"\nDevice ID: {device['id']}")
        print(f"  Name: {device['name']}")
        print(f"  Channels: {device['channels']}")
        print(f"  Sample Rate: {device['sample_rate']}")


def check_environment():
    """Check if environment is properly configured"""
    import os
    from pathlib import Path
    
    print("Checking Environment Configuration...\n")
    
    # Check .env file
    env_file = Path(".env")
    if env_file.exists():
        print("✅ .env file found")
    else:
        print("❌ .env file not found")
        print("   Run: copy .env.example .env")
    
    # Check API keys
    groq_key = os.getenv("GROQ_API_KEY")
    if groq_key and len(groq_key) > 10:
        print("✅ GROQ_API_KEY configured")
    else:
        print("❌ GROQ_API_KEY not configured")
        print("   Add your Groq API key to .env file")
    
    # Check database URL
    db_url = os.getenv("DATABASE_URL")
    if db_url:
        print("✅ DATABASE_URL configured")
    else:
        print("❌ DATABASE_URL not configured")
    
    # Check audio recordings directory
    from config.settings import AUDIO_STORAGE_PATH
    if AUDIO_STORAGE_PATH.exists():
        print(f"✅ Audio storage directory exists: {AUDIO_STORAGE_PATH}")
    else:
        print(f"❌ Audio storage directory not found: {AUDIO_STORAGE_PATH}")
        AUDIO_STORAGE_PATH.mkdir(parents=True, exist_ok=True)
        print(f"   Created directory: {AUDIO_STORAGE_PATH}")
    
    print("\n✅ Environment check completed!")


def run_all_tests():
    """Run all tests"""
    print("=" * 60)
    print("Running All Tests for AI Voice Interview System")
    print("=" * 60)
    
    try:
        check_environment()
        print("\n" + "=" * 60 + "\n")
        
        test_database()
        print("\n" + "=" * 60 + "\n")
        
        test_ai_service()
        print("\n" + "=" * 60 + "\n")
        
        # Skip audio test in automated testing
        # test_audio()
        
        print("\n" + "=" * 60)
        print("All Tests Completed!")
        print("=" * 60)
        
    except Exception as e:
        print(f"\n❌ Error during testing: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1:
        command = sys.argv[1]
        
        if command == "test-ai":
            test_ai_service()
        elif command == "test-db":
            test_database()
        elif command == "test-audio":
            test_audio()
        elif command == "list-devices":
            list_audio_devices()
        elif command == "check-env":
            check_environment()
        elif command == "all":
            run_all_tests()
        else:
            print(f"Unknown command: {command}")
            print("\nAvailable commands:")
            print("  test-ai       - Test AI service")
            print("  test-db       - Test database")
            print("  test-audio    - Test audio services")
            print("  list-devices  - List audio devices")
            print("  check-env     - Check environment")
            print("  all           - Run all tests")
    else:
        print("AI Voice Interview System - Utility Scripts")
        print("\nUsage: python scripts/utils.py <command>")
        print("\nAvailable commands:")
        print("  test-ai       - Test AI service")
        print("  test-db       - Test database")
        print("  test-audio    - Test audio services")
        print("  list-devices  - List audio devices")
        print("  check-env     - Check environment")
        print("  all           - Run all tests")
