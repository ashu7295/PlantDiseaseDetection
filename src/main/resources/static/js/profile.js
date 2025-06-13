document.addEventListener('DOMContentLoaded', function() {
    // Profile form elements
    const profileEditForm = document.getElementById('profileEditForm');
    const saveProfileBtn = document.getElementById('saveProfileBtn');
    const editProfileBtn = document.querySelector('[data-bs-target="#editProfileModal"]');
    const profileModalBtn = document.querySelector('[data-bs-target="#profileModal"]');

    // Load profile data when profile modal is opened
    if (profileModalBtn) {
        profileModalBtn.addEventListener('click', async function(e) {
            e.preventDefault();
            await loadProfileData(true);
        });
    }

    // Load profile data when edit modal is opened
    if (editProfileBtn) {
        editProfileBtn.addEventListener('click', async function(e) {
            e.preventDefault();
            await loadProfileData(false);
        });
    }

    // Handle profile form submission
    if (profileEditForm) {
        profileEditForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            if (!validateForm()) {
                showError('Please fill in all required fields correctly.');
                return;
            }
            
            // Disable submit button and show loading state
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Saving...';
            }
            
            const formData = {
                name: document.getElementById('profileName')?.value?.trim() || '',
                phoneNumber: document.getElementById('profilePhone')?.value?.trim() || '',
                address: document.getElementById('profileAddress')?.value?.trim() || '',
                city: document.getElementById('profileCity')?.value?.trim() || '',
                state: document.getElementById('profileState')?.value?.trim() || '',
                postalCode: document.getElementById('profilePostalCode')?.value?.trim() || '',
                country: document.getElementById('profileCountry')?.value?.trim() || ''
            };
            
            try {
                const response = await fetch('/api/user/profile', {
                    method: 'PUT',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(formData)
                });
                
                if (response.status === 401) {
                    console.log('User not authenticated, redirecting to login');
                    window.location.href = '/login';
                    return;
                }
                
                const result = await response.json();
                
                if (response.ok && result.status === 'success') {
                    showSuccess('Profile updated successfully!');
                    
                    // Update UI elements with the returned user data
                    if (result.user) {
                        updateUIElements(result.user);
                    }
                    
                    // Refresh navbar data
                    if (window.refreshNavbarUserData) {
                        await window.refreshNavbarUserData();
                    }
                    
                    // Refresh profile view modal data
                    await loadProfileData(true);
                    
                    // Auto-close modal after 2 seconds
                    setTimeout(() => {
                        const modal = bootstrap.Modal.getInstance(document.getElementById('editProfileModal'));
                        if (modal) {
                            modal.hide();
                        }
                    }, 2000);
                } else {
                    const errorMessage = result.message || 'Failed to update profile';
                    showError(errorMessage);
                    console.error('Profile update failed:', result);
                }
            } catch (error) {
                console.error('Error updating profile:', error);
                showError('Network error. Please check your connection and try again.');
            } finally {
                // Reset button state
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<i class="fas fa-save me-2"></i>Save Changes';
                }
            }
        });
    }

    // Load profile data function
    async function loadProfileData(isViewModal = false) {
        try {
            console.log('Loading profile data for modal, isViewModal:', isViewModal);
            
            const response = await fetch('/api/user/profile', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            console.log('Profile modal response status:', response.status);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            
            // Log only the necessary data structure without prototype chain
            console.log('Profile modal data received:', {
                status: data.status,
                user: {
                    name: data.user?.name,
                    email: data.user?.email,
                    address: data.user?.address,
                    city: data.user?.city,
                    state: data.user?.state,
                    country: data.user?.country,
                    postalCode: data.user?.postalCode,
                    phoneNumber: data.user?.phoneNumber,
                    joinDate: data.user?.joinDate,
                    stats: {
                        totalAnalyses: data.user?.stats?.totalAnalyses || 0,
                        healthyPlants: data.user?.stats?.healthyPlants || 0,
                        diseasedPlants: data.user?.stats?.diseasedPlants || 0,
                        successRate: data.user?.stats?.successRate || 0,
                        averageConfidence: data.user?.stats?.averageConfidence || 0,
                        freeAnalysesLimit: data.user?.stats?.freeAnalysesLimit || 0,
                        freeAnalysesUsed: data.user?.stats?.freeAnalysesUsed || 0,
                        remainingFreeAnalyses: data.user?.stats?.remainingFreeAnalyses || 0,
                        hasActiveSubscription: data.user?.stats?.hasActiveSubscription || false,
                        canPerformAnalysis: data.user?.stats?.canPerformAnalysis || false
                    }
                }
            });

            if (data.status === 'success' && data.user) {
                updateProfileUI(data.user, isViewModal);
            } else {
                throw new Error('Failed to load profile data');
            }
        } catch (error) {
            console.error('Error loading profile data:', error);
            showToast('error', 'Failed to load profile data. Please try again.');
        }
    }

    function updateProfileUI(userData, isViewModal) {
        console.log('Updating profile UI with data:', userData);
        
        // Update modal title and user name
        const profileModalName = document.getElementById('profileModalName');
        if (profileModalName) {
            profileModalName.textContent = userData.name || 'Guest User';
        }

        // Update modal email
        const profileModalEmail = document.getElementById('profileModalEmail');
        if (profileModalEmail) {
            profileModalEmail.textContent = userData.email || 'Not logged in';
        }

        // Update modal initials
        const profileModalInitials = document.getElementById('profileModalInitials');
        if (profileModalInitials) {
            const initials = userData.name ? userData.name.charAt(0).toUpperCase() : 'G';
            profileModalInitials.textContent = initials;
        }

        // Update modal status
        const profileModalStatus = document.getElementById('profileModalStatus');
        if (profileModalStatus) {
            const statusText = userData.hasActiveSubscription ? 'Premium Member' : 'Free Member';
            const statusSpan = profileModalStatus.querySelector('span');
            if (statusSpan) {
                statusSpan.textContent = statusText;
            }
            
            // Update status badge styling
            if (userData.hasActiveSubscription) {
                profileModalStatus.classList.remove('bg-success-subtle', 'text-success');
                profileModalStatus.classList.add('bg-warning-subtle', 'text-warning');
            } else {
                profileModalStatus.classList.remove('bg-warning-subtle', 'text-warning');
                profileModalStatus.classList.add('bg-success-subtle', 'text-success');
            }
        }

        // Update personal information displays
        const displayMappings = {
            'profilePhoneDisplay': userData.phoneNumber || 'Not provided',
            'profileUserTypeDisplay': userData.hasActiveSubscription ? 'Premium' : 'Standard',
            'profileCityDisplay': userData.city || 'Not provided',
            'profileStateDisplay': userData.state || 'Not provided',
            'profileAddressDisplay': userData.address || 'Not provided'
        };

        Object.entries(displayMappings).forEach(([elementId, value]) => {
            const element = document.getElementById(elementId);
            if (element) {
                element.textContent = value;
            }
        });

        // Update navbar user display
        const userNameDisplay = document.getElementById('userNameDisplay');
        const dropdownUserName = document.getElementById('dropdownUserName');
        const dropdownUserEmail = document.getElementById('dropdownUserEmail');
        const userInitials = document.getElementById('userInitials');
        const dropdownUserInitials = document.getElementById('dropdownUserInitials');
        const userRoleDisplay = document.getElementById('userRoleDisplay');
        const userStatusText = document.getElementById('userStatusText');

        if (userNameDisplay) userNameDisplay.textContent = userData.name || 'Guest';
        if (dropdownUserName) dropdownUserName.textContent = userData.name || 'Guest';
        if (dropdownUserEmail) dropdownUserEmail.textContent = userData.email || 'Not logged in';
        if (userInitials) userInitials.textContent = userData.name ? userData.name.charAt(0).toUpperCase() : 'G';
        if (dropdownUserInitials) dropdownUserInitials.textContent = userData.name ? userData.name.charAt(0).toUpperCase() : 'G';
        if (userRoleDisplay) userRoleDisplay.textContent = userData.hasActiveSubscription ? 'Premium Member' : 'Free Member';
        if (userStatusText) userStatusText.textContent = userData.hasActiveSubscription ? 'Premium Member' : 'Free Member';

        // Update form fields if in edit mode
        if (!isViewModal) {
            const formMappings = {
                'profileName': userData.name || '',
                'profileEmail': userData.email || '',
                'profilePhone': userData.phoneNumber || '',
                'profileAddress': userData.address || '',
                'profileCity': userData.city || '',
                'profileState': userData.state || '',
                'profilePostalCode': userData.postalCode || '',
                'profileCountry': userData.country || ''
            };

            Object.entries(formMappings).forEach(([elementId, value]) => {
                const element = document.getElementById(elementId);
                if (element) {
                    element.value = value;
                }
            });
        }

        // Update stats if available
        if (userData.stats) {
            updateStatsUI(userData.stats);
        }
    }

    function updateStatsUI(stats) {
        // Analysis Stats
        setElementValue('total-analyses', stats.totalAnalyses);
        setElementValue('healthy-plants', stats.healthyPlants);
        setElementValue('diseased-plants', stats.diseasedPlants);
        setElementValue('success-rate', `${stats.successRate}%`);
        setElementValue('avg-confidence', `${stats.averageConfidence}%`);
        
        // Subscription Stats
        setElementValue('free-analyses-limit', stats.freeAnalysesLimit);
        setElementValue('free-analyses-used', stats.freeAnalysesUsed);
        setElementValue('remaining-analyses', stats.remainingFreeAnalyses);
        
        // Subscription Status
        const subscriptionStatus = stats.hasActiveSubscription ? 'Active' : 'Free Plan';
        setElementValue('subscription-status', subscriptionStatus);
    }

    function setElementValue(id, value) {
        const element = document.getElementById(id);
        if (element) {
            if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {
                element.value = value || '';
            } else {
                element.textContent = value || '';
            }
        }
    }

    function updateProfilePicture(profilePicture, prefix) {
        const imgElement = document.getElementById(`${prefix}profile-picture`);
        if (imgElement) {
            imgElement.src = profilePicture || '/images/default-profile.png';
            imgElement.alt = 'Profile Picture';
        }
    }

    // Update UI functions
    function updateProfileViewModal(profile) {
        // Update basic info
        const elements = {
            'profileModalName': profile.name || 'Guest User',
            'profileModalEmail': profile.email || 'Not logged in',
            'profilePhoneDisplay': profile.phoneNumber || 'Not provided',
            'profileUserTypeDisplay': profile.userType || 'Standard',
            'profileCityDisplay': profile.city || 'Not provided',
            'profileStateDisplay': profile.state || 'Not provided',
            'profileAddressDisplay': profile.address || 'Not provided'
        };
        
        Object.entries(elements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value;
            }
        });
        
        // Update initials
        const initials = (profile.name || 'G').substring(0, 1).toUpperCase();
        const initialsElements = ['profileModalInitials', 'userInitials', 'dropdownUserInitials'];
        initialsElements.forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = initials;
            }
        });
    }

    function updateUIElements(userData) {
        // Update all name displays
        const nameElements = {
            'dropdownUserName': userData.name || 'Guest User',
            'userNameDisplay': userData.name || 'Guest User',
            'profileModalName': userData.name || 'Guest User',
            'profileModalEmail': userData.email || 'Not logged in'
        };
        
        Object.entries(nameElements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value;
            }
        });
        
        // Update initials
        const initials = (userData.name || 'G').substring(0, 1).toUpperCase();
        const initialsElements = ['userInitials', 'dropdownUserInitials', 'profileModalInitials'];
        initialsElements.forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = initials;
            }
        });
    }

    // Helper functions
    function validateForm() {
        let isValid = true;
        const form = document.getElementById('profileEditForm');
        
        // Clear previous validation states
        clearValidationStates();
        
        // Validate required fields
        const nameField = document.getElementById('profileName');
        if (nameField && !nameField.value.trim()) {
            nameField.classList.add('is-invalid');
            isValid = false;
        } else if (nameField) {
            nameField.classList.add('is-valid');
        }
        
        // Validate phone number if provided
        const phoneField = document.getElementById('profilePhone');
        if (phoneField && phoneField.value.trim() && !isValidPhone(phoneField.value.trim())) {
            phoneField.classList.add('is-invalid');
            isValid = false;
        } else if (phoneField && phoneField.value.trim()) {
            phoneField.classList.add('is-valid');
        }
        
        return isValid;
    }

    function showSuccess(message) {
        const successMsg = document.getElementById('profileSuccessMsg');
        const errorMsg = document.getElementById('profileErrorMsg');
        if (successMsg && errorMsg) {
            successMsg.classList.remove('d-none');
            errorMsg.classList.add('d-none');
            successMsg.querySelector('span').textContent = message;
        }
    }

    function showError(message) {
        const successMsg = document.getElementById('profileSuccessMsg');
        const errorMsg = document.getElementById('profileErrorMsg');
        if (successMsg && errorMsg) {
            successMsg.classList.add('d-none');
            errorMsg.classList.remove('d-none');
            errorMsg.querySelector('span').textContent = message;
        }
    }

    function hideMessages() {
        const successMsg = document.getElementById('profileSuccessMsg');
        const errorMsg = document.getElementById('profileErrorMsg');
        if (successMsg && errorMsg) {
            successMsg.classList.add('d-none');
            errorMsg.classList.add('d-none');
        }
    }

    function clearValidationStates() {
        const form = document.getElementById('profileEditForm');
        if (form) {
            form.querySelectorAll('.is-invalid, .is-valid').forEach(element => {
                element.classList.remove('is-invalid', 'is-valid');
            });
        }
    }

    function isValidPhone(phone) {
        // Basic phone number validation
        return /^[+]?[0-9\s\-\(\)]{10,}$/.test(phone);
    }
});

// Function to show toast notifications
function showToast(type, message) {
    const alertElement = type === 'success' ? document.getElementById('profileSuccessMsg') : document.getElementById('profileErrorMsg');
    const errorTextElement = document.getElementById('profileErrorText');
    
    if (type === 'error' && errorTextElement) {
        errorTextElement.textContent = message;
    }
    
    // Hide both alerts first
    document.getElementById('profileSuccessMsg').classList.add('d-none');
    document.getElementById('profileErrorMsg').classList.add('d-none');
    
    // Show the relevant alert
    alertElement.classList.remove('d-none');
    
    // Auto hide after 5 seconds
    setTimeout(() => {
        alertElement.classList.add('d-none');
    }, 5000);
}

// Function to load user data
async function loadUserData() {
    try {
        const response = await fetch('/api/user/profile', {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }

        if (!response.ok) {
            throw new Error('Failed to load profile data');
        }

        const userData = await response.json();
        
        // Update form fields
        document.getElementById('profileName').value = userData.name || '';
        document.getElementById('profileEmail').value = userData.email || '';
        document.getElementById('profilePhone').value = userData.phoneNumber || '';
        document.getElementById('profileAddress').value = userData.address || '';
        document.getElementById('profileCity').value = userData.city || '';
        document.getElementById('profileState').value = userData.state || '';
        document.getElementById('profilePostalCode').value = userData.postalCode || '';
        document.getElementById('profileCountry').value = userData.country || '';

    } catch (error) {
        console.error('Error loading user data:', error);
        showToast('error', 'Failed to load profile data. Please try again later.');
    }
}

// Function to update profile
async function updateProfile(formData) {
    try {
        const response = await fetch('/api/user/profile', {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = '/login';
                return;
            }
            throw new Error('Failed to update profile');
        }

        const result = await response.json();
        
        if (result.status === 'success') {
            showToast('success', 'Profile updated successfully');
            // Reload the page after a short delay to show updated data
            setTimeout(() => {
                window.location.reload();
            }, 2000);
        } else {
            throw new Error(result.message || 'Failed to update profile');
        }
    } catch (error) {
        console.error('Error updating profile:', error);
        showToast('error', error.message || 'Failed to update profile. Please try again later.');
    }
}

// Form validation and submission
document.addEventListener('DOMContentLoaded', function() {
    // Load user data when the page loads
    loadUserData();

    const form = document.getElementById('profileEditForm');
    if (form) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            
            // Clear previous alerts
            document.getElementById('profileSuccessMsg').classList.add('d-none');
            document.getElementById('profileErrorMsg').classList.add('d-none');

            // Validate form
            if (!form.checkValidity()) {
                event.stopPropagation();
                form.classList.add('was-validated');
                return;
            }

            // Collect form data
            const formData = {
                name: document.getElementById('profileName').value.trim(),
                email: document.getElementById('profileEmail').value.trim(),
                phoneNumber: document.getElementById('profilePhone').value.trim(),
                address: document.getElementById('profileAddress').value.trim(),
                city: document.getElementById('profileCity').value.trim(),
                state: document.getElementById('profileState').value.trim(),
                postalCode: document.getElementById('profilePostalCode').value.trim(),
                country: document.getElementById('profileCountry').value.trim()
            };

            // Disable submit button and show loading state
            const submitBtn = document.getElementById('saveProfileBtn');
            const originalBtnText = submitBtn.innerHTML;
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Saving...';

            // Submit form
            updateProfile(formData).finally(() => {
                // Re-enable submit button and restore text
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalBtnText;
            });
        });
    }

    // Initialize any Bootstrap tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
});