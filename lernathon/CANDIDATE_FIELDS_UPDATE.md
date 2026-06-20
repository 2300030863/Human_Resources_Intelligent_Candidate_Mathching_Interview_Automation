# Candidate Experience & Skills Update

## Problem
When viewing candidates in the Candidates page, the "Experience" and "Skills" columns were showing empty values because:
- Candidates auto-created during job applications had skills set to empty string (`""`)
- Experience years was being set to `0`
- The UI was not handling these empty/default values well

## Solution Implemented

### Frontend Changes (ApplyJobs.tsx)

**Added new form fields to job application:**
1. **Years of Experience** - Required number input field (0-50 years)
2. **Skills** - Required textarea for comma-separated skills

**Form State:**
```javascript
const [skills, setSkills] = useState("");
const [experienceYears, setExperienceYears] = useState("");
```

**Validation:**
- Both fields are now required for job application
- Submit button is disabled unless all fields including skills and experience are filled

**Form Data:**
```javascript
formData.append('skills', skills);
formData.append('experienceYears', experienceYears);
```

### Backend Changes

**ApplicationController.java**
- Added `skills` parameter (String, optional)
- Added `experienceYears` parameter (Integer, optional)

```java
@PostMapping("/submit")
public ResponseEntity<Application> submitApplication(
    @RequestParam Long jobId,
    @RequestParam("resume") MultipartFile resume,
    @RequestParam(required = false) String phone,
    @RequestParam(required = false) String skills,              // NEW
    @RequestParam(required = false) Integer experienceYears,    // NEW
    @RequestParam(required = false) String coverLetter,
    Authentication authentication
)
```

**ApplicationService.java**
- Updated method signature to accept skills and experienceYears
- When creating new candidate: Sets skills and experience from form data
- When updating existing candidate: Updates skills and experience if provided

```java
// Create new candidate with skills and experience
candidate = Candidate.builder()
    .email(user.getEmail())
    .firstName(user.getFirstName())
    .lastName(user.getLastName())
    .phone(phone != null ? phone : "")
    .skills(skills != null && !skills.isEmpty() ? skills : "")
    .experienceYears(experienceYears != null && experienceYears > 0 ? experienceYears : 0)
    .education("")
    .build();

// Update existing candidate
if (skills != null && !skills.isEmpty()) {
    candidate.setSkills(skills);
}
if (experienceYears != null && experienceYears > 0) {
    candidate.setExperienceYears(experienceYears);
}
```

### UI Display Changes (Candidates.tsx)

**Better handling of empty values:**
- Shows "Not specified" instead of "-" or empty when data is missing
- Checks for `experienceYears > 0` to differentiate between actual 0 and unset values
- Checks for non-empty skills string before displaying

```javascript
{candidate.experienceYears && candidate.experienceYears > 0 
  ? `${candidate.experienceYears} years` 
  : <span className="text-muted-foreground">Not specified</span>}

{candidate.skills && candidate.skills.trim().length > 0 ? (
  // Display skill badges
) : (
  <span className="text-xs text-muted-foreground">Not specified</span>
)}
```

## Result

Now when candidates apply for jobs:
1. They are prompted to enter their years of experience (required)
2. They must list their skills separated by commas (required)
3. This data is saved to their candidate profile
4. The Candidates page properly displays this information
5. Empty/missing values show as "Not specified" instead of blank

## Testing

To test the changes:
1. Login as a candidate (candidate@lernathon.com / candidate123)
2. Navigate to "Apply Jobs"
3. Click "Apply Now" on any job
4. Fill in:
   - Phone number
   - Years of experience (e.g., 5)
   - Skills (e.g., Java, Spring Boot, React, MySQL)
   - Upload resume
5. Submit application
6. Login as admin/recruiter
7. Navigate to "Candidates"
8. Verify the candidate's experience and skills are now displayed

## Notes

- Skills should be entered as comma-separated values (e.g., "Java, Python, React")
- Experience years must be a positive number
- Both fields are required for new applications
- Existing candidates' data will be updated if they apply for new jobs
- The AI matching service uses these skills for calculating match scores
