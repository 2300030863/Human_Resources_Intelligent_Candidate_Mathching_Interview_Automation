package com.lernathon.recruitment.service;

import com.lernathon.recruitment.entity.Application;
import com.lernathon.recruitment.entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' hh:mm a");

    // ─────────────────────────────────────────────
    // 1. New Job Posted → email all registered candidates
    // ─────────────────────────────────────────────
    @Async
    public void notifyJobPosted(Job job, List<String> candidateEmails) {
        String lastDate = job.getLastDate() != null
                ? job.getLastDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "Check the portal";

        for (String email : candidateEmails) {
            try {
                send(email,
                        "New Job Opportunity Available – " + job.getTitle(),
                        "Hello,\n\n" +
                        "A new job opportunity has been posted:\n\n" +
                        "Role       : " + job.getTitle() + "\n" +
                        "Department : " + nvl(job.getDepartment()) + "\n" +
                        "Location   : " + nvl(job.getLocation()) + "\n" +
                        "Apply By   : " + lastDate + "\n\n" +
                        "Log in to HireAI to view details and apply.\n\n" +
                        "Best regards,\nHireAI Team");
            } catch (Exception e) {
                log.error("Failed to send job-posted email to {}: {}", email, e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────
    // 2. Application Submitted Confirmation
    // ─────────────────────────────────────────────
    @Async
    public void notifyApplicationSubmitted(String email, String candidateName, String jobTitle) {
        try {
            send(email,
                    "Application Submitted – " + jobTitle,
                    "Dear " + candidateName + ",\n\n" +
                    "Your application for the position of " + jobTitle + " has been received successfully.\n\n" +
                    "Application Status: APPLIED\n\n" +
                    "We will review your profile and notify you of the next steps.\n\n" +
                    "Best regards,\nHireAI Team");
        } catch (Exception e) {
            log.error("Failed to send application-submitted email to {}: {}", email, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 3. Status Update Notifications
    // ─────────────────────────────────────────────
    @Async
    public void notifyStatusUpdate(Application application) {
        String email = application.getCandidate().getEmail();
        String name  = application.getCandidate().getFirstName();
        String job   = application.getJob().getTitle();
        Application.ApplicationStatus status = application.getStatus();

        try {
            switch (status) {
                case EXAM_ELIGIBLE -> send(email,
                        "You Are Eligible for the Online Exam – " + job,
                        "Dear " + name + ",\n\n" +
                        "Congratulations! You have been shortlisted for the online exam for " + job + ".\n\n" +
                        "Please log in to HireAI to prepare and await further instructions.\n\n" +
                        "Best regards,\nHireAI Team");

                case EXAM_PASSED -> send(email,
                        "Exam Results – " + job,
                        "Dear " + name + ",\n\n" +
                        "Congratulations! You have passed the online exam for " + job + ".\n" +
                        "Score: " + nvl(application.getExamScore()) + "/100\n\n" +
                        "You are now eligible for the interview round.\n\n" +
                        "Best regards,\nHireAI Team");

                case EXAM_FAILED -> send(email,
                        "Exam Results – " + job,
                        "Dear " + name + ",\n\n" +
                        "Thank you for attending the exam for " + job + ".\n" +
                        "Score: " + nvl(application.getExamScore()) + "/100\n\n" +
                        "Unfortunately, you did not qualify this time. " +
                        "We encourage you to apply for future opportunities.\n\n" +
                        "Best regards,\nHireAI Team");

                case INTERVIEW_SCHEDULED -> {
                    String dateStr = application.getInterviewDate() != null
                            ? application.getInterviewDate().format(FMT)
                            : "Please check the portal";
                    send(email,
                            "Interview Scheduled – " + job,
                            "Dear " + name + ",\n\n" +
                            "Your interview for " + job + " has been scheduled.\n\n" +
                            "Interview Date: " + dateStr + "\n\n" +
                            "Please be available at the scheduled time. " +
                            "Further details will be shared separately.\n\n" +
                            "Best regards,\nHireAI Team");
                }

                case SELECTED -> send(email,
                        "Congratulations! You Are Selected – " + job,
                        "Dear " + name + ",\n\n" +
                        "We are thrilled to inform you that you have been SELECTED for the position of " + job + "!\n\n" +
                        "Our HR team will contact you shortly with next steps and the offer details.\n\n" +
                        "Best regards,\nHireAI Team");

                case REJECTED -> send(email,
                        "Application Update – " + job,
                        "Dear " + name + ",\n\n" +
                        "Thank you for your interest in " + job + " and for attending the interview.\n\n" +
                        "After careful consideration, we will not be moving forward with your application at this time. " +
                        "We will keep your profile on file for future opportunities.\n\n" +
                        "Best regards,\nHireAI Team");

                default -> log.debug("No notification template for status: {}", status);
            }
        } catch (Exception e) {
            log.error("Failed to send status-update email to {} for status {}: {}", email, status, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 4. Rejected by AI Score (match < 80%)
    // ─────────────────────────────────────────────
    @Async
    public void notifyRejectedByAIScore(String email, String candidateName, String jobTitle, double matchScore) {
        try {
            send(email,
                    "Application Update – " + jobTitle,
                    "Dear " + candidateName + ",\n\n" +
                    "Thank you for applying for the position of " + jobTitle + ".\n\n" +
                    "After our AI-based resume screening, your profile achieved a match score of " +
                    String.format("%.0f", matchScore) + "% against the job requirements.\n" +
                    "Our minimum required match score is 80%.\n\n" +
                    "Unfortunately, we are unable to proceed with your application at this time.\n" +
                    "We encourage you to update your profile and apply for roles that better match your skills.\n\n" +
                    "Best regards,\nHireAI Team");
        } catch (Exception e) {
            log.error("Failed to send AI-rejection email to {}: {}", email, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 5. Exam Result (passed or failed)
    // ─────────────────────────────────────────────
    @Async
    public void notifyExamResult(String email, String candidateName, String jobTitle,
                                  double score, double requiredScore, boolean passed) {
        try {
            if (passed) {
                send(email,
                        "Exam Results – You Passed! – " + jobTitle,
                        "Dear " + candidateName + ",\n\n" +
                        "Congratulations! You have successfully passed the online exam for " + jobTitle + ".\n\n" +
                        "Your Score : " + String.format("%.1f", score) + "%\n" +
                        "Required   : " + String.format("%.0f", requiredScore) + "%\n\n" +
                        "You have been auto-progressed to the Interview round. " +
                        "An interview has been scheduled for you – please log in to HireAI for details.\n\n" +
                        "Best regards,\nHireAI Team");
            } else {
                send(email,
                        "Exam Results – " + jobTitle,
                        "Dear " + candidateName + ",\n\n" +
                        "Thank you for taking the online exam for " + jobTitle + ".\n\n" +
                        "Your Score : " + String.format("%.1f", score) + "%\n" +
                        "Required   : " + String.format("%.0f", requiredScore) + "%\n\n" +
                        "Unfortunately, you did not meet the minimum score to proceed to the interview round.\n" +
                        "We encourage you to keep improving your skills and apply for future opportunities.\n\n" +
                        "Best regards,\nHireAI Team");
            }
        } catch (Exception e) {
            log.error("Failed to send exam-result email to {}: {}", email, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────
    private void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
        log.info("Email sent to {} – {}", to, subject);
    }

    private String nvl(Object val) {
        return val != null ? val.toString() : "N/A";
    }
}
