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
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(to);
        helper.setSubject("Password Changed Successfully");
        
        String emailContent = loadEmailTemplate("templates/password-change.html");
        
        helper.setText(emailContent, true);
        emailSender.send(message);
        logger.info("Password change confirmation email sent to: {}", to);
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