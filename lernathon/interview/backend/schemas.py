"""
Pydantic schemas for request/response validation
"""
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime


class QuestionGenerateRequest(BaseModel):
    """Request schema for question generation"""
    job_role: str = Field(..., min_length=1, description="Job role for the interview")
    skills: str = Field(..., min_length=1, description="Required skills (comma-separated)")
    difficulty: str = Field(..., description="Difficulty level: Basic, Intermediate, or Advanced")
    previous_questions: Optional[list[str]] = Field(default=None, description="Previously asked questions")


class QuestionGenerateResponse(BaseModel):
    """Response schema for question generation"""
    question: str
    success: bool = True


class AnswerEvaluateRequest(BaseModel):
    """Request schema for answer evaluation"""
    question: str = Field(..., min_length=1)
    answer: str = Field(..., min_length=1)


class AnswerEvaluateResponse(BaseModel):
    """Response schema for answer evaluation"""
    technical: int = Field(..., ge=0, le=5)
    clarity: int = Field(..., ge=0, le=3)
    communication: int = Field(..., ge=0, le=2)
    total: int = Field(..., ge=0, le=10)
    feedback: str
    success: bool = True


class InterviewCreateRequest(BaseModel):
    """Request schema for creating interview record"""
    application_id: int = Field(..., description="Application ID from recruitment system")
    type: Optional[str] = Field(default="TECHNICAL", description="Interview type")
    status: Optional[str] = Field(default="SCHEDULED", description="Interview status")
    scheduled_at: Optional[datetime] = None
    duration_minutes: Optional[int] = None
    location: Optional[str] = None
    meeting_link: Optional[str] = None
    feedback: Optional[str] = None
    rating: Optional[int] = Field(default=None, ge=1, le=10)
    notes: Optional[str] = None


class InterviewResponse(BaseModel):
    """Response schema for interview record"""
    id: int
    application_id: int
    interviewer_id: Optional[int]
    type: str
    status: str
    scheduled_at: Optional[datetime]
    duration_minutes: Optional[int]
    location: Optional[str]
    meeting_link: Optional[str]
    feedback: Optional[str]
    rating: Optional[int]
    notes: Optional[str]
    created_at: datetime
    updated_at: Optional[datetime]
    
    class Config:
        from_attributes = True


class SessionCreateRequest(BaseModel):
    """Request schema for creating interview session"""
    application_id: Optional[int] = None
    candidate_name: str
    job_role: str
    skills: str
    difficulty_level: str
    total_questions: int = 5


class SessionResponse(BaseModel):
    """Response schema for interview session"""
    id: int
    session_id: str
    application_id: Optional[int]
    candidate_name: str
    job_role: str
    skills: str
    difficulty_level: str
    total_questions: int
    questions_answered: int
    average_score: float
    status: str
    started_at: datetime
    completed_at: Optional[datetime]
    
    class Config:
        from_attributes = True
