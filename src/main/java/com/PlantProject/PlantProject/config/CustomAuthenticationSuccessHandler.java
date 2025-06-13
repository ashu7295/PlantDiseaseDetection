package com.PlantProject.PlantProject.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        logger.info("Authentication success for user: {}", authentication.getName());
        logger.info("Authentication principal: {}", authentication.getPrincipal());
        logger.info("Authentication authorities: {}", authentication.getAuthorities());
        
        // Set session attributes
        request.getSession().setAttribute("user", authentication.getName());
        request.getSession().setAttribute("authenticated", true);
        
        if (isAjaxRequest(request)) {
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"success\",\"message\":\"Authentication successful\"}");
        } else {
            setDefaultTargetUrl("/dashboard");
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
} 