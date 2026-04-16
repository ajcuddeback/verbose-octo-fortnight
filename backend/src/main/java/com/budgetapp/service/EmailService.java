package com.budgetapp.service;

import com.budgetapp.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // TODO: Ensure spring.mail.username and spring.mail.password are set in application.properties
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    @Async
    public void sendDataDeletionWarningEmail(User user, LocalDate deletionDate) {
        // TODO: Ensure spring.mail.username and spring.mail.password are set in application.properties
        String subject = "Important: Your ClearPath Budget Data Will Be Deleted on " +
                deletionDate.format(DATE_FORMATTER);

        String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #f44336; padding: 20px; text-align: center;">
                        <h1 style="color: white; margin: 0;">Data Deletion Warning</h1>
                    </div>
                    <div style="padding: 30px; background-color: #ffffff;">
                        <p>Dear %s,</p>
                        <p>This is an important notice that your ClearPath Budget account data is scheduled
                        for permanent deletion on <strong>%s</strong>.</p>
                        <p>This is because your subscription was cancelled approximately 2 years ago.</p>
                        <p>If you wish to keep your data, please resubscribe before the deletion date.</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s/subscription" style="background-color: #4CAF50; color: white;
                            padding: 15px 30px; text-decoration: none; border-radius: 5px;
                            font-size: 16px;">Resubscribe Now</a>
                        </div>
                        <p>If you have any questions, please contact our support team.</p>
                        <p>Best regards,<br>The ClearPath Budget Team</p>
                    </div>
                    <div style="padding: 20px; background-color: #f5f5f5; text-align: center;
                    font-size: 12px; color: #666;">
                        <p>ClearPath Budget - Your Personal Finance Companion</p>
                    </div>
                </body>
                </html>
                """.formatted(
                user.getFirstName(),
                deletionDate.format(DATE_FORMATTER),
                frontendUrl
        );

        sendHtmlEmail(user.getEmail(), subject, htmlContent);
        log.info("Data deletion warning email sent to: {}", user.getEmail());
    }

    @Async
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to ClearPath Budget!";

        String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #4CAF50; padding: 20px; text-align: center;">
                        <h1 style="color: white; margin: 0;">Welcome to ClearPath Budget!</h1>
                    </div>
                    <div style="padding: 30px; background-color: #ffffff;">
                        <p>Hi %s,</p>
                        <p>Welcome to ClearPath Budget! We're excited to help you take control of your
                        personal finances.</p>
                        <p>Here's what you can do with ClearPath Budget:</p>
                        <ul>
                            <li>Track your income and expenses</li>
                            <li>Create and manage budgets</li>
                            <li>Set and track financial goals</li>
                            <li>Monitor your bills and subscriptions</li>
                            <li>Get insights into your spending habits</li>
                            <li>View your Financial Health Score</li>
                        </ul>
                        <p>You're currently on a <strong>Free Trial</strong>. Upgrade to unlock all features!</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s/dashboard" style="background-color: #4CAF50; color: white;
                            padding: 15px 30px; text-decoration: none; border-radius: 5px;
                            font-size: 16px;">Get Started</a>
                        </div>
                        <p>Best regards,<br>The ClearPath Budget Team</p>
                    </div>
                    <div style="padding: 20px; background-color: #f5f5f5; text-align: center;
                    font-size: 12px; color: #666;">
                        <p>ClearPath Budget - Your Personal Finance Companion</p>
                    </div>
                </body>
                </html>
                """.formatted(user.getFirstName(), frontendUrl);

        sendHtmlEmail(user.getEmail(), subject, htmlContent);
        log.info("Welcome email sent to: {}", user.getEmail());
    }

    @Async
    public void sendSubscriptionCancelledEmail(User user, LocalDate dataWipeDate) {
        String subject = "Your ClearPath Budget Subscription Has Been Cancelled";

        String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #FF9800; padding: 20px; text-align: center;">
                        <h1 style="color: white; margin: 0;">Subscription Cancelled</h1>
                    </div>
                    <div style="padding: 30px; background-color: #ffffff;">
                        <p>Dear %s,</p>
                        <p>We're sorry to see you go. Your ClearPath Budget subscription has been
                        successfully cancelled.</p>
                        <p>Here's what you need to know:</p>
                        <ul>
                            <li>Your account will remain accessible until the end of your billing period</li>
                            <li>Your data will be retained for <strong>2 years</strong> (until %s)</li>
                            <li>You'll receive a reminder 30 days before your data is scheduled for deletion</li>
                        </ul>
                        <p>If you change your mind, you can resubscribe at any time to continue using
                        all features and keep your data.</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s/subscription" style="background-color: #4CAF50; color: white;
                            padding: 15px 30px; text-decoration: none; border-radius: 5px;
                            font-size: 16px;">Resubscribe</a>
                        </div>
                        <p>Thank you for being a ClearPath Budget customer. We hope to see you back!</p>
                        <p>Best regards,<br>The ClearPath Budget Team</p>
                    </div>
                    <div style="padding: 20px; background-color: #f5f5f5; text-align: center;
                    font-size: 12px; color: #666;">
                        <p>ClearPath Budget - Your Personal Finance Companion</p>
                    </div>
                </body>
                </html>
                """.formatted(
                user.getFirstName(),
                dataWipeDate.format(DATE_FORMATTER),
                frontendUrl
        );

        sendHtmlEmail(user.getEmail(), subject, htmlContent);
        log.info("Subscription cancelled email sent to: {}", user.getEmail());
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
