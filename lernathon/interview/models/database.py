"""
Database models for AI Voice Interview System
"""
from datetime import datetime
from sqlalchemy import Column, Integer, String, Text, DateTime, Float, ForeignKey, Enum
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base
import enum

Base = declarative_base()


class InterviewType(enum.Enum):
    """Interview types matching Java backend"""
    PHONE_SCREEN = "PHONE_SCREEN"
    VIDEO = "VIDEO"
    TECHNICAL = "TECHNICAL"
    BEHAVIORAL = "BEHAVIORAL"
    PANEL = "PANEL"
    FINAL = "FINAL"


class InterviewStatus(enum.Enum):
    """Interview status matching Java backend"""
    SCHEDULED = "SCHEDULED"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    NO_SHOW = "NO_SHOW"
    RESCHEDULED = "RESCHEDULED"


class Interview(Base):
    """Interview model - matches Java backend schema"""
    __tablename__ = "interviews"
    
    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    # Note: FK constraints removed for compatibility with existing DB schema
    application_id = Column(Integer, nullable=False, index=True)
    interviewer_id = Column(Integer, nullable=True)
    
    # Candidate information (denormalized for easy access)
    candidate_name = Column(String(255), nullable=True, index=True)
    candidate_email = Column(String(255), nullable=True)
    job_title = Column(String(255), nullable=True)
    
    type = Column(Enum(InterviewType), default=InterviewType.TECHNICAL)
    status = Column(Enum(InterviewStatus), default=InterviewStatus.SCHEDULED)
    
    scheduled_at = Column(DateTime, nullable=True)
    duration_minutes = Column(Integer, nullable=True)
    
    location = Column(String(500), nullable=True)
    meeting_link = Column(String(500), nullable=True)
    
    feedback = Column(Text, nullable=True)
    rating = Column(Integer, nullable=True)  # Overall 1-10 rating
    
    # Detailed scoring/marks
    technical_score = Column(Integer, nullable=True)  # Technical knowledge score
    communication_score = Column(Integer, nullable=True)  # Communication clarity score
    problem_solving_score = Column(Integer, nullable=True)  # Problem solving score
    cultural_fit_score = Column(Integer, nullable=True)  # Cultural fit score
    total_score = Column(Integer, nullable=True)  # Total score out of 100
    
    notes = Column(Text, nullable=True)
    
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    def __repr__(self):
        return f"<Interview {self.id}: Application {self.application_id} - {self.type.value}>"
    
    def to_dict(self):
        """Convert model to dictionary"""
        return {
            "id": self.id,
            "application_id": self.application_id,
            "interviewer_id": self.interviewer_id,
            "candidate_name": self.candidate_name,
            "candidate_email": self.candidate_email,
            "job_title": self.job_title,
            "type": self.type.value if self.type else None,
            "status": self.status.value if self.status else None,
            "scheduled_at": self.scheduled_at.isoformat() if self.scheduled_at else None,
            "duration_minutes": self.duration_minutes,
            "location": self.location,
            "meeting_link": self.meeting_link,
            "feedback": self.feedback,
            "rating": self.rating,
            "technical_score": self.technical_score,
            "communication_score": self.communication_score,
            "problem_solving_score": self.problem_solving_score,
            "cultural_fit_score": self.cultural_fit_score,
            "total_score": self.total_score,
            "notes": self.notes,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None
        }


# Legacy interview model for backward compatibility (if needed)
class LegacyInterview(Base):
    """Legacy interview session model"""
    __tablename__ = "legacy_interviews"
    
    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    candidate_name = Column(String(255), nullable=False, index=True)
    job_role = Column(String(255), nullable=False)
    question = Column(Text, nullable=False)
    answer_transcript = Column(Text, nullable=True)
    technical_score = Column(Integer, default=0)
    clarity_score = Column(Integer, default=0)
    communication_score = Column(Integer, default=0)
    total_score = Column(Integer, default=0)
    feedback = Column(Text, nullable=True)
    audio_file_path = Column(String(500), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    def __repr__(self):
        return f"<LegacyInterview {self.id}: {self.candidate_name} - {self.job_role}>"


class InterviewSession(Base):
    """Interview session tracking"""
    __tablename__ = "interview_sessions"
    
    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    session_id = Column(String(100), unique=True, index=True)
    application_id = Column(Integer, ForeignKey('applications.id'), nullable=True)
    candidate_name = Column(String(255), nullable=False)
    job_role = Column(String(255), nullable=False)
    skills = Column(Text, nullable=False)
    difficulty_level = Column(String(50), nullable=False)
    total_questions = Column(Integer, default=0)
    questions_answered = Column(Integer, default=0)
    average_score = Column(Float, default=0.0)
    status = Column(String(50), default="in_progress")  # in_progress, completed, abandoned
    started_at = Column(DateTime, default=datetime.utcnow)
    completed_at = Column(DateTime, nullable=True)
    
    def __repr__(self):
        return f"<InterviewSession {self.session_id}: {self.candidate_name}>"
    
    def to_dict(self):
        """Convert model to dictionary"""
        return {
            "id": self.id,
            "session_id": self.session_id,
            "application_id": self.application_id,
            "candidate_name": self.candidate_name,
            "job_role": self.job_role,
            "skills": self.skills,
            "difficulty_level": self.difficulty_level,
            "total_questions": self.total_questions,
            "questions_answered": self.questions_answered,
            "average_score": self.average_score,
            "status": self.status,
            "started_at": self.started_at.isoformat() if self.started_at else None,
            "completed_at": self.completed_at.isoformat() if self.completed_at else None
        }

