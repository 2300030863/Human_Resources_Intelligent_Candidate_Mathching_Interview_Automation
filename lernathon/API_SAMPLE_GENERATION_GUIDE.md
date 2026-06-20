# 🤖 API-Based Sample Generation - Implementation Guide

## Overview
The coding exam interface now supports **dynamic AI-powered sample input/output generation** via API instead of relying solely on static test cases. This allows creating flexible, varied sample test cases on-demand.

---

## ✅ Frontend Implementation (Complete)

### New Features Added

1. **State Management**
   ```tsx
   const [isGeneratingSamples, setIsGeneratingSamples] = useState(false);
   const [generatedSamples, setGeneratedSamples] = useState<Array<{ input: string; expectedOutput: string }>>([]);
   ```

2. **Generate Samples Button**
   - Located in the sample test cases card header
   - Shows loading spinner while generating
   - Disabled during generation
   - Tooltip: "Generate new sample test cases dynamically"

3. **Sample Display Logic**
   ```tsx
   const displayedSamples = generatedSamples.length > 0 ? generatedSamples : (testCases || []);
   ```
   - Prioritizes generated samples over static test cases
   - Falls back to static testCases if no generated samples
   - Shows green "AI Generated" badge when using generated samples

4. **Evaluation Integration**
   ```tsx
   const activeSamples = generatedSamples.length > 0 ? generatedSamples : testCases;
   ```
   - Uses generated samples for evaluation when available
   - Seamlessly switches between generated and static test cases

---

## 🔧 Backend API Requirements

### Endpoint: `POST /exams/generate-samples`

#### Request Body
```json
{
  "questionId": "string",    // The coding question ID
  "language": "string",      // "python" | "java" | "javascript"
  "code": "string"           // Current code (optional, may be empty)
}
```

#### Response (Success - 200)
```json
{
  "samples": [
    {
      "input": "5",
      "expectedOutput": "120"
    },
    {
      "input": "3",
      "expectedOutput": "6"
    },
    {
      "input": "0",
      "expectedOutput": "1"
    }
  ],
  "message": "Generated 3 sample test cases" // Optional
}
```

#### Response (Error - 400/500)
```json
{
  "message": "Failed to generate samples: [reason]"
}
```

---

## 💡 Implementation Strategies

### Strategy 1: AI-Powered Generation (Recommended)
Use an LLM (GPT, Claude, etc.) to generate intelligent samples based on:
- Question description
- Problem difficulty
- Language constraints
- Edge cases

**Example Prompt Template:**
```
Generate 3-5 test cases for this coding problem:
Title: {questionTitle}
Description: {questionDescription}
Language: {language}

Return ONLY JSON array:
[
  {"input": "...", "expectedOutput": "..."},
  ...
]
```

### Strategy 2: Rule-Based Generation
For common problem types:
- **Math problems**: Generate random numbers within constraints
- **String problems**: Create sample strings with edge cases (empty, long, special chars)
- **Array problems**: Generate arrays of varying sizes

### Strategy 3: Template-Based
Pre-define sample generators for problem categories:
- Sorting → unsorted arrays
- Searching → arrays with target values
- Math → numerical inputs

---

## 📊 Expected Behavior

### User Flow
1. User opens coding exam question
2. Sees static sample test cases (if available)
3. Clicks "Generate Samples" button
4. Frontend calls `POST /exams/generate-samples`
5. Backend generates samples using AI/rules
6. Frontend displays generated samples with green "AI Generated" badge
7. User can write code against these samples
8. "Evaluate" button uses generated samples for testing

### Edge Cases Handled
- ✅ No static test cases → shows only generated samples
- ✅ Generation fails → error toast, keeps existing samples
- ✅ Empty response → shows error toast
- ✅ Multiple generations → replaces previous generated samples
- ✅ Evaluation → uses generated samples if available, else static

---

## 🎨 Visual Indicators

### Generated Samples Display
```
┌─────────────────────────────────────────┐
│ Sample Test Cases (Public)  [Generate]  │ ← Button added
├─────────────────────────────────────────┤
│ ✓ AI Generated: 3 sample(s)            │ ← Green alert badge
│                                          │
│ Sample 1                                │
│ Input: 5                                │
│ Expected Output: 120                    │
│ ...                                     │
└─────────────────────────────────────────┘
```

---

## 🧪 Testing Checklist

### Frontend Tests
- [ ] Generate button appears in card header
- [ ] Loading spinner shows during generation
- [ ] Success toast shows: "✓ Samples Generated"
- [ ] Error toast shows on API failure
- [ ] Green badge appears with generated samples
- [ ] Hidden test counter shows correct count
- [ ] Evaluation uses generated samples

### Backend Tests
- [ ] Endpoint returns valid JSON
- [ ] Samples have `input` and `expectedOutput` fields
- [ ] Returns appropriate error messages
- [ ] Handles invalid questionId
- [ ] Handles missing question data
- [ ] Rate limiting (if using external AI APIs)

---

## 📝 Sample Backend Implementation (Pseudocode)

```java
@PostMapping("/exams/generate-samples")
public ResponseEntity<Map<String, Object>> generateSamples(@RequestBody Map<String, String> request) {
    String questionId = request.get("questionId");
    String language = request.get("language");
    String code = request.get("code");
    
    // 1. Fetch question details
    Question question = questionService.findById(questionId);
    if (question == null) {
        return ResponseEntity.badRequest().body(Map.of("message", "Question not found"));
    }
    
    // 2. Generate samples (AI/Rule-based)
    List<Map<String, String>> samples = sampleGeneratorService.generate(
        question.getTitle(),
        question.getDescription(),
        language,
        3 // number of samples
    );
    
    // 3. Return samples
    return ResponseEntity.ok(Map.of(
        "samples", samples,
        "message", "Generated " + samples.size() + " sample test cases"
    ));
}
```

---

## 🔐 Security Considerations

1. **Rate Limiting**: Prevent spam generation requests
2. **Authentication**: Verify examAttemptId/sessionToken
3. **Input Validation**: Sanitize questionId and code inputs
4. **Cost Control**: If using paid AI APIs, implement quotas
5. **Timeout**: Set reasonable timeout for AI generation (5-10 seconds)

---

## 🚀 Future Enhancements

1. **Difficulty Levels**: Generate easy/medium/hard samples
2. **Custom Count**: Allow users to specify number of samples (3, 5, 10)
3. **Sample History**: Cache generated samples per question
4. **Explanation**: Show why each sample was generated
5. **Regenerate Single**: Replace individual samples
6. **Clear Button**: Reset to static test cases

---

## 📖 Related Files

### Frontend
- `frontend/src/components/CodingEditor.tsx` (lines 61-62, 193-223, 571-650)
  - State management
  - API call implementation
  - Display logic updates

### Backend (To Implement)
- `backend/src/main/java/com/recruitment/controller/ExamController.java`
  - Add POST /exams/generate-samples endpoint
- `backend/src/main/java/com/recruitment/service/SampleGeneratorService.java`
  - Create service for sample generation logic

---

## ✅ Current Status

### Completed
- ✅ Frontend state management (isGeneratingSamples, generatedSamples)
- ✅ Generate button UI with loading state
- ✅ API call to POST /exams/generate-samples
- ✅ Display logic (prioritizes generated over static)
- ✅ Green "AI Generated" badge indicator
- ✅ Hidden test counter supports both types
- ✅ Evaluation uses generated samples when available
- ✅ Error handling with toast notifications
- ✅ Seamless fallback to static test cases

### Pending
- ⏳ Backend API endpoint implementation
- ⏳ AI/Rule-based sample generation logic
- ⏳ Database schema (optional: cache samples)
- ⏳ Testing and validation

---

## 🎯 Quick Start for Backend Developer

1. Create endpoint: `POST /exams/generate-samples`
2. Accept: `{ questionId, language, code }`
3. Return: `{ samples: [{ input, expectedOutput }] }`
4. Use AI API (GPT/Claude) or rule-based logic
5. Test with frontend at `localhost:5174`

**Test Request:**
```bash
curl -X POST http://localhost:8080/api/exams/generate-samples \
  -H "Content-Type: application/json" \
  -d '{
    "questionId": "1",
    "language": "python",
    "code": "def factorial(n):\n    return 1"
  }'
```

**Expected Response:**
```json
{
  "samples": [
    {"input": "5", "expectedOutput": "120"},
    {"input": "3", "expectedOutput": "6"},
    {"input": "0", "expectedOutput": "1"}
  ]
}
```

---

## 🐛 Troubleshooting

### Issue: Generate button not appearing
- **Check**: testCases prop is passed to CodingEditor
- **Fix**: Ensure parent component provides testCases (can be empty array)

### Issue: API call fails with 404
- **Check**: Backend endpoint exists at `/api/exams/generate-samples`
- **Fix**: Implement backend endpoint first

### Issue: Samples not displaying
- **Check**: Response format matches `{ samples: [...] }`
- **Fix**: Ensure backend returns correct JSON structure

### Issue: Evaluation still uses static test cases
- **Check**: generatedSamples state is populated
- **Fix**: Verify API response sets generatedSamples correctly

---

**Implementation Date:** January 2025  
**Frontend Status:** ✅ Complete  
**Backend Status:** ⏳ Pending Implementation
