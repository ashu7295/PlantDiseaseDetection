document.addEventListener('DOMContentLoaded', function() {
    // Initialize components
    initializeSidebar();
    loadAnalysisStats();
    loadRecentActivity();
    loadUserProfile();

    // Sidebar functionality
    function initializeSidebar() {
        const sidebar = document.getElementById('sidebar');
        const mainContent = document.getElementById('mainContent');
        const sidebarToggle = document.getElementById('sidebarToggle');

        sidebarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('expanded');
            mainContent.classList.toggle('expanded');
        });

        // Close sidebar when clicking outside on mobile
        document.addEventListener('click', function(event) {
            if (window.innerWidth <= 768) {
                if (!sidebar.contains(event.target) && !sidebarToggle.contains(event.target)) {
                    sidebar.classList.remove('expanded');
                    mainContent.classList.remove('expanded');
                }
            }
        });

        // Handle sidebar navigation
        document.querySelectorAll('.sidebar-item').forEach(item => {
            item.addEventListener('click', function(e) {
                const href = this.getAttribute('href');
                if (href.startsWith('#')) {
                    e.preventDefault();
                    const target = document.querySelector(href);
                    if (target) {
                        target.scrollIntoView({ behavior: 'smooth' });
                    }
                }
                
                // Update active state
                document.querySelectorAll('.sidebar-item').forEach(i => i.classList.remove('active'));
                this.classList.add('active');
            });
        });
    }

    // Load analysis statistics
    async function loadAnalysisStats() {
        try {
            const response = await fetch('/analysis/api/stats', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                },
                credentials: 'include'
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const stats = await response.json();
            console.log('Stats data received:', stats);
            
            // Update the stats display
            document.getElementById('totalAnalyses').textContent = stats.totalAnalyses || 0;
            document.getElementById('healthyPlants').textContent = stats.healthyPlants || 0;
            document.getElementById('diseasedPlants').textContent = stats.diseasedPlants || 0;
            
            // Animate counters
            animateCounters();
        } catch (error) {
            console.error('Error loading stats:', error);
            document.getElementById('totalAnalyses').textContent = '0';
            document.getElementById('healthyPlants').textContent = '0';
            document.getElementById('diseasedPlants').textContent = '0';
        }
    }

    function animateCounters() {
        const counters = document.querySelectorAll('.stat-value');
        counters.forEach(counter => {
            const target = parseInt(counter.textContent);
            if (isNaN(target)) return;
            
            let current = 0;
            const increment = target / 30;
            const timer = setInterval(() => {
                current += increment;
                if (current >= target) {
                    counter.textContent = target;
                    clearInterval(timer);
                } else {
                    counter.textContent = Math.floor(current);
                }
            }, 50);
        });
    }

    // Load recent activity
    async function loadRecentActivity() {
        try {
            const response = await fetch('/api/analysis/history?limit=5', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                },
                credentials: 'include'
            });
            const history = await response.json();
            const container = document.getElementById('analysisHistory');
            
            if (!Array.isArray(history) || history.length === 0) {
                container.innerHTML = `
                    <div class="text-center text-muted py-4">
                        <i class="fas fa-leaf fa-3x mb-3 opacity-50"></i>
                        <p>No recent activity found. Start by analyzing your first plant!</p>
                        <a href="/analysis/page" class="action-btn">Analyze Now</a>
                    </div>
                `;
                return;
            }
            
            container.innerHTML = history.map(item => `
                <div class="activity-item">
                    <div class="activity-icon ${item.prediction.includes('healthy') ? 'healthy' : 'diseased'}">
                        <i class="fas ${item.prediction.includes('healthy') ? 'fa-leaf' : 'fa-bug'}"></i>
                    </div>
                    <div class="activity-content">
                        <div class="activity-title">
                            ${item.plantType.charAt(0).toUpperCase() + item.plantType.slice(1)} Analysis
                        </div>
                        <div class="activity-meta">
                            ${item.prediction.replace(/.*___/, '')} • ${(item.confidence * 100).toFixed(1)}% confidence • ${new Date(item.analysisDate).toLocaleDateString()}
                        </div>
                    </div>
                </div>
            `).join('');
            
            if (history.length >= 5) {
                container.innerHTML += `
                    <div class="text-center mt-3">
                        <a href="/api/analysis/page/history" class="action-btn">View All History</a>
                    </div>
                `;
            }
        } catch (error) {
            console.error('Error loading recent activity:', error);
            document.getElementById('analysisHistory').innerHTML = `
                <div class="text-center text-danger py-4">
                    <i class="fas fa-exclamation-triangle fa-2x mb-3"></i>
                    <p>Error loading recent activity</p>
                </div>
            `;
        }
    }

    // Load user profile
    async function loadUserProfile() {
        try {
            const response = await fetch('/api/user/profile');
            const profile = await response.json();
            
            if (profile.status === 'error') {
                console.error('Error loading profile:', profile.message);
                return;
            }

            // Update dropdown if elements exist
            const dropdownUserName = document.getElementById('dropdownUserName');
            const dropdownUserEmail = document.getElementById('dropdownUserEmail');
            
            if (dropdownUserName) {
                dropdownUserName.textContent = profile.name || 'Guest';
            }
            if (dropdownUserEmail) {
                dropdownUserEmail.textContent = profile.email || 'Not logged in';
            }
        } catch (error) {
            console.error('Error loading user profile:', error);
        }
    }

    // Smooth scrolling for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Add scroll-based animations
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, observerOptions);

    document.querySelectorAll('.animate-fadein').forEach(el => {
        observer.observe(el);
    });
}); 