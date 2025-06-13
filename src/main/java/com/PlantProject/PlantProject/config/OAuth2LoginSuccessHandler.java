package com.PlantProject.PlantProject.config;

import com.PlantProject.PlantProject.model.AuthProvider;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauthUser = oauthToken.getPrincipal();
            
            // Log all attributes for debugging
            logger.debug("OAuth2 user attributes: {}", oauthUser.getAttributes());
            
            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("name");
            String providerId = oauthUser.getAttribute("sub");

            if (email == null) {
                logger.error("Email not found in OAuth2 user attributes");
                throw new RuntimeException("Email not found in OAuth2 user attributes");
            }

            logger.info("Processing OAuth2 login for user: {}", email);

            // Check if user exists by email
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        logger.info("Creating new user for email: {}", email);
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName(name);
                        newUser.setAuthProvider(AuthProvider.GOOGLE);
                        newUser.setProviderId(providerId);
                        return newUser;
                    });

            // Update user info if needed
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                logger.info("Updating local user to Google authentication: {}", email);
                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setProviderId(providerId);
            }

            // Update name if it has changed
            if (name != null && !name.equals(user.getName())) {
                logger.info("Updating user name for: {}", email);
                user.setName(name);
            }

            // Save user
            user = userRepository.save(user);
            logger.info("Successfully saved user: {}", user.getEmail());

            // Create a new authentication with the email as the principal
            OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                oauthUser,
                oauthToken.getAuthorities(),
                oauthToken.getAuthorizedClientRegistrationId()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            // Redirect to dashboard
            setDefaultTargetUrl("/dashboard");
            super.onAuthenticationSuccess(request, response, newAuth);
        } catch (Exception e) {
            logger.error("Error processing OAuth2 login", e);
            throw e;
        }
    }
} 