# Duplicate Candidate Fix & Manual Input Priority

## Problem Statement
Candidates were being created as duplicates when:
1. Same person uploaded resume multiple times with different email addresses
2. Phone numbers appeared in different formats (e.g., `2300030863` vs `+91 2300030863`)
3. AI-extracted data was overwriting manually entered information

## Solution Implemented

### 1. **Added Name Fields to Application Form**
- Added `firstName` and `lastName` fields to the job application form
- Candidates can now enter their correct name manually
- AI-extracted names from resumes can auto-fill these fields if empty

### 2. **Phone Number Priority System**
- Implemented `phoneManuallyEntered` flag to track manual input
- When user manually enters a phone number, it's marked as priority
- AI-extracted phone numbers will NOT overwrite manually entered ones
- Visual feedback shows whether phone was manually entered or auto-filled

### 3. **Prevent Email-Based Duplicates**
- AI screening now uses authenticated user's email (not resume email)
- Candidate lookup always uses the logged-in user's email
- This prevents creating multiple profiles for the same person with different emails

### 4. **Smart Auto-Fill Logic**
Changed auto-fill behavior to be additive, not destructive:
- ✅ Auto-fills empty fields from resume
- ❌ Does NOT overwrite manually entered values
- ✅ Preserves user's explicit inputs

## Technical Changes

### Frontend Changes (`ApplyJobs.tsx`)

#### Added State Variables
```typescript
const [firstName, setFirstName] = useState("");
const [lastName, setLastName] = useState("");
const [phoneManuallyEntered, setPhoneManuallyEntered] = useState(false);
```

#### Updated Auto-Fill Logic
```typescript
// Auto-fill name if not already entered
if (response.name && !firstName && !lastName) {
  const nameParts = response.name.split(" ");
  setFirstName(nameParts[0] || "");
  setLastName(nameParts.slice(1).join(" ") || "");
}

// Auto-fill phone ONLY if user hasn't manually entered it
if (response.phone && !phoneManuallyEntered && !phoneNumber) {
  // Parse and set phone number
  setPhoneNumber(response.phone.replace(/[^\d]/g, ''));
  setPhone(response.phone);
}
```

#### Phone Input with Manual Flag
```typescript
<Input
  onChange={(e) => {
    const value = e.target.value.replace(/[^\d]/g, '');
    setPhoneNumber(value);
    setPhoneManuallyEntered(true); // Mark as manually entered
  }}
/>
```

#### Visual Feedback
```typescript
<p className="text-xs text-muted-foreground">
  {phoneManuallyEntered ? 
    "✓ Manually entered (won't be overridden by resume)" : 
    "Enter manually or auto-filled from resume"}
</p>
```

### Backend Changes

#### ApplicationController.java
Added `firstName` and `lastName` parameters:
```java
@PostMapping("/submit")
public ResponseEntity<Application> submitApplication(
    @RequestParam Long jobId,
    @RequestParam("resume") MultipartFile resume,
    @RequestParam(required = false) String firstName,
    @RequestParam(required = false) String lastName,
    @RequestParam(required = false) String phone,
    // ... other params
    Authentication authentication
) {
    String userEmail = authentication.getName();
    return ResponseEntity.ok(applicationService.submitApplicationWithResume(
        userEmail, jobId, resume, firstName, lastName, phone, skills, experienceYears, coverLetter
    ));
}
```

#### ApplicationService.java
Updated to handle name fields with priority logic:
```java
public Application submitApplicationWithResume(
    String userEmail, Long jobId, MultipartFile resume, 
    String firstName, String lastName, String phone, 
    String skills, Integer experienceYears, String coverLetter) {
    
    Candidate candidate = candidateService.getCandidateByEmail(userEmail);
    
    if (candidate == null) {
        // Create new candidate
        candidate = Candidate.builder()
            .email(user.getEmail())
            .firstName(firstName != null && !firstName.isEmpty() ? firstName : user.getFirstName())
            .lastName(lastName != null && !lastName.isEmpty() ? lastName : user.getLastName())
            .phone(phone != null ? phone : "")
            .skills(skills != null && !skills.isEmpty() ? skills : "")
            .experienceYears(experienceYears != null && experienceYears > 0 ? experienceYears : 0)
            .build();
        candidate = candidateService.createCandidate(candidate);
    } else {
        // Update candidate - manually entered values take priority
        boolean updated = false;
        if (firstName != null && !firstName.isEmpty()) {
            candidate.setFirstName(firstName);
            updated = true;
        }
        if (lastName != null && !lastName.isEmpty()) {
            candidate.setLastName(lastName);
            updated = true;
        }
        if (phone != null && !phone.isEmpty()) {
            candidate.setPhone(phone);
            updated = true;
        }
        // ... update other fields
        if (updated) {
            candidate = candidateService.updateCandidate(candidate.getId(), candidate);
        }
    }
    // ... rest of application creation
}
```

#### CandidateController.java - AI Screening
Fixed to use authenticated user's email:
```java
@PostMapping("/ai-screening")
public ResponseEntity<AIResumeScreeningResponse> aiResumeScreening(
    @RequestParam("file") MultipartFile file,
    Authentication authentication) {
    
    // Get authenticated user's email (NOT resume email)
    String userEmail = authentication.getName();
    
    // Parse resume
    ResumeParserService.ParsedResume parsed = resumeParserService.parseResume(file);
    
    // Check existing candidate using user's email
    Candidate existingCandidate = candidateService.getCandidateByEmail(userEmail);
    
    // Preserve existing values if present
    String finalFirstName = (existingCandidate != null && existingCandidate.getFirstName() != null) 
        ? existingCandidate.getFirstName() : parsedFirstName;
    String finalPhone = (existingCandidate != null && existingCandidate.getPhone() != null) 
        ? existingCandidate.getPhone() : parsed.getPhone();
    
    // Create/update using authenticated email
    Candidate candidate = candidateService.createOrUpdateFromResume(
        finalFirstName, finalLastName, userEmail, // Use userEmail, not resume email
        finalPhone, parsed.getSkills(), finalExperience, finalEducation, parsed.getRawContent()
    );
    
    // Return parsed data for auto-fill (frontend decides whether to use it)
    return ResponseEntity.ok(response);
}
```

## Benefits

### ✅ No More Duplicates
- One candidate profile per user account
- Same email = same candidate, regardless of resume content

### ✅ Manual Input Priority
- User's explicit entries always take precedence
- AI suggestions enhance UX without being intrusive

### ✅ Better Data Quality
- Candidates can correct their names
- Phone numbers remain consistent
- Skills can be refined manually

### ✅ Improved User Experience
- Visual feedback on which fields are manual vs auto-filled
- Transparency in data handling
- No unexpected data overwrites

## Testing Scenarios

### Test 1: Manual Entry Priority
1. Enter phone number manually: `9876543210`
2. Upload resume with phone: `+91 1234567890`
3. ✅ Manual phone (`9876543210`) is preserved
4. ❌ Resume phone is ignored

### Test 2: Auto-Fill Empty Fields
1. Leave name fields empty
2. Upload resume with name: "John Doe"
3. ✅ Name auto-filled: First="John", Last="Doe"
4. User can still edit these fields

### Test 3: No Duplicates
1. Login as user1@example.com
2. Upload resume with email: user1-old@example.com
3. ✅ Candidate created with user1@example.com
4. Upload another resume with different email
5. ✅ Same candidate updated (no duplicate created)

### Test 4: Phone Format Consistency
1. Manual entry: `2300030863` with country code `+91`
2. Full phone stored: `+91 2300030863`
3. Upload resume with phone: `2300030863`
4. ✅ Manual entry preserved in database

## Migration Notes

### For Existing Duplicate Candidates
If you already have duplicate candidates in the database:

1. **Identify duplicates:**
   ```sql
   SELECT email, COUNT(*) 
   FROM candidates 
   GROUP BY email 
   HAVING COUNT(*) > 1;
   ```

2. **Merge duplicates manually:**
   - Keep the candidate with the most complete information
   - Update applications to reference the kept candidate
   - Delete duplicate records

3. **Going forward:**
   - New system prevents duplicates automatically
   - Manual entries always take priority

## Configuration

No configuration changes needed. The system automatically:
- Uses authenticated user's email for all candidate operations
- Respects manual input flags
- Preserves existing data when updating

## Deployment Steps

1. **Rebuild backend:**
   ```bash
   cd backend
   mvn clean package -DskipTests
   ```

2. **Restart backend service:**
   ```bash
   java -jar target/recruitment-backend-1.0.0.jar
   ```

3. **Frontend auto-updates:**
   - Vite dev server hot-reloads changes automatically
   - No manual restart needed for development

4. **Verify fixes:**
   - Test manual phone entry
   - Test resume upload with different email
   - Confirm no duplicates created
   - Check visual feedback for manual vs auto-filled

## Summary

The duplicate candidate issue has been resolved by:
1. ✅ **Using authenticated email** instead of resume email for candidate lookup
2. ✅ **Adding name fields** to the application form
3. ✅ **Implementing manual input priority** with `phoneManuallyEntered` flag
4. ✅ **Smart auto-fill logic** that enhances without overwriting
5. ✅ **Visual feedback** showing data source (manual vs auto-filled)

All changes are backward compatible and don't require database schema changes.
