"""
FastAPI Backend for AI Voice Interview System
"""
from fastapi import FastAPI, HTTPException, Depends, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from datetime import datetime
import uuid
from pathlib import Path

from backend.schemas import (
    QuestionGenerateRequest, QuestionGenerateResponse,
    AnswerEvaluateRequest, AnswerEvaluateResponse,
    InterviewCreateRequest, InterviewResponse,
    SessionCreateRequest, SessionResponse
)
from database import get_db, init_database
from models.database import Interview, InterviewSession, InterviewType, InterviewStatus
from services import get_ai_service, get_stt_service
from config.settings import APP_NAME, APP_VERSION, AUDIO_STORAGE_PATH

# Initialize FastAPI app
app = FastAPI(
    title=APP_NAME,
    version=APP_VERSION,
    description="AI-powered voice interview system with automated evaluation"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def startup_event():
    """Initialize database on startup"""
    init_database()
    print(f"{APP_NAME} v{APP_VERSION} started successfully!")


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": f"Welcome to {APP_NAME}",
        "version": APP_VERSION,
        "status": "running"
    }


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "timestamp": datetime.utcnow()}


@app.post("/api/generate-question", response_model=QuestionGenerateResponse)
async def generate_question(request: QuestionGenerateRequest):
    """
    Generate interview question based on job role, skills, and difficulty
    """
    try:
        ai_service = get_ai_service()
        question = ai_service.generate_question(
            job_role=request.job_role,
            skills=request.skills,
            difficulty=request.difficulty,
            previous_questions=request.previous_questions
        )
        
        return QuestionGenerateResponse(question=question, success=True)
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error generating question: {str(e)}")


@app.post("/api/evaluate-answer", response_model=AnswerEvaluateResponse)
async def evaluate_answer(request: AnswerEvaluateRequest):
    """
    Evaluate candidate's answer using AI
    """
    try:
        ai_service = get_ai_service()
        evaluation = ai_service.evaluate_answer(
            question=request.question,
            answer=request.answer
        )
        
        return AnswerEvaluateResponse(**evaluation, success=True)
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error evaluating answer: {str(e)}")


@app.post("/api/transcribe-audio")
async def transcribe_audio(file: UploadFile = File(...)):
    """
    Transcribe uploaded audio file to text
    """
    try:
        # Save uploaded file temporarily
        temp_path = AUDIO_STORAGE_PATH / f"temp_{uuid.uuid4().hex}.wav"
        
        with open(temp_path, "wb") as f:
            content = await file.read()
            f.write(content)
        
        # Transcribe audio
        stt_service = get_stt_service()
        result = stt_service.transcribe_audio(str(temp_path))
        
        # Clean up temp file
        if temp_path.exists():
            temp_path.unlink()
        
        if result["success"]:
            return {
                "success": True,
                "transcript": result["transcript"],
                "language": result.get("language", "en")
            }
        else:
            raise HTTPException(status_code=500, detail=result.get("error", "Transcription failed"))
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error transcribing audio: {str(e)}")


@app.post("/api/interviews", response_model=InterviewResponse)
async def create_interview(request: InterviewCreateRequest, db: Session = Depends(get_db)):
    """
    Create new interview record in database
    """
    try:
        # Parse enum values
        interview_type = InterviewType[request.type] if hasattr(request, 'type') and request.type else InterviewType.TECHNICAL
        interview_status = InterviewStatus[request.status] if hasattr(request, 'status') and request.status else InterviewStatus.SCHEDULED
        
        interview = Interview(
            application_id=request.application_id,
            type=interview_type,
            status=interview_status,
            scheduled_at=request.scheduled_at if hasattr(request, 'scheduled_at') else None,
            duration_minutes=request.duration_minutes if hasattr(request, 'duration_minutes') else None,
            location=request.location if hasattr(request, 'location') else None,
            meeting_link=request.meeting_link if hasattr(request, 'meeting_link') else None,
            feedback=request.feedback if hasattr(request, 'feedback') else None,
            rating=request.rating if hasattr(request, 'rating') else None,
            notes=request.notes if hasattr(request, 'notes') else None
        )
        
        db.add(interview)
        db.commit()
        db.refresh(interview)
        
        return interview
    
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Error creating interview record: {str(e)}")


@app.get("/api/interviews", response_model=list[InterviewResponse])
async def get_interviews(
    skip: int = 0,
    limit: int = 100,
    application_id: int = None,
    status: str = None,
    db: Session = Depends(get_db)
):
    """
    Get list of interview records with optional filtering
    """
    try:
        query = db.query(Interview)
        
        if application_id:
            query = query.filter(Interview.application_id == application_id)
        
        if status:
            query = query.filter(Interview.status == status)
        
        interviews = query.order_by(Interview.created_at.desc()).offset(skip).limit(limit).all()
        
        return interviews
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching interviews: {str(e)}")


@app.get("/api/interviews/{interview_id}", response_model=InterviewResponse)
async def get_interview(interview_id: int, db: Session = Depends(get_db)):
    """
    Get specific interview by ID
    """
    try:
        interview = db.query(Interview).filter(Interview.id == interview_id).first()
        
        if not interview:
            raise HTTPException(status_code=404, detail="Interview not found")
        
        return interview
    
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching interview: {str(e)}")


@app.post("/api/sessions", response_model=SessionResponse)
async def create_session(request: SessionCreateRequest, db: Session = Depends(get_db)):
    """
    Create new interview session
    """
    try:
        session = InterviewSession(
            session_id=str(uuid.uuid4()),
            application_id=request.application_id if hasattr(request, 'application_id') else None,
            candidate_name=request.candidate_name,
            job_role=request.job_role,
            skills=request.skills,
            difficulty_level=request.difficulty_level,
            total_questions=request.total_questions,
            status="in_progress"
        )
        
        db.add(session)
        db.commit()
        db.refresh(session)
        
        return session
    
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Error creating session: {str(e)}")


@app.get("/api/sessions/{session_id}", response_model=SessionResponse)
async def get_session(session_id: str, db: Session = Depends(get_db)):
    """
    Get interview session by session ID
    """
    try:
        session = db.query(InterviewSession).filter(InterviewSession.session_id == session_id).first()
        
        if not session:
            raise HTTPException(status_code=404, detail="Session not found")
        
        return session
    
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching session: {str(e)}")


@app.put("/api/sessions/{session_id}")
async def update_session(
    session_id: str,
    questions_answered: int = None,
    average_score: float = None,
    status: str = None,
    db: Session = Depends(get_db)
):
    """
    Update interview session
    """
    try:
        session = db.query(InterviewSession).filter(InterviewSession.session_id == session_id).first()
        
        if not session:
            raise HTTPException(status_code=404, detail="Session not found")
        
        if questions_answered is not None:
            session.questions_answered = questions_answered
        
        if average_score is not None:
            session.average_score = average_score
        
        if status is not None:
            session.status = status
            if status == "completed":
                session.completed_at = datetime.utcnow()
        
        db.commit()
        db.refresh(session)
        
        return session
    
    except HTTPException:
        raise
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Error updating session: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
