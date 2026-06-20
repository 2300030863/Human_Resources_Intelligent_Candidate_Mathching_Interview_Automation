# Test script to verify QuestionDTO includes testCases field
Write-Host "`n=== Testing Backend API Question Structure ===" -ForegroundColor Cyan

# This will show if the backend is returning testCases
Write-Host "`nTo verify the backend returns testCases in QuestionDTO:" -ForegroundColor Yellow
Write-Host "1. Generate a NEW exam from the frontend" -ForegroundColor White
Write-Host "2. Open Browser DevTools (F12) → Network tab" -ForegroundColor White  
Write-Host "3. Look for the /api/exam/generate request" -ForegroundColor White
Write-Host "4. Check the Response - you should see:" -ForegroundColor White
Write-Host "   {" -ForegroundColor Green
Write-Host "     'questions': [" -ForegroundColor Green
Write-Host "       {" -ForegroundColor Green
Write-Host "         'id': 123," -ForegroundColor Green
Write-Host "         'question': '...'," -ForegroundColor Green
Write-Host "         'testCases': [" -ForegroundColor Cyan
Write-Host "           {'input': '...', 'expectedOutput': '...'}" -ForegroundColor Cyan
Write-Host "         ]" -ForegroundColor Cyan
Write-Host "       }" -ForegroundColor Green
Write-Host "     ]" -ForegroundColor Green
Write-Host "   }" -ForegroundColor Green

Write-Host "`n Backend Status:" -ForegroundColor Yellow
$backendRunning = Test-NetConnection -ComputerName localhost -Port 8089 -InformationLevel Quiet -WarningAction SilentlyContinue
if ($backendRunning) {
    Write-Host " ✓ Backend running on port 8089" -ForegroundColor Green
} else {
    Write-Host " ✗ Backend NOT running on port 8089" -ForegroundColor Red
}

Write-Host "`n"
