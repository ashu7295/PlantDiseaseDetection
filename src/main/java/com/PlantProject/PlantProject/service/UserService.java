package com.PlantProject.PlantProject.service;

import com.PlantProject.PlantProject.dto.SignupRequest;
import com.PlantProject.PlantProject.model.OTPType;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.model.UserRole;
import com.PlantProject.PlantProject.repository.UserRepository;
import com.PlantProject.PlantProject.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OTPService otpService;

    @Autowired
    private EmailService emailService;

    public User createUser(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Attempt to create user with existing email: {}", request.getEmail());
            throw new RuntimeException("Email already exists");
        }

        logger.info("Creating new user with email: {}", request.getEmail());
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setVerified(true);
        return userRepository.save(user);
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Getting current user for email: {}", email);
        return findByEmail(email);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User saveUser(User user) {
        logger.info("Saving user: {}", user.getEmail());
        // Only encode password if it's a new one (not already encoded)
        if (user.getPassword() != null && !user.getPassword().isEmpty() && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Transactional
    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User already exists with this email address");
        }
        
        // Validate password
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        
        // Validate email format
        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // Validate phone number
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Set default role if not specified
        if (user.getUserRole() == null) {
            user.setUserRole(UserRole.USER);
        }
        
        // Set verification status
        user.setVerified(false);
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Generate and send OTP
        try {
            otpService.generateAndSendOTP(user.getEmail(), OTPType.EMAIL_VERIFICATION);
        } catch (Exception e) {
            logger.error("Failed to send OTP to user: " + user.getEmail(), e);
            throw new RuntimeException("Failed to send verification OTP. Please try again later.");
        }
        
        return savedUser;
    }

    @Transactional
    public boolean verifyEmail(String email, String otp) {
        if (otpService.validateOTP(email, otp, OTPType.EMAIL_VERIFICATION)) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                user.setVerified(true);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void initiatePasswordReset(String email) throws Exception {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        otpService.generateAndSendOTP(email, OTPType.PASSWORD_RESET);
    }

    @Transactional
    public boolean resetPassword(String email, String otp, String newPassword) {
        if (otpService.validateOTP(email, otp, OTPType.PASSWORD_RESET)) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                // Validate new password strength
                if (!isPasswordStrong(newPassword)) {
                    throw new RuntimeException("Password must be at least 8 characters with uppercase, lowercase, number, and special character");
                }
                
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                
                // Send password change confirmation email
                try {
                    emailService.sendPasswordChangeConfirmation(user.getEmail(), SecurityUtils.getClientIpAddress(), SecurityUtils.getBrowserName());
                    logger.info("Password change confirmation email sent to: {}", user.getEmail());
                } catch (Exception e) {
                    logger.warn("Failed to send password change confirmation email to: {}", user.getEmail(), e);
                    // Don't fail the password reset if email fails
                }
                
                return true;
            }
        }
        return false;
    }

    /**
     * Validates password strength according to industry standards
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        
        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return false;
        }
        
        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            return false;
        }
        
        // Check for at least one special character
        if (!password.matches(".*[@$!%*?&].*")) {
            return false;
        }
        
        return true;
    }

    @Transactional
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update fields
        existingUser.setName(user.getName());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        existingUser.setAddress(user.getAddress());
        existingUser.setCity(user.getCity());
        existingUser.setState(user.getState());
        existingUser.setCountry(user.getCountry());
        existingUser.setPostalCode(user.getPostalCode());
        existingUser.setFarmSize(user.getFarmSize());
        existingUser.setCropsGrown(user.getCropsGrown());
        existingUser.setYearsOfExperience(user.getYearsOfExperience());
        existingUser.setSpecialization(user.getSpecialization());
        
        return userRepository.save(existingUser);
    }
} 