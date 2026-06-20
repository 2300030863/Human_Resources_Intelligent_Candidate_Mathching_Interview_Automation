# 📝 Coding Exam: Input/Output Based Testing - Quick Guide

## ✅ What Changed

The coding exam now shows **sample input and output test cases** prominently at the top, so candidates know exactly what format to expect.

## 🎯 How It Works

### 1️⃣ **Candidate Sees Sample Test Cases**
At the top of the exam page, candidates will see examples like:

```
Example 1:
Input:          Expected Output:
5               25
10              100
```

### 2️⃣ **Candidate Writes Code**
The candidate writes code that:
- **Reads input** from stdin (using `input()`, `Scanner`, `readline`, etc.)
- **Processes** the input
- **Prints output** to stdout in the exact format

### 3️⃣ **Testing Flow**

#### Option A: Test with Custom Input (Run Button)
1. Candidate writes custom input in the "Custom Input" tab
2. Clicks **Run** button
3. Sees the output their code produces
4. Can verify if it matches expected format

#### Option B: Evaluate Against All Test Cases
1. Candidate clicks **Evaluate** button
2. System runs code against ALL test cases (including hidden ones)
3. Shows detailed results:
   - ✓ Test Case 1: PASSED
   - ✓ Test Case 2: PASSED
   - ✗ Test Case 3: FAILED (shows expected vs received)
4. Calculates marks: `(2/3) × 25 = 16.67 marks`

## 📊 Marking System

```
Marks = (Passed Test Cases / Total Test Cases) × Maximum Marks
```

**Examples:**
- All tests pass (3/3): Full marks (25/25)
- Partial pass (2/3): 16.67 marks
- Failed all (0/3): 0 marks
- Compilation error: 0 marks
- Runtime error: 0 marks

## 🎨 New UI Features

### Sample Test Cases (Top of Page)
- Shows 3 sample test cases with input/output
- Clear instructions on how to approach the problem
- Step-by-step guide
- Marking formula displayed

### Code Editor
- Now has language badge (PYTHON, JAVA, JAVASCRIPT)
- Better default templates with input/output examples

### Custom Input Tab
- Enhanced with helpful instructions
- Shows input format examples
- Quick test without using up evaluation attempts

### Output Console Tab
- Structured compilation results
- Program output display
- Runtime error visualization
- Execution time tracking

### Test Evaluation Tab
- Detailed test case breakdown
- Expected vs Received comparison for failures
- Marks calculation display
- Final status badge (PASS/PARTIAL/FAIL)

## 💻 Code Templates Updated

### Python Example:
```python
# Read input from stdin and write output to stdout
# Example: Read two numbers and print their sum

# Read input
line = input().strip()
# Process input
# ...

# Print output
print(result)
```

### Java Example:
```java
import java.util.Scanner;

public class Solution {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Read input
        String line = scanner.nextLine();
        
        // Process input
        // ...
        
        // Print output
        System.out.println(result);
        
        scanner.close();
    }
}
```

### JavaScript Example:
```javascript
const readline = require('readline');
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

rl.on('line', (line) => {
    // Read input
    const input = line.trim();
    
    // Process input
    // ...
    
    // Print output
    console.log(result);
    
    rl.close();
});
```

## 🔥 Candidate Workflow Summary

1. **📋 Read** sample test cases at the top
2. **✍️ Write** code that reads input and produces output
3. **▶️ Test** with custom input using Run button
4. **🧪 Evaluate** against all test cases
5. **📊 View** marks and detailed results
6. **➡️ Submit** when satisfied and move to next question

## 🎯 Backend Requirements

The backend should implement:

1. **POST /exams/run** - Execute code with custom input
   ```json
   Request: { "code": "...", "input": "5\n10", "language": "python" }
   Response: { "status": "SUCCESS", "output": "15", "executionTime": "0.2s" }
   ```

2. **POST /exams/submit** - Evaluate against test cases
   ```json
   Request: {
     "code": "...",
     "testCases": [
       {"input": "5", "expectedOutput": "25"},
       {"input": "10", "expectedOutput": "100"}
     ],
     "language": "python",
     "maxMarks": 25
   }
   Response: {
     "status": "PASS",
     "totalTestCases": 2,
     "passed": 2,
     "marksAwarded": 25,
     "maxMarks": 25,
     "details": [
       {"testCase": 1, "status": "PASSED"},
       {"testCase": 2, "status": "PASSED"}
     ]
   }
   ```

## ⚠️ Important Notes

1. **Exact Match Required**: Output must match expected output exactly (including whitespace, newlines)
2. **Multiple Test Cases**: Code is tested against multiple inputs automatically
3. **Hidden Test Cases**: Some test cases are hidden from candidates to prevent hardcoding
4. **Proportional Marking**: Partial credit given for passing some tests
5. **One Evaluation**: Typically one evaluation attempt per question (can be configured)

## 🎓 Benefits

✅ Clear expectations - Candidates know exactly what's expected  
✅ Better testing - Can test with custom input before final submission  
✅ Fair marking - Proportional marks based on test cases passed  
✅ Professional UI - Clean, modern interface  
✅ Real-time feedback - Immediate results and marks  
✅ Detailed errors - Shows expected vs actual output for failed tests  

---

**Status:** ✅ Frontend Complete - Backend Integration Needed
