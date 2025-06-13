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
        logger.info("OAuth2 login success for user: {}", authentication.getName());
        
        OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
        logger.info("OAuth2 user attributes: {}", oauth2User.getAttributes());
        
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        logger.info("Processing OAuth2 login for user - email: {}, name: {}", email, name);
        
        // Find or create user
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                logger.info("Creating new user from OAuth2 login - email: {}", email);
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setAuthProvider(AuthProvider.GOOGLE);
                newUser.setVerified(true);
                return userRepository.save(newUser);
            });
        
        logger.info("User found/created: {} (id: {})", user.getEmail(), user.getId());
        
        // Set session attributes
        request.getSession().setAttribute("user", user.getEmail());
        request.getSession().setAttribute("authenticated", true);
        
        // Update security context
        Authentication newAuth = new OAuth2AuthenticationToken(
            oauth2User,
            oauth2User.getAuthorities(),
            ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        
        logger.info("Updated security context with new authentication");
        
        // Redirect to dashboard
        getRedirectStrategy().sendRedirect(request, response, "/dashboard");
    }
} 