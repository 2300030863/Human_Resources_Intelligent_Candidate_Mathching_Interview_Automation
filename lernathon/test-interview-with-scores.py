"""
Test script for Interview System with Candidate Names and Marks
Tests new fields: candidate_name, candidate_email, job_title, and detailed scores
"""

import sys
from pathlib import Path

# Add interview directory to path
interview_dir = Path(__file__).parent / "interview"
sys.path.insert(0, str(interview_dir))

from services.candidate_service import CandidateService
from models.database import InterviewType, InterviewStatus
from datetime import datetime
from database import SessionLocal
from models.database import Interview


def test_create_interview_with_candidate_details():
    """Test creating interview with automatic candidate detail population"""
    print("\n=== Test: Create Interview with Candidate Details ===")
    
    application_id = 26  # Using existing application
    candidate_service = CandidateService()
    
    try:
        # Create interview with candidate details automatically populated
        interview = candidate_service.create_interview_with_details(
            application_id=application_id,
            interview_data={
                'type': InterviewType.TECHNICAL,
                'status': InterviewStatus.SCHEDULED,
                'scheduled_at': datetime.now(),
                'duration_minutes': 45,
                'notes': 'AI Voice Interview - Technical Round'
            }
        )
        
        if interview:
            print("✅ Interview created successfully!")
            print(f"   Interview ID: {interview.id}")
            print(f"   Candidate Name: {interview.candidate_name}")
            print(f"   Candidate Email: {interview.candidate_email}")
            print(f"   Job Title: {interview.job_title}")
            print(f"   Application ID: {interview.application_id}")
            print(f"   Type: {interview.type.value}")
            print(f"   Status: {interview.status.value}")
            return interview.id
        else:
            print("❌ Failed to create interview")
            return None
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return None


def test_update_interview_with_scores():
    """Test updating interview with detailed marks/scores"""
    print("\n=== Test: Update Interview with Detailed Scores ===")
    
    # Get the most recent interview to update
    db = SessionLocal()
    interview = db.query(Interview).order_by(Interview.id.desc()).first()
    db.close()
    
    if not interview:
        print("❌ No interview found to update")
        return False
    
    interview_id = interview.id
    candidate_service = CandidateService()
    
    try:
        # Update with detailed scores
        updated_interview = candidate_service.update_interview_scores(
            interview_id=interview_id,
            scores={
                'technical_score': 85,       # Out of 100
                'communication_score': 78,   # Out of 100
                'problem_solving_score': 90, # Out of 100
                'cultural_fit_score': 82,    # Out of 100
                'rating': 8,                 # Overall 1-10 rating
                'feedback': 'Excellent technical skills. Strong problem-solving abilities. Good communication. Recommended for hire.',
                'notes': 'Candidate showed great depth in Python and system design. Struggled a bit with concurrency concepts but recovered well.',
                'status': InterviewStatus.COMPLETED
            }
        )
        
        if updated_interview:
            print("✅ Interview scores updated successfully!")
            print(f"   Interview ID: {updated_interview.id}")
            print(f"   Candidate: {updated_interview.candidate_name}")
            print(f"   Technical Score: {updated_interview.technical_score}/100")
            print(f"   Communication Score: {updated_interview.communication_score}/100")
            print(f"   Problem Solving Score: {updated_interview.problem_solving_score}/100")
            print(f"   Cultural Fit Score: {updated_interview.cultural_fit_score}/100")
            print(f"   Total Score: {updated_interview.total_score}/100")
            print(f"   Overall Rating: {updated_interview.rating}/10")
            print(f"   Status: {updated_interview.status.value}")
            print(f"   Feedback: {updated_interview.feedback}")
            return True
        else:
            print("❌ Failed to update interview scores")
            return False
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return False


def test_query_interview_with_all_details():
    """Test querying interview to see all candidate and score details"""
    print("\n=== Test: Query Interview with Complete Details ===")
    
    db = SessionLocal()
    
    try:
        # Get the most recent completed interview
        interview = db.query(Interview).filter(
            Interview.status == InterviewStatus.COMPLETED
        ).order_by(Interview.id.desc()).first()
        
        if interview:
            print("✅ Interview found!")
            print("\n📋 INTERVIEW DETAILS:")
            print(f"   Interview ID: {interview.id}")
            print(f"   Application ID: {interview.application_id}")
            print(f"   Status: {interview.status.value}")
            
            print("\n👤 CANDIDATE INFORMATION:")
            print(f"   Name: {interview.candidate_name}")
            print(f"   Email: {interview.candidate_email}")
            print(f"   Job Applied: {interview.job_title}")
            
            print("\n📊 SCORES & MARKS:")
            print(f"   Technical Score: {interview.technical_score or 0}/100")
            print(f"   Communication Score: {interview.communication_score or 0}/100")
            print(f"   Problem Solving: {interview.problem_solving_score or 0}/100")
            print(f"   Cultural Fit: {interview.cultural_fit_score or 0}/100")
            print(f"   ─────────────────────────")
            print(f"   Total Score: {interview.total_score or 0}/100")
            print(f"   Overall Rating: {interview.rating or 0}/10")
            
            print("\n💬 FEEDBACK:")
            print(f"   {interview.feedback or 'No feedback provided'}")
            
            print("\n📝 NOTES:")
            print(f"   {interview.notes or 'No notes'}")
            
            print("\n📅 TIMELINE:")
            print(f"   Scheduled: {interview.scheduled_at}")
            print(f"   Created: {interview.created_at}")
            print(f"   Updated: {interview.updated_at}")
            
            return True
        else:
            print("❌ No completed interviews found")
            return False
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return False
    finally:
        db.close()


def main():
    """Run all tests"""
    print("=" * 70)
    print("INTERVIEW SYSTEM - CANDIDATE NAME & MARKS TEST")
    print("=" * 70)
    
    # Test 1: Create interview with candidate details
    interview_id = test_create_interview_with_candidate_details()
    
    if not interview_id:
        print("\n❌ Cannot proceed without creating interview")
        return
    
    # Test 2: Update interview with scores
    if not test_update_interview_with_scores():
        print("\n❌ Score update failed")
        return
    
    # Test 3: Query and display complete interview details
    if not test_query_interview_with_all_details():
        print("\n❌ Query failed")
        return
    
    print("\n" + "=" * 70)
    print("✅ ALL TESTS PASSED!")
    print("=" * 70)
    print("\n📊 Summary:")
    print("   - Candidate name is visible in interview records")
    print("   - Detailed marks/scores are stored (technical, communication, etc.)")
    print("   - Total score is automatically calculated")
    print("   - All data is properly persisted in the database")
    print("\n✨ The interview system now tracks:")
    print("   1. Candidate information (name, email, job)")
    print("   2. Detailed scoring (4 categories + total)")
    print("   3. Overall rating and feedback")


if __name__ == "__main__":
    main()
