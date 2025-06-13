package com.PlantProject.PlantProject.config;

import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.http.SessionCreationPolicy;

@Log4j2
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    public static volatile String lastRawPassword = null;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/signup", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                .requestMatchers("/api/users/signup", "/api/users/verify-otp", "/api/users/forgot-password", "/api/users/reset-password").permitAll()
                .requestMatchers("/analyze", "/analysis/**", "/api/analyze/**").permitAll()
                .requestMatchers("/api/**", "/analysis/**").authenticated()
                .requestMatchers("/dashboard", "/subscription").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .successHandler(oauth2LoginSuccessHandler)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired")
            );
        
        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        return userRequest -> {
            String clientName = userRequest.getClientRegistration().getClientName();
            logger.info("Processing OAuth2 login for client: {}", clientName);

            // Get the OAuth2 user attributes from the request
            Map<String, Object> attributes = userRequest.getAdditionalParameters();
            logger.debug("OAuth2 user attributes: {}", attributes);

            // For Google OAuth2, we need to extract the ID token
            String idToken = (String) attributes.get("id_token");
            if (idToken != null) {
                // Parse the JWT token to get user info
                String[] parts = idToken.split("\\.");
                if (parts.length == 3) {
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                    try {
                        Map<String, Object> claims = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(payload, Map.class);
                        
                        // Extract user info from claims
                        String email = (String) claims.get("email");
                        String name = (String) claims.get("name");
                        String sub = (String) claims.get("sub");

                        if (email == null) {
                            logger.error("Email not found in ID token claims");
                            throw new RuntimeException("Email not found in ID token claims");
                        }

                        // Create a map with the user attributes
                        Map<String, Object> userAttributes = new HashMap<>();
                        userAttributes.put("email", email);
                        userAttributes.put("name", name);
                        userAttributes.put("sub", sub);

                        // Create a new OAuth2User with email as the principal
                        return new DefaultOAuth2User(
                            Collections.singleton(new OAuth2UserAuthority(userAttributes)),
                            userAttributes,
                            "email"
                        );
                    } catch (Exception e) {
                        logger.error("Error parsing ID token", e);
                        throw new RuntimeException("Error parsing ID token", e);
                    }
                }
            }

            logger.error("ID token not found in OAuth2 response");
            throw new RuntimeException("ID token not found in OAuth2 response");
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            log.info("email------------------------------------: {}", email);
            logger.info("Attempting to authenticate user with email: {}", email);
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
            logger.info("User found: {} (id: {})", user.getEmail(), user.getId());
            logger.info("User password (encoded): {}", user.getPassword());
            // Debug: check if last raw password matches encoded password
            if (lastRawPassword != null) {
                org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
                boolean matches = encoder.matches(lastRawPassword, user.getPassword());
                logger.info("DEBUG: Raw password '{}' matches encoded password: {}", lastRawPassword, matches);
            }
            return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities("ROLE_USER")
                .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}