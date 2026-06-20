# 🎨 Coding Exam Interface - Visual Layout

## 📱 Complete Interface Structure

```
┌─────────────────────────────────────────────────────────────────────┐
│ 📋 Sample Test Cases - Your code should handle these inputs         │
│                                                                      │
│ ℹ️ INSTRUCTIONS: Write code that reads the input and produces      │
│    the expected output. Your code will be tested against these      │
│    and additional hidden test cases.                                │
│                                                                      │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ [Example 1]                                                   │   │
│ │                                                               │   │
│ │  Input:                    │  Expected Output:               │   │
│ │  ┌──────────────────┐      │  ┌──────────────────┐          │   │
│ │  │ 5                │      │  │ 25               │          │   │
│ │  │ 10               │      │  │ 100              │          │   │
│ │  └──────────────────┘      │  └──────────────────┘          │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ [Example 2]                                                   │   │
│ │  Input: 3              │  Expected Output: 9                 │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ [Example 3]                                                   │   │
│ │  Input: 7              │  Expected Output: 49                │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ ℹ️  + 2 more hidden test case(s)                                   │
│     These will be used for evaluation when you click "Evaluate"     │
│                                                                      │
│ 💡 How to solve this problem:                                       │
│  1. Read the sample inputs and outputs carefully                    │
│  2. Write code that reads input from stdin                          │
│  3. Process the input according to requirements                     │
│  4. Print output to stdout in exact format shown                    │
│  5. Use Run button to test with custom input                        │
│  6. Click Evaluate to test all cases and get marks                  │
│                                                                      │
│ 📊 Marking: (Passed Tests / Total Tests) × 25 marks                │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ Code Editor                                          [PYTHON]        │
├─────────────────────────────────────────────────────────────────────┤
│  1 │ # Read input from stdin and write output to stdout            │
│  2 │ # Example: Read two numbers and print their sum               │
│  3 │                                                                │
│  4 │ # Read input                                                   │
│  5 │ line = input().strip()                                         │
│  6 │ # Process input                                                │
│  7 │ # ...                                                          │
│  8 │                                                                │
│  9 │ # Print output                                                 │
│ 10 │ print(result)                                                  │
│    │                                                                │
│    │                                                                │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ [🔨 Compile] [▶️ Run with Custom Input] [🧪 Evaluate & Get Marks]  │
│                                              [Next Question ➡️]     │
│                                                                      │
│ ℹ️ Quick Guide: Compile → Run → Evaluate → Next/Submit            │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ [Custom Input] [Output Console] [Test Evaluation]                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  CUSTOM INPUT TAB:                                                  │
│  ┌────────────────────────────────────────────────┐                │
│  │ ⌨️ Test Your Code with Custom Input            │                │
│  │                                                │                │
│  │ ℹ️ Enter input in same format as samples      │                │
│  │                                                │                │
│  │ Enter custom input for testing...             │                │
│  │                                                │                │
│  │ Example (if test expects two numbers):        │                │
│  │ 5                                              │                │
│  │ 10                                             │                │
│  │                                                │                │
│  └────────────────────────────────────────────────┘                │
│  💡 Tip: Use sample test cases as reference                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  OUTPUT CONSOLE TAB:                                                │
│  ┌────────────────────────────────────────────────┐                │
│  │ 📄 Compilation Result              [✓ SUCCESS] │                │
│  ├────────────────────────────────────────────────┤                │
│  │ Status: SUCCESS                                │                │
│  │ Message: Compiled Successfully                 │                │
│  └────────────────────────────────────────────────┘                │
│                                                                      │
│  ┌────────────────────────────────────────────────┐                │
│  │ ▶️ Program Output                  [✓ SUCCESS] │                │
│  ├────────────────────────────────────────────────┤                │
│  │ ╔═══════════════════════════════════╗          │                │
│  │ ║ 25                                ║          │                │
│  │ ╚═══════════════════════════════════╝          │                │
│  │ 🕐 Execution Time: 0.2s                        │                │
│  └────────────────────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  TEST EVALUATION TAB:                                               │
│  ┌────────────────────────────────────────────────┐                │
│  │ ✅ Test Case Evaluation       [⚠ PARTIAL PASS] │                │
│  ├────────────────────────────────────────────────┤                │
│  │                                                │                │
│  │ ┌────────────────────────────────────────┐    │                │
│  │ │ ✓ Test Case 1: PASSED      [PASSED]    │    │                │
│  │ └────────────────────────────────────────┘    │                │
│  │                                                │                │
│  │ ┌────────────────────────────────────────┐    │                │
│  │ │ ✓ Test Case 2: PASSED      [PASSED]    │    │                │
│  │ └────────────────────────────────────────┘    │                │
│  │                                                │                │
│  │ ┌────────────────────────────────────────┐    │                │
│  │ │ ✗ Test Case 3: FAILED      [FAILED]    │    │                │
│  │ │   Expected: 49                         │    │                │
│  │ │   Received: 48                         │    │                │
│  │ └────────────────────────────────────────┘    │                │
│  │                                                │                │
│  │ ───────────────────────────────────────────   │                │
│  │                                                │                │
│  │ Total Passed:              2 / 3               │                │
│  │                                                │                │
│  │ 📊 Marks Awarded:         16.67 / 25          │                │
│  │                                                │                │
│  │ 🎯 Final Status:     [⚠ PARTIAL PASS]         │                │
│  │                                                │                │
│  └────────────────────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────────────┘
```

## 🎨 Color Scheme

### Status Colors:
- ✅ **PASS** → Green (#22c55e)
- ⚠️ **PARTIAL_PASS** → Orange (#f97316)
- ❌ **FAIL** → Red (#ef4444)
- ⏱️ **TIMEOUT** → Dark Red (#991b1b)

### Section Colors:
- **Sample Test Cases** → Blue gradient
- **Code Editor** → Dark theme (VS Dark)
- **Compile Output** → Blue/Red based on status
- **Run Output** → Green/Red based on status
- **Evaluation** → Purple accent

## 🔄 User Flow Example

### Scenario: Square a Number Problem

```
Problem: Read a number and print its square

Sample Input:  5
Sample Output: 25
```

#### Step 1: Read Sample Cases ✅
Candidate sees 3 examples showing input → output

#### Step 2: Write Code ✅
```python
n = int(input())
print(n * n)
```

#### Step 3: Test with Custom Input ✅
- Go to "Custom Input" tab
- Enter: `7`
- Click "Run with Custom Input"
- See output: `49` ✓

#### Step 4: Evaluate ✅
- Click "Evaluate & Get Marks"
- See results:
  - Test 1 (input: 5) → ✓ PASSED
  - Test 2 (input: 10) → ✓ PASSED
  - Test 3 (input: -3) → ✓ PASSED
  - Test 4 (input: 0) → ✓ PASSED
  - Test 5 (input: 100) → ✓ PASSED

**Result:** 5/5 tests passed → 25/25 marks 🎉

#### Step 5: Submit ✅
- Click "Next Question" or "Submit Exam"

## 📊 Marks Breakdown Display

```
┌─────────────────────────────────────────┐
│  Test Results Summary                   │
├─────────────────────────────────────────┤
│  Total Test Cases:  5                   │
│  Passed:           4                   │
│  Failed:           1                   │
│  Pass Rate:        80%                  │
│                                         │
│  📊 Marks Calculation:                  │
│  (4 ÷ 5) × 25 = 20 marks               │
│                                         │
│  Final Score:  20 / 25                 │
│  Status:       ⚠ PARTIAL PASS          │
└─────────────────────────────────────────┘
```

## 🎯 Key Features Highlighted

1. **📋 Prominent Sample Cases** - Shows expected format clearly
2. **💡 Step-by-Step Guide** - Tells candidates exactly what to do
3. **⌨️ Custom Input Testing** - Test before final evaluation
4. **🧪 Detailed Evaluation** - Shows which tests passed/failed
5. **📊 Clear Marking** - Transparent marks calculation
6. **🎨 Color-Coded Results** - Easy to understand at a glance
7. **⚡ Real-time Feedback** - Instant results for all actions

## 🚀 Advantages Over Previous Design

| Feature | Before | After |
|---------|--------|-------|
| Sample visibility | Hidden/unclear | Prominent at top ✅ |
| Input format | Unclear | Clear examples ✅ |
| Testing | Limited | Custom + Evaluation ✅ |
| Marks display | Basic | Detailed breakdown ✅ |
| User guidance | Minimal | Step-by-step ✅ |
| Error details | Basic | Expected vs Actual ✅ |
| Status indication | Text only | Color-coded badges ✅ |

---

**Visual Demo:** Navigate to `http://localhost:5174/exam/[examId]` to see the live interface!
