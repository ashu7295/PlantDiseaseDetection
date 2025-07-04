package com.PlantProject.PlantProject.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private UserRole userRole = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType = UserType.FARMER;

    @Column(name = "farm_size")
    private Double farmSize;

    @Column(name = "crops_grown")
    private String cropsGrown;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "specialization")
    private String specialization;

    private String address;
    private String city;
    private String state;
    private String country;
    
    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "auth_provider")
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "is_verified")
    private Boolean verified = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Analysis limit tracking fields
    @Column(name = "free_analyses_used", nullable = false)
    private Integer freeAnalysesUsed = 0;

    @Column(name = "free_analyses_limit", nullable = false)
    private Integer freeAnalysesLimit = 10;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (freeAnalysesUsed == null) {
            freeAnalysesUsed = 0;
        }
        if (freeAnalysesLimit == null) {
            freeAnalysesLimit = 10;
        }
        if (userRole == null) {
            userRole = UserRole.USER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userRole.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // Allow login even without email verification
    }
} 