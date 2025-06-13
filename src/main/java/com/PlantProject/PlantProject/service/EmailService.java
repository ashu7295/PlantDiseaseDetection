package com.PlantProject.PlantProject.service;

import com.PlantProject.PlantProject.model.AnalysisResult;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void sendOTPEmail(String to, String otp) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(to);
        helper.setSubject("Your OTP for Plant Disease Detection");
        
        String emailContent = loadEmailTemplate("templates/otp-email.html")
                .replace("{{otp}}", otp);
        
        helper.setText(emailContent, true);
        emailSender.send(message);
        logger.info("OTP email sent to: {}", to);
    }

    public void sendAnalysisNotification(String to, AnalysisResult result, String imageName) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(to);
        helper.setSubject("Plant Disease Analysis Results");
        
        String emailContent = loadEmailTemplate("templates/analysis-notification.html")
                .replace("{{plantName}}", result.getPlantName())
                .replace("{{disease}}", result.getDisease())
                .replace("{{confidence}}", String.format("%.2f%%", result.getConfidence() * 100));
        
        helper.setText(emailContent, true);
        
        // Attach the analyzed image
        Path imagePath = Paths.get("uploads", result.getImagePath());
        if (imagePath.toFile().exists()) {
            FileSystemResource file = new FileSystemResource(imagePath.toFile());
            helper.addAttachment(imageName, file);
        }
        
        emailSender.send(message);
        logger.info("Analysis notification email sent to: {}", to);
    }

    public void sendPasswordChangeConfirmation(String to) throws MessagingException {
        sendPasswordChangeConfirmation(to, null, null);
    }

    public void sendPasswordChangeConfirmation(String to, String ipAddress, String userAgent) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(to);
        helper.setSubject("🔒 Password Changed Successfully - Plant Disease Detection");
        
        String emailContent = loadEmailTemplate("templates/password-change-confirmation.html");
        
        // Replace placeholders with actual values
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        
        emailContent = emailContent.replace("{{timestamp}}", now.format(formatter));
        emailContent = emailContent.replace("{{ipAddress}}", ipAddress != null ? ipAddress : "Unknown");
        emailContent = emailContent.replace("{{userAgent}}", userAgent != null ? userAgent : "Unknown");
        emailContent = emailContent.replace("{{loginUrl}}", "http://localhost:8080/login");
        emailContent = emailContent.replace("{{supportUrl}}", "mailto:support@plantdiseasedetection.com");
        
        helper.setText(emailContent, true);
        emailSender.send(message);
        logger.info("Password change confirmation email sent to: {}", to);
    }

    public void sendContactFormNotification(String name, String email, String subject, String message) throws MessagingException {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        
        // Send to admin email
        helper.setTo("ashutoshrana972@gmail.com");
        helper.setSubject("Contact Form Submission: " + subject);
        helper.setReplyTo(email);
        
        // Create email content
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        
        String emailContent = String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px;">
                        New Contact Form Submission
                    </h2>
                    
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="color: #2c3e50; margin-top: 0;">Contact Details</h3>
                        <p><strong>Name:</strong> %s</p>
                        <p><strong>Email:</strong> %s</p>
                        <p><strong>Subject:</strong> %s</p>
                        <p><strong>Submitted:</strong> %s</p>
                    </div>
                    
                    <div style="background: #ffffff; padding: 20px; border: 1px solid #dee2e6; border-radius: 8px;">
                        <h3 style="color: #2c3e50; margin-top: 0;">Message</h3>
                        <p style="white-space: pre-wrap;">%s</p>
                    </div>
                    
                    <div style="margin-top: 20px; padding: 15px; background: #e8f4fd; border-radius: 8px;">
                        <p style="margin: 0; font-size: 14px; color: #666;">
                            <strong>Note:</strong> You can reply directly to this email to respond to the sender.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, name, email, subject, now.format(formatter), message);
        
        helper.setText(emailContent, true);
        emailSender.send(mimeMessage);
        logger.info("Contact form notification sent for submission by {} ({})", name, email);
    }

    private String loadEmailTemplate(String templatePath) {
        try {
            Resource resource = new ClassPathResource(templatePath);
            Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            logger.error("Error loading email template: {}", templatePath, e);
            throw new RuntimeException("Error loading email template", e);
        }
    }
} 