package com.PlantProject.PlantProject.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class LoginLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(LoginLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if ("/login".equals(req.getServletPath()) && "POST".equalsIgnoreCase(req.getMethod())) {
            String email = req.getParameter("email");
            String password = req.getParameter("password");
            logger.info("LOGIN ATTEMPT: email={}, password={}", email, password);
            // Store the last raw password for debugging
            SecurityConfig.lastRawPassword = password;
        }
        chain.doFilter(request, response);
    }
} 