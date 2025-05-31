document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('detectionForm');
    const imageFile = document.getElementById('imageFile');
    const imagePreview = document.getElementById('imagePreview');
    const previewSection = document.getElementById('previewSection');
    const resultsSection = document.getElementById('resultsSection');
    const loadingSpinner = document.getElementById('loadingSpinner');

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

        const plantType = document.getElementById('plantType').value;
        const file = imageFile.files[0];

        if (!plantType || !file) {
            alert('Please select both plant type and image file');
            return;
        }

        // Show loading spinner
        loadingSpinner.style.display = 'block';
        resultsSection.style.display = 'none';

        // Create form data
        const formData = new FormData();
        formData.append('image', file);
        formData.append('plantType', plantType);

        try {
            const response = await fetch('/api/plant-disease/detect', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (result.status === 'success') {
                // Update results
                document.getElementById('resultPlantType').textContent = result.plant_type;
                document.getElementById('resultDisease').textContent = result.prediction;
                
                // Format confidence score
                const confidence = (Math.max(...result.confidence_scores[0]) * 100).toFixed(2);
                document.getElementById('resultConfidence').textContent = `${confidence}%`;

                // Show results
                resultsSection.style.display = 'block';
            } else {
                throw new Error(result.message || 'Detection failed');
            }
        } catch (error) {
            alert('Error: ' + error.message);
        } finally {
            // Hide loading spinner
            loadingSpinner.style.display = 'none';
        }
    });
}); 