# Troubleshooting: Skills Auto-Fill Not Working

## Current Status Check

✅ **AI Service**: Running on http://localhost:5000  
✅ **Backend**: Running on http://localhost:8089  
✅ **API Test**: Passed - AI extracted 33 skills successfully  

## Issue: Skills not auto-filling in frontend

### Step-by-Step Debugging

#### 1. Verify All Services Are Running

```powershell
# Check AI Service (port 5000)
netstat -ano | findstr :5000

# Check Backend (port 8089)
netstat -ano | findstr :8089

# Check Frontend (port 5173)
netstat -ano | findstr :5173
```

**Expected**: All three ports should show LISTENING

#### 2. Test Backend Connection

Open browser and go to:
```
http://localhost:8089/api/candidates
```

**Expected**: JSON response (even if empty array `[]`)
**If you get an error**: Backend is not running properly

#### 3. Test AI Service Connection

Open browser and go to:
```
http://localhost:5000/health
```

**Expected**: 
```json
{
  "status": "healthy",
  "service": "AI Resume Skill Extraction API"
}
```

#### 4. Check Browser Console

1. Open your frontend (http://localhost:5173)
2. Press **F12** to open Developer Tools  
3. Go to **Console** tab
4. Try uploading a resume
5. Look for errors (they will be in RED)

**Common errors to look for:**
- ❌ `Network Error` - Backend not running
- ❌ `CORS Error` - Backend CORS not configured
- ❌ `404 Not Found` - Wrong endpoint URL
- ❌ `500 Internal Server Error` - Backend error

#### 5. Check Network Tab

1. In Developer Tools, go to **Network** tab
2. Upload a resume on the job application form
3. Look for a request to `/api/candidates/ai-screening`
4. Click on it and check:
   - **Status**: Should be `200 OK`
   - **Response**: Should contain skills array
   - **Preview**: Check the JSON response

#### 6. Test Direct Backend API Call

Open PowerShell and run:

```powershell
# Create a simple test
$file = Get-Item "c:\project\dl project\lernathon\test-sample-resume.txt"
$uri = "http://localhost:8089/api/candidates/ai-screening"

# This should work if backend is configured correctly
Invoke-WebRequest -Uri http://localhost:8089/api/candidates
```

#### 7. Check Backend Logs

Look at the backend terminal for errors. You should see:
```
POST /api/candidates/ai-screening
```

**If you see errors like:**
- `Connection refused` → AI service not running
- `UnknownHostException` → Wrong AI service URL
- `JSON parse error` → AI response format issue

## Quick Fixes

### Fix 1: Restart All Services

```powershell
# Terminal 1 - AI Service
cd "c:\project\dl project\lernathon\AI-Resume-Analyzer"
python api_service.py

# Terminal 2 - Backend  
cd "c:\project\dl project\lernathon\backend"
mvn spring-boot:run

# Terminal 3 - Frontend
cd "c:\project\dl project\lernathon\frontend"
npm run dev
```

### Fix 2: Clear Browser Cache

1. Press **Ctrl+Shift+Delete**
2. Clear cached images and files
3. Reload page (Ctrl+F5)

### Fix 3: Check Authentication

The `/api/candidates/ai-screening` endpoint might require authentication.

**Test if you're logged in:**
1. Check localStorage in browser console:
   ```javascript
   localStorage.getItem('auth_token')
   ```
2. If null, you need to log in first
3. Go to login page and sign in

### Fix 4: Verify File Upload

Make sure you're uploading a valid file:
- ✅ Supported formats: PDF, DOC, DOCX, TXT
- ✅ File size: Less than 5MB
- ✅ File contains readable text

## Test with Sample Resume

Use the test file created:
```
c:\project\dl project\lernathon\test-sample-resume.txt
```

This file should extract skills:
- Java
- Spring Boot
- React
- TypeScript
- PostgreSQL
- Docker
- Kubernetes
- AWS

## Expected Behavior

When you upload a resume, you should see:

1. **Loading State** (2-5 seconds):
   - Spinner with "AI is analyzing your resume..."
   
2. **Success State**:
   - ✨ Toast notification: "Profile Auto-Created!"
   - Skills field auto-filled with comma-separated skills
   - Green badge: "Profile auto-created (ID: 123)"
   - Phone number filled (if detected)
   - Experience years filled (if detected)

## Still Not Working?

### Check these common issues:

1. **Wrong API endpoint in frontend**
   - Check `frontend/src/lib/api-client.ts`
   - Should be `http://localhost:8089/api` or your host:8089/api

2. **Backend not calling AI service**
   - Check `backend/src/main/resources/application.yml`
   - Look for: `ai.matching.service.url: http://localhost:5000`
   - Make sure `enabled: true`

3. **CORS blocking requests**
   - Backend should allow frontend origin
   - Check application.yml for `cors.allowed-origins`

4. **Database not connected**
   - Backend needs MySQL running
   - Check credentials in application.yml

## Debug Mode

Enable verbose logging:

**In api_service.py**, the AI service already logs:
- ✅ Every request received
- ✅ AI responses
- ✅ Extracted skills count

**Check the Python terminal** where api_service.py is running for logs like:
```
AI Response: ["Java", "React", ...]
✅ Extracted 10 skills from resume
127.0.0.1 - - [14/Feb/2026 10:30:45] "POST /extract-skills HTTP/1.1" 200 -
```

## Need More Help?

Share:
1. Browser console errors (F12 > Console tab)
2. Network tab response for `/ai-screening` request
3. Backend terminal output
4. AI service terminal output

---

**Created**: February 14, 2026  
**Last Updated**: February 14, 2026
