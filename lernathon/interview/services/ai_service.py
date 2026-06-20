"""
AI Service for Question Generation and Answer Evaluation using Groq API
"""
import json
from groq import Groq
from config.settings import GROQ_API_KEY


class AIService:
    """AI Service for interview question generation and evaluation"""
    
    def __init__(self):
        self.client = Groq(api_key=GROQ_API_KEY)
        # Use faster model for quicker responses
        self.model = "llama-3.1-8b-instant"  # Much faster than 70b model
    
    def generate_question(self, job_role: str, skills: str, difficulty: str, previous_questions: list = None) -> str:
        """
        Generate interview question based on job role, skills, and difficulty
        
        Args:
            job_role: Target job role
            skills: Required skills (comma-separated)
            difficulty: Question difficulty (Basic/Intermediate/Advanced)
            previous_questions: List of previously asked questions to avoid repetition
            
        Returns:
            Generated question string
        """
        avoid_questions = ""
        if previous_questions:
            avoid_questions = f"\n\nAvoid asking questions similar to:\n" + "\n".join([f"- {q}" for q in previous_questions])
        
        prompt = f"""Generate one technical interview question for: {job_role}

Skills: {skills}
Difficulty: {difficulty}
{avoid_questions}

Return only the question."""
        
        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "You are an expert interviewer. Generate concise technical questions."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.7,
                max_tokens=150,  # Reduced further for speed
                timeout=5  # Shorter timeout for faster response
            )
            
            question = response.choices[0].message.content.strip()
            # Remove any quotation marks if present
            question = question.strip('"').strip("'")
            return question
            
        except Exception as e:
            print(f"Error generating question: {e}")
            return "Error generating question. Please try again."
    
    def evaluate_answer(self, question: str, answer: str) -> dict:
        """
        Evaluate candidate's answer using AI
        
        Args:
            question: The interview question
            answer: Candidate's answer transcript
            
        Returns:
            Dictionary with scores and feedback
        """
        prompt = f"""Question:
{question}

Candidate Answer:
{answer}

Evaluate the answer based on:
1. Technical Accuracy (0-5 points): How technically correct and accurate is the answer?
2. Clarity (0-3 points): How clear and well-structured is the explanation?
3. Communication (0-2 points): How well does the candidate communicate their ideas?

Return ONLY a valid JSON object in this exact format (no other text):
{{
  "technical": <number 0-5>,
  "clarity": <number 0-3>,
  "communication": <number 0-2>,
  "total": <sum of above three>,
  "feedback": "<brief constructive feedback in 1-2 sentences>"
}}"""
        
        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "You are an expert interviewer who evaluates candidate answers objectively. Always return valid JSON only."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.3,
                max_tokens=250,  # Optimized for faster evaluation
                timeout=15  # 15 second timeout
            )
            
            result = response.choices[0].message.content.strip()
            
            # Try to parse JSON
            try:
                evaluation = json.loads(result)
                
                # Validate scores
                evaluation["technical"] = max(0, min(5, int(evaluation.get("technical", 0))))
                evaluation["clarity"] = max(0, min(3, int(evaluation.get("clarity", 0))))
                evaluation["communication"] = max(0, min(2, int(evaluation.get("communication", 0))))
                evaluation["total"] = evaluation["technical"] + evaluation["clarity"] + evaluation["communication"]
                
                if "feedback" not in evaluation:
                    evaluation["feedback"] = "No feedback provided."
                
                return evaluation
                
            except json.JSONDecodeError:
                # Fallback if JSON parsing fails
                print(f"JSON parsing failed. Raw response: {result}")
                return {
                    "technical": 0,
                    "clarity": 0,
                    "communication": 0,
                    "total": 0,
                    "feedback": "Unable to evaluate the answer properly. Please try again."
                }
                
        except Exception as e:
            print(f"Error evaluating answer: {e}")
            return {
                "technical": 0,
                "clarity": 0,
                "communication": 0,
                "total": 0,
                "feedback": f"Error during evaluation: {str(e)}"
            }
    
    def analyze_job_posting(self, job_role: str, job_description: str = "") -> dict:
        """
        Analyze job posting and extract skills, decide duration, questions, and difficulty
        
        Args:
            job_role: Job title/role
            job_description: Job description text (optional)
            
        Returns:
            Dictionary with extracted_skills, duration_mins, num_questions, difficulty_levels
        """
        prompt = f"""Analyze this job position and provide interview configuration:

Job Role: {job_role}
Job Description: {job_description if job_description else "Not provided"}

Extract and provide:
1. Key technical skills (3-5 most important)
2. Recommended interview duration in minutes (10-60 mins based on role complexity)
3. Recommended number of questions (5-15 based on duration)
4. Difficulty distribution (percentages for Basic/Intermediate/Advanced)

Return ONLY a valid JSON object:
{{
  "skills": ["skill1", "skill2", "skill3"],
  "duration_mins": <number>,
  "num_questions": <number>,
  "difficulty_distribution": {{
    "basic": <percentage 0-100>,
    "intermediate": <percentage 0-100>,
    "advanced": <percentage 0-100>
  }},
  "experience_level": "junior|mid|senior"
}}"""
        
        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "You are an expert tech recruiter. Analyze job requirements and provide interview configuration. Return only valid JSON."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.3,
                max_tokens=400,
                timeout=10
            )
            
            result = response.choices[0].message.content.strip()
            config = json.loads(result)
            
            # Validate and set defaults
            if "skills" not in config or not config["skills"]:
                config["skills"] = self._extract_default_skills(job_role)
            
            config["duration_mins"] = max(10, min(60, config.get("duration_mins", 20)))
            config["num_questions"] = max(5, min(15, config.get("num_questions", 8)))
            
            if "difficulty_distribution" not in config:
                config["difficulty_distribution"] = {"basic": 30, "intermediate": 50, "advanced": 20}
            
            if "experience_level" not in config:
                config["experience_level"] = "mid"
            
            return config
            
        except Exception as e:
            print(f"Error analyzing job posting: {e}")
            # Return defaults
            return {
                "skills": self._extract_default_skills(job_role),
                "duration_mins": 20,
                "num_questions": 8,
                "difficulty_distribution": {"basic": 30, "intermediate": 50, "advanced": 20},
                "experience_level": "mid"
            }
    
    def _extract_default_skills(self, job_role: str) -> list:
        """Extract default skills based on job role"""
        job_lower = job_role.lower()
        
        if "software" in job_lower or "developer" in job_lower:
            return ["Programming", "Data Structures", "Problem Solving", "System Design"]
        elif "data" in job_lower:
            return ["Data Analysis", "SQL", "Python", "Statistics", "Machine Learning"]
        elif "frontend" in job_lower or "react" in job_lower:
            return ["JavaScript", "React", "HTML/CSS", "Web Development"]
        elif "backend" in job_lower:
            return ["API Design", "Databases", "Server Architecture", "Security"]
        elif "devops" in job_lower:
            return ["CI/CD", "Docker", "Kubernetes", "Cloud Services"]
        elif "manager" in job_lower:
            return ["Leadership", "Project Management", "Communication", "Strategy"]
        else:
            return ["Problem Solving", "Communication", "Technical Skills", "Collaboration"]
    
    def get_difficulty_for_question(self, question_number: int, total_questions: int, difficulty_distribution: dict) -> str:
        """
        Determine difficulty level for a specific question based on distribution
        
        Args:
            question_number: Current question number (1-indexed)
            total_questions: Total number of questions
            difficulty_distribution: Dict with basic/intermediate/advanced percentages
            
        Returns:
            Difficulty level string: "Basic", "Intermediate", or "Advanced"
        """
        basic_pct = difficulty_distribution.get("basic", 30)
        intermediate_pct = difficulty_distribution.get("intermediate", 50)
        
        # Calculate cutoff points
        basic_count = max(1, int(total_questions * basic_pct / 100))
        intermediate_count = int(total_questions * intermediate_pct / 100)
        
        if question_number <= basic_count:
            return "Basic"
        elif question_number <= (basic_count + intermediate_count):
            return "Intermediate"
        else:
            return "Advanced"


# Singleton instance
_ai_service = None

def get_ai_service() -> AIService:
    """Get AI service singleton instance"""
    global _ai_service
    if _ai_service is None:
        _ai_service = AIService()
    return _ai_service
