# FIXING 403 ERRORS - Authentication Setup Guide ✅

## Issues Fixed

### 1. **Frontend API Client Enhanced**
- Updated to handle both 401 and 403 errors
- Automatically redirects to login page when authentication fails
- File: `frontend/src/lib/api-client.ts`

### 2. **Backend Security Configuration Updated**
- Made exam endpoints publicly accessible (they use session tokens)
- Exam endpoints don't require JWT authentication
- File: `backend/src/main/java/com/lernathon/recruitment/config/SecurityConfig.java`

### 3. **Test User Created**
- Script created at: `backend/create-test-user.sql`
- Ready to create test users for development

## The Root Cause

The 403 errors occurred because:
1. **User not logged in** - The dashboard requires authentication
2. **No JWT token** - API requests need a valid JWT token in the Authorization header
3. **Protected routes** - All endpoints except `/auth/**` and `/health` require authentication

## How to Fix and Test

### Step 1: Register/Login to the Application

#### Option A: Use the Web UI (Recommended)
1. Open your browser to the frontend URL (usually `http://localhost:5173`)
2. Navigate to `/auth` or click the login link
3. **Register a new account:**
   - Email: `your-email@example.com`
   - Password: anything you want
   - First Name: Your first name
   - Last Name: Your last name
   - Role will default to CANDIDATE

4. **Or use the test credentials:**
   - Email: `candidate@test.com`
   - Password: `password123`
   - (If you ran the SQL script)

#### Option B: Register via API
```powershell
$registerBody = @{
    email = "test@example.com"
    password = "password123"
    firstName = "Test"
    lastName = "User"
    role = "CANDIDATE"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8089/api/auth/register" -Method Post -Body $registerBody -ContentType "application/json"
```

### Step 2: Login and Get Token
```powershell
$loginBody = @{
    email = "test@example.com"
    password = "password123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8089/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"

Write-Host "Token: $($response.token)"
Write-Host "User: $($response.firstName) $($response.lastName) ($($response.role))"
```

### Step 3: Access the Dashboard
1. Go to `http://localhost:5173/auth`
2. Login with your credentials
3. You should be redirected to `/dashboard`
4. The dashboard will now load data successfully

## What Changed in the Code

### Frontend: `api-client.ts`
```typescript
// Before: Only handled 401
if (error.response?.status === 401) { ... }

// After: Handles both 401 and 403
if (error.response?.status === 401 || error.response?.status === 403) {
  localStorage.removeItem('auth_token');
  localStorage.removeItem('user');
  window.location.href = '/auth';
}
```

### Backend: `SecurityConfig.java`
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**", "/health").permitAll()
    // Added exam endpoints as public (they use session tokens)
    .requestMatchers("/exams/*/start", "/exams/submit-answer", ...).permitAll()
    .anyRequest().authenticated()
)
```

## Authentication Flow

```
┌─────────────────────────────────────────┐
│ 1. User visits /dashboard              │
│    (Protected Route)                    │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ 2. ProtectedRoute checks if logged in  │
│    - Looks for 'auth_token' in          │
│      localStorage                       │
└────────────────┬────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
  [Has Token]      [No Token]
        │                 │
        │                 └──> Redirect to /auth
        │
        ▼
┌─────────────────────────────────────────┐
│ 3. Dashboard loads                      │
│    - Makes API call to /api/applications│
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ 4. API Client intercepts request       │
│    - Adds: Authorization: Bearer <token>│
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ 5. Backend JwtAuthenticationFilter      │
│    - Validates token                    │
│    - Sets SecurityContext               │
└────────────────┬────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
  [Valid Token]    [Invalid Token]
        │                 │
        │                 └──> 403 Forbidden
        │                      (Redirects to /auth)
        ▼
┌─────────────────────────────────────────┐
│ 6. Request succeeds                     │
│    - Data returned to frontend          │
│    - Dashboard displays properly        │
└─────────────────────────────────────────┘
```

## Testing the Fix

### 1. Clear Browser Data (Important!)
```javascript
// Open browser console and run:
localStorage.clear();
// Then refresh the page
```

### 2. Test Authentication Flow
1. Navigate to `/auth`
2. Register or login
3. Should redirect to `/dashboard`
4. Dashboard should load without 403 errors

### 3. Test API Calls
Once logged in, API calls should work:
- `/api/applications` - ✅ Should return applications
- `/api/interviews` - ✅ Should return interviews  
- `/api/jobs` - ✅ Should return jobs
- `/api/exams/*` - ✅ Public (no auth needed)

### 4. Test Exam Flow (No Auth Required)
Exam endpoints work without login:
- Generate exam
- Start exam (uses session token)
- Submit answers
- Complete exam

## Common Issues & Solutions

### Issue: Still getting 403 after login
**Solution:** 
- Clear browser cache and localStorage
- Check browser console for token: `localStorage.getItem('auth_token')`
- Try logging out and logging in again

### Issue: Token not being sent
**Solution:**
- Check Network tab in browser DevTools
- Look for "Authorization" header in request
- Should see: `Bearer eyJhbGc...`

### Issue: "Invalid token" errors
**Solution:**
- Token may have expired
- Logout and login again
- Check backend logs for JWT validation errors

## Security Notes

### JWT Token Storage
- Tokens stored in localStorage
- Automatically added to all API requests
- Removed on 401/403 errors

### Token Expiration
- Tokens expire after configured time
- On expiration, user redirected to login
- User must login again to get new token

### Exam Session Tokens
- Separate from JWT tokens
- Used for exam-specific authentication
- Prevent unauthorized exam access

## Next Steps

1. **Login to the application** using the web UI or API
2. **Access the dashboard** - You should no longer see 403 errors
3. **Test the exam flow** - Now works with automatic sample input
4. **Create more test data** if needed (jobs, candidates, etc.)

## Summary

✅ Frontend now handles 403 errors and redirects to login  
✅ Backend security properly configured  
✅ Exam endpoints publicly accessible (use session tokens)  
✅ Test user creation script available  
✅ Authentication flow documented

**To test your setup:**
1. Go to `http://localhost:5173/auth`
2. Register or login
3. Navigate to dashboard
4. Data should load without errors! 🎉
