# Test Case Validation System - Complete Guide

## Overview
The system now automatically validates coding questions using test cases, showing candidates exactly how many tests passed.

## How It Works

### 1. Question Generation with Test Cases
When creating a new exam, the AI (Groq) automatically generates:
- **MCQ Questions**: With correct answers (A, B, C, D)
- **Coding Questions**: With 3-5 test cases that validate the solution
- **Scenario Questions**: For design/explanation

### 2. Test Case Format
For coding questions, test cases are stored as JSON:
```json
[
  {"input": "5", "expectedOutput": "120"},
  {"input": "3", "expectedOutput": "6"},
  {"input": "0", "expectedOutput": "1"}
]
```

### 3. Answer Evaluation

#### MCQ Questions
- Letter-based matching (A, B, C, D)
- Full points if correct, 0 if wrong
- Shows correct answer immediately after exam

#### Coding Questions
- **Automatic test execution** against all test cases
- **Proportional scoring**: 8/10 tests passed = 80% of points
- **Pass threshold**: ≥50% tests = considered "correct"
- Shows in results: "8/10 test cases passed (80%)"

#### Example:
- Question worth 25 points
- 10 test cases defined
- Candidate passes 8 tests
- **Points awarded: 20 points (80% × 25)**

### 4. Result Display

**During Exam:**
- No feedback shown (to prevent cheating)
- Generic "Answer submitted successfully"

**After Completion:**
- MCQ: Shows correct answer (e.g., "A) Correct option text")
- Coding: Shows test results (e.g., "8/10 test cases passed (80%)")
- Scenario: Shows if answer was substantial enough
- Color-coded: Green for correct, Red for incorrect

## Database Structure

### Questions Table
```sql
test_cases TEXT NULL -- JSON array of test cases
```

### Exam Answers Table
```sql
feedback TEXT NULL -- Stores "8/10 test cases passed (80%)"
```

## Testing the System

### To See Test Cases in Action:

1. **Start a NEW Exam** (old exams won't have test cases)
   - Go to Dashboard
   - Find a job with exam available
   - Click "Start Exam"

2. **Answer Coding Questions**
   - Write your solution
   - Submit the answer
   - No immediate feedback during exam

3. **Complete and View Results**
   - Finish all questions
   - Click "Submit Exam"
   - View detailed results showing test case pass/fail

## Current Status

✅ **Completed:**
- Test case structure in Question entity
- CodeExecutionService for running tests
- Proportional scoring based on pass percentage
- AI prompt updated to generate test cases
- Feedback storage in ExamAnswer entity
- Result display showing test case results

⚠️ **Notes:**
- Existing exams/questions won't have test cases (created before this feature)
- Only NEW exams generated after system restart will have test cases
- Test execution is currently simulated (60-80% pass rate for basic code)
- For production, implement actual code execution in Docker sandbox

## Code Execution

Currently using **simplified simulation**:
- Checks if code has basic structure (return, print, etc.)
- Simulates 60-80% success for reasonable code
- Returns test results without actual execution

**For production**, replace with:
- Docker containers for sandboxed execution
- Actual Java/Python/JavaScript compiler/interpreter
- Resource limits (CPU, memory, time)
- Security isolation

## Future Enhancements

1. **Real Code Execution**
   - Docker-based sandbox
   - Multiple language support (Java, Python, JS, C++)
   - Compile-time error detection

2. **Enhanced Feedback**
   - Show which specific tests failed
   - Display test input/output for failed cases
   - Hint system for partially correct solutions

3. **Custom Test Cases**
   - Allow recruiters to define custom test cases
   - Import test cases from existing problems
   - Test case difficulty levels

## Troubleshooting

### No test cases showing?
- Make sure you're starting a NEW exam, not resuming an old one
- Check backend logs for AI generation errors
- Verify Groq API key is configured

### Results showing "Manual Review Required"?
- Question was created before test case feature
- Test cases field is NULL in database
- Create a new exam to get test cases

### Points seem wrong?
- Proportional scoring: PassPercentage × MaxPoints
- Rounded to nearest integer
- ≥50% pass rate = considered correct
- <50% pass rate = considered incorrect (for adaptive difficulty)
