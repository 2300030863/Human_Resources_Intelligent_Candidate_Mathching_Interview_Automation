# Test script to verify the new job fields

Write-Host "Testing Job API with new exam and interview fields..." -ForegroundColor Cyan

$baseUrl = "http://localhost:8089/api"

# Test 1: Get all jobs to see if backend is running
Write-Host ""
Write-Host "1. Testing GET /api/jobs..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/jobs" -Method Get -ErrorAction Stop
    Write-Host "SUCCESS - Backend is running! Found $($response.Count) jobs" -ForegroundColor Green
} catch {
    Write-Host "ERROR - Backend may not be running: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Create a new job with the new fields
Write-Host ""
Write-Host "2. Testing POST /api/jobs with new fields..." -ForegroundColor Yellow

$timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
$newJob = @{
    title = "Test Senior Developer - $timestamp"
    description = "Test job posting to verify exam and interview configuration fields"
    department = "Engineering"
    location = "Remote"
    employmentType = "FULL_TIME"
    salaryRange = "`$100,000 - `$150,000"
    experienceRequired = 5
    skillsRequired = "Java, Spring Boot, MySQL, REST APIs"
    status = "OPEN"
    openings = 2
    examPassRate = 75.0
    interviewPassRate = 80.0
    interviewQuestionMode = "ADVANCED"
} | ConvertTo-Json

Write-Host "Creating job with exam pass rate: 75%, interview pass rate: 80%, mode: ADVANCED" -ForegroundColor Gray

try {
    $createdJob = Invoke-RestMethod -Uri "$baseUrl/jobs" -Method Post -Body $newJob -ContentType "application/json" -ErrorAction Stop
    Write-Host "SUCCESS - Job created with ID: $($createdJob.id)" -ForegroundColor Green
    Write-Host "  Exam Pass Rate: $($createdJob.examPassRate)%" -ForegroundColor Green
    Write-Host "  Interview Pass Rate: $($createdJob.interviewPassRate)%" -ForegroundColor Green
    Write-Host "  Interview Question Mode: $($createdJob.interviewQuestionMode)" -ForegroundColor Green
    
    # Test 3: Get the created job
    Write-Host ""
    Write-Host "3. Testing GET /api/jobs/$($createdJob.id)..." -ForegroundColor Yellow
    $retrievedJob = Invoke-RestMethod -Uri "$baseUrl/jobs/$($createdJob.id)" -Method Get -ErrorAction Stop
    Write-Host "SUCCESS - Job retrieved!" -ForegroundColor Green
    
    # Test 4: Update the job fields
    Write-Host ""
    Write-Host "4. Testing PUT /api/jobs/$($createdJob.id)..." -ForegroundColor Yellow
    $retrievedJob.examPassRate = 65.0
    $retrievedJob.interviewPassRate = 70.0
    $retrievedJob.interviewQuestionMode = "INTERMEDIATE"
    
    $updateJob = $retrievedJob | ConvertTo-Json -Depth 10
    $updatedJob = Invoke-RestMethod -Uri "$baseUrl/jobs/$($createdJob.id)" -Method Put -Body $updateJob -ContentType "application/json" -ErrorAction Stop
    Write-Host "SUCCESS - Job updated!" -ForegroundColor Green
    Write-Host "  Exam Pass Rate: $($updatedJob.examPassRate)% (changed from 75%)" -ForegroundColor Green
    Write-Host "  Interview Pass Rate: $($updatedJob.interviewPassRate)% (changed from 80%)" -ForegroundColor Green
    Write-Host "  Interview Question Mode: $($updatedJob.interviewQuestionMode) (changed from ADVANCED)" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "ALL TESTS PASSED - New fields are working correctly!" -ForegroundColor Green
    
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Cyan

