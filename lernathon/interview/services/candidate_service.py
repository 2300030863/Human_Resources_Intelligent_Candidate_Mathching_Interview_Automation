"""
Service for fetching candidate and application data
"""
from sqlalchemy import text
from database import SessionLocal


class CandidateService:
    """Service to fetch candidate and application information"""
    
    @staticmethod
    def get_application_by_id(application_id: int):
        """Get application with candidate and job details"""
        db = SessionLocal()
        try:
            query = text("""
                SELECT 
                    a.id as application_id,
                    a.status as application_status,
                    c.id as candidate_id,
                    c.first_name,
                    c.last_name,
                    c.email,
                    c.phone,
                    j.id as job_id,
                    j.title as job_title,
                    j.skills_required
                FROM applications a
                JOIN candidates c ON a.candidate_id = c.id
                JOIN jobs j ON a.job_id = j.id
                WHERE a.id = :application_id
            """)
            
            result = db.execute(query, {"application_id": application_id}).fetchone()
            
            if result:
                return {
                    "application_id": result[0],
                    "application_status": result[1],
                    "candidate_id": result[2],
                    "first_name": result[3],
                    "last_name": result[4],
                    "full_name": f"{result[3]} {result[4]}",
                    "email": result[5],
                    "phone": result[6],
                    "job_id": result[7],
                    "job_title": result[8],
                    "required_skills": result[9]
                }
            return None
            
        except Exception as e:
            print(f"Error fetching application: {e}")
            return None
        finally:
            db.close()
    
    @staticmethod
    def get_candidate_applications(candidate_name: str = None, email: str = None):
        """Get all applications for a candidate"""
        db = SessionLocal()
        try:
            if email:
                query = text("""
                    SELECT 
                        a.id as application_id,
                        a.status,
                        c.first_name,
                        c.last_name,
                        c.email,
                        j.title as job_title
                    FROM applications a
                    JOIN candidates c ON a.candidate_id = c.id
                    JOIN jobs j ON a.job_id = j.id
                    WHERE c.email = :email
                    ORDER BY a.applied_at DESC
                """)
                results = db.execute(query, {"email": email}).fetchall()
            elif candidate_name:
                query = text("""
                    SELECT 
                        a.id as application_id,
                        a.status,
                        c.first_name,
                        c.last_name,
                        c.email,
                        j.title as job_title
                    FROM applications a
                    JOIN candidates c ON a.candidate_id = c.id
                    JOIN jobs j ON a.job_id = j.id
                    WHERE CONCAT(c.first_name, ' ', c.last_name) LIKE :name
                    ORDER BY a.applied_at DESC
                """)
                results = db.execute(query, {"name": f"%{candidate_name}%"}).fetchall()
            else:
                return []
            
            applications = []
            for row in results:
                applications.append({
                    "application_id": row[0],
                    "status": row[1],
                    "first_name": row[2],
                    "last_name": row[3],
                    "full_name": f"{row[2]} {row[3]}",
                    "email": row[4],
                    "job_title": row[5]
                })
            
            return applications
            
        except Exception as e:
            print(f"Error fetching candidate applications: {e}")
            return []
        finally:
            db.close()
    
    @staticmethod
    def get_scheduled_interviews(application_id: int = None):
        """Get scheduled interviews for an application"""
        db = SessionLocal()
        try:
            if application_id:
                query = text("""
                    SELECT 
                        i.id,
                        i.type,
                        i.status,
                        i.scheduled_at,
                        i.duration_minutes,
                        a.id as application_id,
                        c.first_name,
                        c.last_name,
                        j.title as job_title
                    FROM interviews i
                    JOIN applications a ON i.application_id = a.id
                    JOIN candidates c ON a.candidate_id = c.id
                    JOIN jobs j ON a.job_id = j.id
                    WHERE i.application_id = :application_id
                    ORDER BY i.scheduled_at DESC
                """)
                results = db.execute(query, {"application_id": application_id}).fetchall()
            else:
                query = text("""
                    SELECT 
                        i.id,
                        i.type,
                        i.status,
                        i.scheduled_at,
                        i.duration_minutes,
                        a.id as application_id,
                        c.first_name,
                        c.last_name,
                        j.title as job_title
                    FROM interviews i
                    JOIN applications a ON i.application_id = a.id
                    JOIN candidates c ON a.candidate_id = c.id
                    JOIN jobs j ON a.job_id = j.id
                    WHERE i.status = 'SCHEDULED'
                    ORDER BY i.scheduled_at ASC
                """)
                results = db.execute(query).fetchall()
            
            interviews = []
            for row in results:
                interviews.append({
                    "interview_id": row[0],
                    "type": row[1],
                    "status": row[2],
                    "scheduled_at": row[3],
                    "duration_minutes": row[4],
                    "application_id": row[5],
                    "candidate_name": f"{row[6]} {row[7]}",
                    "job_title": row[8]
                })
            
            return interviews
            
        except Exception as e:
            print(f"Error fetching interviews: {e}")
            return []
        finally:
            db.close()
    
    @staticmethod
    def create_interview_with_details(application_id: int, interview_data: dict):
        """
        Create an interview and automatically populate candidate details
        
        Args:
            application_id: The application ID
            interview_data: Dict with interview details (type, scheduled_at, etc.)
        
        Returns:
            Created interview object or None
        """
        from models.database import Interview, InterviewType, InterviewStatus
        from datetime import datetime
        
        db = SessionLocal()
        try:
            # First, get the candidate details
            app_data = CandidateService.get_application_by_id(application_id)
            if not app_data:
                print(f"Application {application_id} not found")
                return None
            
            # Create interview with candidate details
            interview = Interview(
                application_id=application_id,
                candidate_name=app_data['full_name'],
                candidate_email=app_data['email'],
                job_title=app_data['job_title'],
                type=interview_data.get('type', InterviewType.TECHNICAL),
                status=interview_data.get('status', InterviewStatus.SCHEDULED),
                scheduled_at=interview_data.get('scheduled_at', datetime.now()),
                duration_minutes=interview_data.get('duration_minutes', 30),
                interviewer_id=interview_data.get('interviewer_id'),
                location=interview_data.get('location'),
                meeting_link=interview_data.get('meeting_link'),
                notes=interview_data.get('notes')
            )
            
            db.add(interview)
            db.commit()
            db.refresh(interview)
            
            return interview
            
        except Exception as e:
            db.rollback()
            print(f"Error creating interview: {e}")
            return None
        finally:
            db.close()
    
    @staticmethod
    def update_interview_scores(interview_id: int, scores: dict):
        """
        Update interview with detailed scores and marks
        
        Args:
            interview_id: The interview ID
            scores: Dict with scoring details:
                - technical_score: 0-100
                - communication_score: 0-100
                - problem_solving_score: 0-100
                - cultural_fit_score: 0-100
                - rating: 1-10 overall rating
                - feedback: Text feedback
                - notes: Additional notes
        
        Returns:
            Updated interview object or None
        """
        from models.database import Interview, InterviewStatus
        
        db = SessionLocal()
        try:
            interview = db.query(Interview).filter(Interview.id == interview_id).first()
            
            if not interview:
                print(f"Interview {interview_id} not found")
                return None
            
            # Update scores
            if 'technical_score' in scores:
                interview.technical_score = scores['technical_score']
            if 'communication_score' in scores:
                interview.communication_score = scores['communication_score']
            if 'problem_solving_score' in scores:
                interview.problem_solving_score = scores['problem_solving_score']
            if 'cultural_fit_score' in scores:
                interview.cultural_fit_score = scores['cultural_fit_score']
            
            # Calculate total score if individual scores provided
            score_fields = [
                scores.get('technical_score', 0) or 0,
                scores.get('communication_score', 0) or 0,
                scores.get('problem_solving_score', 0) or 0,
                scores.get('cultural_fit_score', 0) or 0
            ]
            if any(score_fields):
                interview.total_score = sum(score_fields) // 4  # Average
            
            # Update other fields
            if 'rating' in scores:
                interview.rating = scores['rating']
            if 'feedback' in scores:
                interview.feedback = scores['feedback']
            if 'notes' in scores:
                interview.notes = scores['notes']
            if 'status' in scores:
                interview.status = scores['status']
            else:
                # If scores are provided, mark as completed
                interview.status = InterviewStatus.COMPLETED
            
            db.commit()
            db.refresh(interview)
            
            return interview
            
        except Exception as e:
            db.rollback()
            print(f"Error updating interview scores: {e}")
            return None
        finally:
            db.close()


def get_candidate_service():
    """Get candidate service instance"""
    return CandidateService()
