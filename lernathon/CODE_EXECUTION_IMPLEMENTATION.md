# Code Execution System Implementation

## Overview
The code execution system has been **fully implemented** to actually compile and run candidate code against test cases, replacing the previous simulation-based approach.

## What Changed

### Before (Simulated)
- `CodeExecutionService` used a `simulateExecution()` method with random results
- No actual compilation or code execution
- Random pass/fail results (60-80% success rate)

### After (Real Execution)
- **Java**: Real compilation with `javac` and execution with `java` command
- **Python**: Real execution with `python3`/`python` interpreter
- **JavaScript**: Real execution with `node` (Node.js)
- Proper test case validation with actual input/output comparison

## Implementation Details

### 1. Java Code Execution
```
Candidate Code → Wrap in class → Compile with javac → Run with java → Compare output
```

**Features:**
- Automatically wraps code snippets in a class structure if needed
- Compiles Java code using system `javac` command
- Executes compiled bytecode with timeout protection
- Captures stdout and stderr
- 5-second timeout per test case

**Code Handling:**
- Full class definitions: Used as-is
- Single methods: Wrapped in a class
- Code snippets: Wrapped in main method and class

### 2. Python Code Execution
```
Candidate Code → Write to .py file → Execute with python → Compare output
```

**Features:**
- Automatically detects `python3` or `python` command
- Executes Python scripts with input via stdin
- 5-second timeout per test case
- Captures execution errors

### 3. JavaScript Code Execution
```
Candidate Code → Write to .js file → Execute with node → Compare output
```

**Features:**
- Runs JavaScript using Node.js runtime
- Provides input via stdin
- 5-second timeout per test case
- Full error reporting

## Security & Safety Features

### 1. Timeout Protection
- Each test case has a 5-second timeout
- Prevents infinite loops from freezing the system
- Process is forcibly terminated if timeout exceeds

### 2. Temporary File Management
- All code execution happens in temporary directories
- Files are automatically cleaned up after execution
- No pollution of the system file structure

### 3. Error Handling
- Compilation errors are captured and reported
- Runtime errors are caught and logged
- Failed tests are tracked with detailed feedback

### 4. Process Isolation
- Each test case runs in a separate process
- Input/output streams are properly managed
- Error streams are separated from output

## Test Case Format

Test cases should be stored as JSON in the `test_cases` column of the `questions` table:

```json
[
  {
    "input": "5",
    "expectedOutput": "120"
  },
  {
    "input": "3",
    "expectedOutput": "6"
  }
]
```

## Scoring System

### Points Calculation
- **Total Points**: Based on `points` field in question (default: 10)
- **Earned Points**: Proportional to test cases passed
  - Example: If 3/5 tests pass → 60% of total points

### Pass/Fail Threshold
- A coding question is marked as "correct" if **≥50% of test cases pass**
- This affects the consecutive correct/wrong counters for adaptive difficulty

### Adaptive Difficulty
- **3 consecutive correct** → Difficulty increases
- **2 consecutive wrong** → Difficulty decreases
- Difficulty levels: EASY → MEDIUM → HARD → ADVANCED

## Requirements

### System Requirements
1. **Java** (for Java code execution)
   - JDK must be installed
   - `javac` and `java` commands must be in PATH

2. **Python** (for Python code execution)
   - Python 3.x or Python 2.x
   - `python3` or `python` command must be in PATH

3. **Node.js** (for JavaScript code execution)
   - Node.js runtime installed
   - `node` command must be in PATH

### Backend Requirements
- Spring Boot application
- Database with `questions` table containing `test_cases` column

## Testing the Implementation

### 1. Create a Coding Question with Test Cases

```sql
INSERT INTO questions (question, type, difficulty, correct_answer, test_cases, points, category)
VALUES (
  'Write a function that calculates the factorial of a number.',
  'CODING',
  'MEDIUM',
  'N/A',
  '[{"input":"5","expectedOutput":"120"},{"input":"3","expectedOutput":"6"},{"input":"0","expectedOutput":"1"}]',
  15,
  'Algorithms'
);
```

### 2. Submit a Correct Java Solution

```java
public class Solution {
    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        int n = sc.nextInt();
        int result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        System.out.println(result);
    }
}
```

**Expected Result:**
- 3/3 test cases pass
- 100% score
- 15/15 points earned

### 3. Submit an Incorrect Solution

```java
public class Solution {
    public static void main(String[] args) {
        System.out.println("Wrong answer");
    }
}
```

**Expected Result:**
- 0/3 test cases pass
- 0% score
- 0/15 points earned

### 4. Check Logs

Look for these log messages in console/logs:
```
Evaluating CODING question {id} with test cases
Test execution result: {passed}/{total} tests passed ({percentage}%)
Coding question evaluated: {feedback} - {points} points awarded
```

## API Flow

```
1. Frontend sends answer via POST /api/exam/submit-answer
   ↓
2. ExamSubmissionService.submitAnswer()
   ↓
3. evaluateAnswerWithDetails() checks question type
   ↓
4. For CODING: codeExecutionService.executeTestCases()
   ↓
5. Based on language:
   - Java: Compile with javac → Run with java
   - Python: Run with python3/python
   - JavaScript: Run with node
   ↓
6. Compare output with expectedOutput
   ↓
7. Calculate score: (passedTests / totalTests) * maxPoints
   ↓
8. Store result in exam_answers table
   ↓
9. Update exam attempt statistics
   ↓
10. Return response (without revealing correctness during exam)
```

## Example Result in Database

After submission, the `exam_answers` table will contain:

| Field | Value |
|-------|-------|
| candidate_answer | (Full code submitted) |
| is_correct | true/false (≥50% pass = true) |
| points_earned | 12 (if 4/5 tests passed on 15-point question) |
| feedback | "4/5 test cases passed (80.0%)" |

## Troubleshooting

### "Compilation timeout"
- Code takes too long to compile (>5 seconds)
- Check for infinite loops or excessive dependencies

### "Execution timeout"
- Code runs too long (>5 seconds per test)
- Check for infinite loops in the submitted code

### "Compilation error"
- Syntax errors in submitted code
- Error message is captured and stored

### "Runtime error"
- Code compiles but crashes during execution
- Exception details are captured

### "Command not found" errors
- Java: Install JDK and add to PATH
- Python: Install Python and add to PATH  
- Node.js: Install Node.js and add to PATH

## Production Recommendations

### 1. Docker Sandboxing
For production, run code execution in Docker containers:
```yaml
docker run --rm -v /tmp/code:/code -w /code \
  --memory=256m --cpus=0.5 \
  --network=none \
  openjdk:17 java Solution
```

### 2. Resource Limits
- Memory limits per execution
- CPU usage limits
- Disk space limits

### 3. Security Hardening
- Run in isolated containers
- No network access during execution
- Restrict file system access
- Scan for malicious code patterns

### 4. Monitoring & Logging
- Track execution times
- Monitor resource usage
- Alert on suspicious activity
- Log all executions for audit

## Next Steps

1. ✅ Real code compilation and execution implemented
2. ✅ Java, Python, JavaScript support
3. ✅ Test case validation with scoring
4. ✅ Timeout and error handling
5. 🔲 Add Docker sandboxing (recommended for production)
6. 🔲 Add support for more languages (C++, C#, Ruby, etc.)
7. 🔲 Implement code security scanning
8. 🔲 Add memory and CPU profiling

## Summary

The code execution system is now **fully functional** and actually runs candidate code:
- ✅ Real compilation for Java
- ✅ Real execution for Java, Python, JavaScript
- ✅ Test case validation with actual input/output
- ✅ Proportional scoring based on pass percentage
- ✅ Timeout protection
- ✅ Error handling and feedback
- ✅ Temporary file cleanup

Candidates' code is now properly evaluated against real test cases!
