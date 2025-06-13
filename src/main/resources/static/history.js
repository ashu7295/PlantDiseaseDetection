document.addEventListener('DOMContentLoaded', function() {
    loadHistory();
});

async function loadHistory() {
    try {
        const response = await fetch('/api/analysis/history');
        const history = await response.json();
        const historyContainer = document.getElementById('historyImages');

        if (history.length === 0) {
            historyContainer.innerHTML = `
                <div class="col-12 text-center">
                    <div class="alert alert-info">
                        <p class="mb-0">No detection history available.</p>
                        <a href="index.html" class="btn btn-primary mt-2">Start Detection</a>
                    </div>
                </div>
            `;
            return;
        }

        // Display all images in a grid
        history.forEach(item => {
            const col = document.createElement('div');
            col.className = 'col-md-4 mb-4';
            
            col.innerHTML = `
                <div class="card h-100">
                    <img src="/uploads/${item.imagePath}" 
                         class="card-img-top" 
                         alt="Plant Image" 
                         style="height: 250px; object-fit: cover;"
                         onerror="this.onerror=null; this.src='/images/placeholder.jpg'; this.alt='Image not found';">
                    <div class="card-body">
                        <h5 class="card-title">${item.plantType}</h5>
                        <p class="card-text">
                            <strong>Disease:</strong> ${item.prediction}<br>
                            <strong>Confidence:</strong> ${item.confidence}%<br>
                            <strong>Date:</strong> ${new Date(item.analysisDate).toLocaleString()}
                        </p>
                    </div>
                </div>
            `;
            
            historyContainer.appendChild(col);
        });
    } catch (error) {
        console.error('Error loading history:', error);
        const historyContainer = document.getElementById('historyImages');
        historyContainer.innerHTML = `
            <div class="col-12 text-center">
                <div class="alert alert-danger">
                    <p class="mb-0">Error loading history. Please try again later.</p>
                    <a href="index.html" class="btn btn-primary mt-2">Back to Detection</a>
                </div>
            </div>
        `;
    }
} 