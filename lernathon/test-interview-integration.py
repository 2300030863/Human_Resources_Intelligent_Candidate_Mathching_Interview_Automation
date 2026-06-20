"""
Test script for Interview System Integration
Tests database connection, candidate fetching, and interview creation
"""

import sys
from pathlib import Path

# Add interview directory to path
interview_dir = Path(__file__).parent / "interview"
sys.path.insert(0, str(interview_dir))

# Add parent directory to path for database module
parent_dir = Path(__file__).parent
sys.path.insert(0, str(parent_dir))

from database import SessionLocal, init_database
from models.database import Interview, InterviewSession, InterviewType, InterviewStatus
from services.candidate_service import CandidateService
from datetime import datetime


def test_database_connection():
    """Test database connection"""
    print("\n=== Testing Database Connection ===")
    try:
        db = SessionLocal()
        print("✅ Database connection successful!")
        db.close()
        return True
    except Exception as e:
        print(f"❌ Database connection failed: {e}")
        return False


def test_fetch_application(application_id=1):
    """Test fetching application data"""
    print(f"\n=== Testing Fetch Application (ID: {application_id}) ===")
    try:
        candidate_service = CandidateService()
        app_data = candidate_service.get_application_by_id(application_id)
        
        if app_data:
            print("✅ Application found!")
            print(f"   Candidate: {app_data['full_name']}")
            print(f"   Email: {app_data['email']}")
            print(f"   Job: {app_data['job_title']}")
            print(f"   Required Skills: {app_data['required_skills']}")
            return True, app_data
        else:
            print(f"❌ Application {application_id} not found")
            print("   Please ensure there is an application with this ID in the database")
            return False, None
            
    except Exception as e:
        print(f"❌ Error fetching application: {e}")
        import traceback
        traceback.print_exc()
        return False, None


def test_create_interview(application_id=1):
    """Test creating an interview record"""
    print(f"\n=== Testing Create Interview (Application ID: {application_id}) ===")
    try:
        db = SessionLocal()
        
        # Create interview
        interview = Interview(
            application_id=application_id,
            type=InterviewType.TECHNICAL,
            status=InterviewStatus.SCHEDULED,
            scheduled_at=datetime.now(),
            duration_minutes=30,
            notes="Test interview created by test script"
        )
        
        db.add(interview)
        db.commit()
        db.refresh(interview)
        
        interview_id = interview.id
        
        print(f"✅ Interview created successfully!")
        print(f"   Interview ID: {interview_id}")
        print(f"   Type: {interview.type.value}")
        print(f"   Status: {interview.status.value}")
        
        db.close()
        return True, interview_id
        
    except Exception as e:
        print(f"❌ Error creating interview: {e}")
        import traceback
        traceback.print_exc()
        return False, None


def test_update_interview(interview_id):
    """Test updating interview with results"""
    print(f"\n=== Testing Update Interview (ID: {interview_id}) ===")
    try:
        db = SessionLocal()
        
        interview = db.query(Interview).filter(Interview.id == interview_id).first()
        
        if interview:
            # Update interview
            interview.status = InterviewStatus.COMPLETED
            interview.rating = 8
            interview.feedback = "Excellent performance in technical interview"
            interview.notes = (interview.notes or "") + "\n\nTest Q&A:\nQ: Test question?\nA: Test answer\nScore: 8/10"
            
            db.commit()
            
            print("✅ Interview updated successfully!")
            print(f"   Status: {interview.status.value}")
            print(f"   Rating: {interview.rating}/10")
            print(f"   Feedback: {interview.feedback}")
            
            db.close()
            return True
        else:
            print(f"❌ Interview {interview_id} not found")
            return False
            
    except Exception as e:
        print(f"❌ Error updating interview: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_query_interview(interview_id):
    """Test querying interview with candidate details"""
    print(f"\n=== Testing Query Interview (ID: {interview_id}) ===")
    try:
        from sqlalchemy import text
        db = SessionLocal()
        
        query = text("""
            SELECT 
                i.id as interview_id,
                i.type,
                i.status,
                i.rating,
                i.feedback,
                c.first_name,
                c.last_name,
                j.title as job_title
            FROM interviews i
            JOIN applications a ON i.application_id = a.id
            JOIN candidates c ON a.candidate_id = c.id
            JOIN jobs j ON a.job_id = j.id
            WHERE i.id = :interview_id
        """)
        
        result = db.execute(query, {"interview_id": interview_id}).fetchone()
        
        if result:
            print("✅ Interview query successful!")
            print(f"   Interview ID: {result[0]}")
            print(f"   Candidate: {result[5]} {result[6]}")
            print(f"   Job: {result[7]}")
            print(f"   Type: {result[1]}")
            print(f"   Status: {result[2]}")
            print(f"   Rating: {result[3]}/10")
            
            db.close()
            return True
        else:
            print(f"❌ Interview {interview_id} not found in query")
            return False
            
    except Exception as e:
        print(f"❌ Error querying interview: {e}")
        import traceback
        traceback.print_exc()
        return False


def cleanup_test_interview(interview_id):
    """Clean up test interview"""
    print(f"\n=== Cleaning Up Test Interview (ID: {interview_id}) ===")
    try:
        db = SessionLocal()
        
        interview = db.query(Interview).filter(Interview.id == interview_id).first()
        
        if interview:
            db.delete(interview)
            db.commit()
            print("✅ Test interview deleted successfully")
            
        db.close()
        return True
        
    except Exception as e:
        print(f"❌ Error cleaning up: {e}")
        return False


def main():
    """Run all tests"""
    print("=" * 60)
    print("INTERVIEW SYSTEM INTEGRATION TEST")
    print("=" * 60)
    
    # Test 1: Database connection
    if not test_database_connection():
        print("\n❌ Database connection failed. Please check:")
        print("   1. MySQL is running")
        print("   2. Database 'recruit_db' exists")
        print("   3. Credentials are correct (root/12345)")
        return
    
    # Test 2: Fetch application
    application_id = 26  # Using existing application with CANDIDATE alluri
    success, app_data = test_fetch_application(application_id)
    
    if not success:
        print("\n❌ Cannot proceed without a valid application.")
        print("   Please create an application first or change application_id in the script.")
        return
    
    # Test 3: Create interview
    success, interview_id = test_create_interview(application_id)
    
    if not success:
        print("\n❌ Interview creation failed.")
        return
    
    # Test 4: Update interview
    if not test_update_interview(interview_id):
        print("\n❌ Interview update failed.")
        return
    
    # Test 5: Query interview
    if not test_query_interview(interview_id):
        print("\n❌ Interview query failed.")
        return
    
    # Cleanup (optional - comment out if you want to keep test data)
    # cleanup_test_interview(interview_id)
    
    print("\n" + "=" * 60)
    print("✅ ALL TESTS PASSED!")
    print("=" * 60)
    print(f"\nTest interview ID: {interview_id}")
    print("You can view this interview in the database or keep it for testing.")
    print("\nTo delete the test interview, uncomment the cleanup line in main()")


if __name__ == "__main__":
    main()
