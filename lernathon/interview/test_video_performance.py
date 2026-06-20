"""
Test to verify question generation is not affected by video refresh
"""
import sys
sys.path.append('.')

from services.ai_service import AIService
import time

def test_generation_speed():
    """Test question generation speed"""
    ai_service = AIService()
    
    print("Testing question generation speed...")
    print("-" * 50)
    
    # Test 5 times
    times = []
    for i in range(5):
        start = time.time()
        question = ai_service.generate_question(
            job_role="Software Engineer",
            skills="Python, JavaScript",
            difficulty="medium",
            previous_questions=[]
        )
        elapsed = time.time() - start
        times.append(elapsed)
        print(f"Test {i+1}: {elapsed:.2f}s - Question generated: {len(question)} chars")
    
    print("-" * 50)
    print(f"Average generation time: {sum(times)/len(times):.2f}s")
    print(f"Fastest: {min(times):.2f}s")
    print(f"Slowest: {max(times):.2f}s")
    print("\n✓ Generation should be consistently fast (< 1s)")

if __name__ == "__main__":
    test_generation_speed()
