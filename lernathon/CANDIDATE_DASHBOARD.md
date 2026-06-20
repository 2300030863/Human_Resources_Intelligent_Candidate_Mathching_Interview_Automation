# Candidate Dashboard

## Overview
The Candidate Dashboard provides job seekers with a personalized view of their job application journey.

## Features Implemented

### 📊 Statistics Cards
- **Active Applications** - Track ongoing applications
- **Upcoming Interviews** - See scheduled interviews
- **Jobs Available** - Browse matching opportunities
- **Success Rate** - Monitor application performance

### 📝 Recent Applications
- View recent job applications
- Track application status (SUBMITTED, UNDER_REVIEW, SCREENING, INTERVIEWING, OFFERED, ACCEPTED, REJECTED, WITHDRAWN)
- See application dates
- Color-coded status badges

### 📅 Upcoming Interviews
- View scheduled interviews
- See interview type (PHONE_SCREEN, VIDEO, TECHNICAL, etc.)
- Check date and time
- Quick access to interview details

### 💼 Recommended Jobs
- Browse available job openings
- View job details (title, department, location, employment type)
- One-click apply functionality
- Filter by profile match

## User Experience

### For Candidates
- Clean, simple interface focused on job search
- Easy tracking of application progress
- Quick access to interview schedules
- Direct job application flow

### Access Control
- Only visible to users with CANDIDATE role
- Recruiters and Admins see different dashboard views
- Role-based menu filtering ensures candidates only see relevant sections

## Test Account
- **Email**: candidate@lernathon.com
- **Password**: candidate123
- **Role**: CANDIDATE

## Navigation
Candidates can access:
- ✅ Dashboard - Application tracking
- ✅ Jobs - Browse and apply
- ✅ Interviews - Schedule view
- ✅ Settings - Profile management

Hidden from candidates:
- ❌ Candidates list
- ❌ AI Matching
- ❌ AI Screening
- ❌ Reports

## Technical Implementation
- Component: `/frontend/src/components/dashboard/CandidateDashboard.tsx`
- Role detection in `/frontend/src/pages/Dashboard.tsx`
- Uses real-time API data from backend services
- Responsive design for all screen sizes
