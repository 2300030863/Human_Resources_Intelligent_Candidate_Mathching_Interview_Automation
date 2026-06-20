# Job Application Date Range Feature - Implementation Guide

## Overview
This feature adds **application start date** and **application end date** to job postings, allowing recruiters to control when candidates can apply for positions.

## Backend Changes Completed ✅

### 1. Job Entity Updates
**File**: `src/main/java/com/lernathon/recruitment/entity/Job.java`
- Added `applicationStartDate: LocalDateTime` - When applications can start
- Added `applicationEndDate: LocalDateTime` - When applications will end

### 2. Database Migration
**File**: `add-application-dates-to-jobs.sql`
```sql
ALTER TABLE jobs 
ADD COLUMN IF NOT EXISTS application_start_date DATETIME NULL,
ADD COLUMN IF NOT EXISTS application_end_date DATETIME NULL;
```

**To apply migration:**
```bash
mysql -u root -p12345 recruitment_db < add-application-dates-to-jobs.sql
```

### 3. Job Service Updates
**File**: `src/main/java/com/lernathon/recruitment/service/JobService.java`

New methods added:
- `isApplicationAllowed(Long jobId)` - Check if applications are allowed
- `getApplicationStatusMessage(Long jobId)` - Get user-friendly status message

### 4. Application Service Updates
**File**: `src/main/java/com/lernathon/recruitment/service/ApplicationService.java`

- Added validation in `createApplication()` to check if applications are allowed before creating

### 5. Job Controller Updates
**File**: `src/main/java/com/lernathon/recruitment/controller/JobController.java`

New endpoint added:
```
GET /api/jobs/{id}/application-allowed
```

Response:
```json
{
  "allowed": true,
  "message": "Applications are open"
}
```

## Frontend Changes Needed

### 1. Job Creation/Edit Form
Update the job form component to include date/time pickers:

```jsx
// Add these fields to your job form
<div className="form-group">
  <label>Application Start Date & Time</label>
  <input 
    type="datetime-local" 
    name="applicationStartDate"
    value={job.applicationStartDate}
    onChange={handleChange}
  />
</div>

<div className="form-group">
  <label>Application End Date & Time</label>
  <input 
    type="datetime-local" 
    name="applicationEndDate"
    value={job.applicationEndDate}
    onChange={handleChange}
  />
</div>
```

### 2. Job Details Page
Display the application status to candidates:

```jsx
// In job details component
useEffect(() => {
  const checkApplicationStatus = async () => {
    try {
      const response = await fetch(`/api/jobs/${jobId}/application-allowed`);
      const data = await response.json();
      setApplicationStatus(data);
    } catch (error) {
      console.error('Failed to check application status:', error);
    }
  };
  checkApplicationStatus();
}, [jobId]);

// Render status
<div className="application-status">
  <p>{applicationStatus?.message}</p>
  {applicationStatus?.allowed ? (
    <button onClick={handleApply}>Apply Now</button>
  ) : (
    <button disabled>Applications Closed</button>
  )}
</div>
```

### 3. Job Listing
Show availability status in job cards:

```jsx
// In job card component
const getApplicationStatus = (startDate, endDate) => {
  const now = new Date();
  const start = new Date(startDate);
  const end = new Date(endDate);

  if (now < start) {
    return { status: 'pending', text: `Opens ${start.toLocaleDateString()}` };
  } else if (now > end) {
    return { status: 'closed', text: 'Closed' };
  } else {
    return { status: 'open', text: 'Open' };
  }
};

<div className={`status-badge ${getApplicationStatus(startDate, endDate).status}`}>
  {getApplicationStatus(startDate, endDate).text}
</div>
```

## API Examples

### Create Job with Application Dates
```bash
POST /api/jobs
Content-Type: application/json

{
  "title": "Senior Software Engineer",
  "description": "We are looking for a senior engineer...",
  "applicationStartDate": "2026-06-20T09:00:00",
  "applicationEndDate": "2026-07-04T17:00:00",
  "status": "OPEN"
}
```

### Update Job with Application Dates
```bash
PUT /api/jobs/1
Content-Type: application/json

{
  "title": "Senior Software Engineer",
  "applicationStartDate": "2026-06-20T09:00:00",
  "applicationEndDate": "2026-07-04T17:00:00"
}
```

### Check if Application is Allowed
```bash
GET /api/jobs/1/application-allowed

Response:
{
  "allowed": true,
  "message": "Applications are open"
}
```

### Apply for a Job (with validation)
```bash
POST /api/candidates/1/applications
Content-Type: application/json

{
  "jobId": 1,
  "coverLetter": "I am very interested..."
}
```

## Application Status Messages

| Condition | Message |
|-----------|---------|
| Job not OPEN | "Job is currently closed" |
| Before applicationStartDate | "Applications will start on [date]" |
| After applicationEndDate | "Applications have ended" |
| Within date range & Job OPEN | "Applications are open" |

## Testing Steps

1. **Apply Database Migration**:
   ```bash
   mysql -u root -p12345 recruitment_db < add-application-dates-to-jobs.sql
   ```

2. **Create a Test Job** with application dates:
   - Start Date: Today 9:00 AM
   - End Date: 30 days from now 5:00 PM

3. **Test Application Submission**:
   - ✅ Should succeed during the date range
   - ❌ Should fail before start date
   - ❌ Should fail after end date

4. **Test API Endpoint**:
   ```bash
   curl http://localhost:8089/api/jobs/1/application-allowed
   ```

## Frontend Components to Update

1. **JobForm.jsx** - Add datetime pickers
2. **JobDetails.jsx** - Show application status
3. **JobCard.jsx** - Display availability badge
4. **CreateJob.jsx** - Include new fields
5. **EditJob.jsx** - Include new fields

## Validation Rules

- ✅ `applicationStartDate` must be before `applicationEndDate`
- ✅ `applicationStartDate` and `applicationEndDate` are optional
- ✅ If no dates provided, applications are allowed if job is OPEN
- ✅ Rejected applications show error: "Applications are not allowed for this job: [reason]"

## Error Handling

```json
{
  "error": "Applications are not allowed for this job: Applications will start on 2026-06-20T09:00:00"
}
```

## Notes

- All timestamps are stored as `LocalDateTime` (includes date and time)
- Timezone is set to UTC in `application.yml`
- Times are displayed in user's local timezone on frontend
- Recruiters can update application dates anytime
- Existing jobs without dates will allow applications if status is OPEN

## Future Enhancements

- Auto-close job applications at specified end date
- Send email notifications before applications close
- Show countdown timer on job listing
- Bulk import application dates from CSV
- Recurring job postings with auto-date calculation
