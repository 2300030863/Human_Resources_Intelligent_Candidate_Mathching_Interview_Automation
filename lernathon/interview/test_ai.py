"""
Quick test for AI service
"""
import sys
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent))

from services.ai_service import get_ai_service

def test_question_generation():
    """Test if question generation works"""
    print("Testing AI Service...")
    print("-" * 50)
    
    try:
        ai_service = get_ai_service()
        print("✓ AI Service initialized")
        
        # Test question generation
        print("\nGenerating question...")
        question = ai_service.generate_question(
            job_role="Software Engineer",
            skills="Python, JavaScript",
            difficulty="medium",
            previous_questions=[]
        )
        
        print(f"\n✓ Question generated successfully:")
        print(f"  {question}")
        
        if question and len(question.strip()) > 0 and "error" not in question.lower():
            print("\n✅ AI Service is working correctly!")
            return True
        else:
            print("\n❌ AI Service returned invalid question")
            return False
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    test_question_generation()
