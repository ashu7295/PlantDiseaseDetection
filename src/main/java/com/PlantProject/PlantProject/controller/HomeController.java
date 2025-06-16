package com.PlantProject.PlantProject.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.PlantProject.PlantProject.dto.SignupRequest;
import com.PlantProject.PlantProject.service.UserService;
import com.PlantProject.PlantProject.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import com.PlantProject.PlantProject.service.AnalysisService;
import java.util.Map;

@Log4j2
@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @Autowired
    private AnalysisService analysisService;

    @GetMapping("/")
    public String home() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model, String error) {
        log.info("login--------------------------------------------------1");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("login--------------------------------------------------2");
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }

        model.addAttribute("user", new User());
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        model.addAttribute("signupRequest", new SignupRequest());
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(@ModelAttribute SignupRequest signupRequest, Model model) {
        try {
            userService.createUser(signupRequest);
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("signupRequest", signupRequest);
            return "signup";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            if (user != null) {
                model.addAttribute("user", user);
                Map<String, Object> stats = analysisService.getUserAnalysisStats(user);
                model.addAttribute("stats", stats);
                model.addAttribute("isAuthenticated", true);
                model.addAttribute("userEmail", user.getEmail());
                model.addAttribute("userName", user.getName() != null ? user.getName() : user.getEmail());
                String userRole = "USER";
                if (user.getUserRole() != null) {
                    userRole = user.getUserRole().toString();
                }
                model.addAttribute("userRole", userRole);
                return "dashboard";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/subscription")
    public String subscription() {
        return "subscription";
    }
} 