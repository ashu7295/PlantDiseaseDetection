document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('detectionForm');
    const imageFile = document.getElementById('imageFile');
    const imagePreview = document.getElementById('imagePreview');
    const previewSection = document.getElementById('previewSection');
    const resultsSection = document.getElementById('resultsSection');
    const loadingSpinner = document.getElementById('loadingSpinner');
    const recentImages = document.getElementById('recentImages');

    // Load recent images on page load
    loadRecentImages();

    // Show image preview when a file is selected
    imageFile.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function(e) {
                imagePreview.src = e.target.result;
                previewSection.style.display = 'block';
            };
            reader.readAsDataURL(file);
        }
    });

    // Handle form submission
    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        const formData = new FormData();
        formData.append('plantType', document.getElementById('plantType').value);
        formData.append('image', imageFile.files[0]);

        loadingSpinner.style.display = 'block';
        resultsSection.style.display = 'none';

        try {
            const response = await fetch('/api/detect', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();
            
            // Display results
            document.getElementById('resultPlantType').textContent = result.plantType;
            document.getElementById('resultDisease').textContent = result.disease;
            document.getElementById('resultConfidence').textContent = result.confidence + '%';
            
            // Save to history
            saveToHistory(result);
            
            // Update recent images
            loadRecentImages();
            
            resultsSection.style.display = 'block';
        } catch (error) {
            console.error('Error:', error);
            alert('An error occurred during detection. Please try again.');
        } finally {
            loadingSpinner.style.display = 'none';
        }
    });

    function saveToHistory(result) {
        const history = JSON.parse(localStorage.getItem('detectionHistory') || '[]');
        const newDetection = {
            ...result,
            imageUrl: imagePreview.src,
            timestamp: new Date().toISOString()
        };
        
        history.unshift(newDetection);
        localStorage.setItem('detectionHistory', JSON.stringify(history));
    }

    function loadRecentImages() {
        fetch('/api/analysis/history')
            .then(response => response.json())
            .then(history => {
                const recentImages = document.getElementById('recentImages');
                recentImages.innerHTML = ''; // Clear existing content
                
                if (!Array.isArray(history) || history.length === 0) {
                    recentImages.innerHTML = `
                        <div class="col-12 text-center">
                            <p class="text-muted">No recent analyses found.</p>
                        </div>
                    `;
                    return;
                }
                
                history.forEach(analysis => {
                    const col = document.createElement('div');
                    col.className = 'col-md-4 mb-3';
                    
                    const card = document.createElement('div');
                    card.className = 'card h-100';
                    
                    const img = document.createElement('img');
                    img.className = 'card-img-top';
                    img.alt = `${analysis.plantType} - ${analysis.prediction}`;
                    img.style.height = '150px';
                    img.style.objectFit = 'cover';
                    img.src = `/uploads/${analysis.imagePath}`;
                    img.onerror = function() {
                        this.src = '/static/images/placeholder.jpg';
                        this.alt = 'Image not available';
                        this.style.opacity = '0.7';
                    };
                    
                    const cardBody = document.createElement('div');
                    cardBody.className = 'card-body p-2';
                    
                    const title = document.createElement('h6');
                    title.className = 'card-title mb-1';
                    title.textContent = analysis.plantType;
                    
                    const text = document.createElement('p');
                    text.className = 'card-text small mb-0';
                    text.textContent = `Disease: ${analysis.prediction}`;
                    
                    cardBody.appendChild(title);
                    cardBody.appendChild(text);
                    
                    card.appendChild(img);
                    card.appendChild(cardBody);
                    col.appendChild(card);
                    recentImages.appendChild(col);
                });
                
                // Add "View More" section
                const viewMoreDiv = document.createElement('div');
                viewMoreDiv.className = 'col-12 text-center mt-2';
                viewMoreDiv.innerHTML = `
                    <p class="text-muted small mb-2">Showing your 6 most recent analyses</p>
                                            <a href="/analysis/history" class="btn btn-outline-primary btn-sm">View Full History</a>
                `;
                recentImages.appendChild(viewMoreDiv);
            })
            .catch(error => {
                console.error('Error loading recent images:', error);
                const recentImages = document.getElementById('recentImages');
                recentImages.innerHTML = `
                    <div class="col-12 text-center">
                        <p class="text-danger">Error loading recent images. Please try again later.</p>
                    </div>
                `;
            });
    }
}); 