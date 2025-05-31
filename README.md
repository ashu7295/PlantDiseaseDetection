# Plant Disease Detection

A web application that uses machine learning to detect diseases in plants. The application supports multiple plant types including apple, corn, grape, peach, and tomato.

## Features

- Real-time plant disease detection
- Support for multiple plant types
- User authentication and profile management
- Detailed analysis with confidence scores
- History tracking of analyses

## Prerequisites

- Java 17 or higher
- Python 3.8 or higher
- Maven
- pip (Python package manager)

## Installation

1. Clone the repository:
```bash
git clone https://github.com/ashu7295/PlantDiseaseDetection.git
cd PlantDiseaseDetection
```

2. Install Python dependencies:
```bash
cd python_app
pip install -r requirements.txt
```

3. Download the model files:
   - Create a directory: `python_app/python_app/models/`
   - Download the model files from [Google Drive](https://drive.google.com/drive/folders/your-folder-id) and place them in the models directory:
     - Trained_Model_Apple.keras
     - Trained_Model_Corn.keras
     - Trained_Model_Grapes.keras
     - Trained_Model_Peach.keras
     - Trained_Model_Tomato.keras

4. Build and run the Spring Boot application:
```bash
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8080`

## Usage

1. Register a new account or login
2. Select the plant type
3. Upload an image of the plant
4. View the analysis results

## Model Files

Due to their large size, the model files are not included in the repository. You can download them from:
[Google Drive Link]

Place the downloaded files in the `python_app/python_app/models/` directory.

## Technologies Used

- Spring Boot
- Thymeleaf
- Bootstrap
- Python
- TensorFlow/Keras
- MySQL

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

Your Name - your.email@example.com
Project Link: https://github.com/ashu7295/PlantDiseaseDetection 