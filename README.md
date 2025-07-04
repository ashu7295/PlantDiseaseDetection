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
- MySQL

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
   - if you want the trained models contact me on this mail i will provide you the models "ashutoshrana972@gmail.com"
     - Trained_Model_Apple.keras
     - Trained_Model_Corn.keras
     - Trained_Model_Grapes.keras
     - Trained_Model_Peach.keras
     - Trained_Model_Tomato.keras

4. Configure the application:
   - Copy `src/main/resources/application.properties.template` to `src/main/resources/application.properties`
   - Update the following properties in `application.properties`:
     - Database credentials
     - Google OAuth credentials (if using Google login)
     - Python app path

5. Build and run the Spring Boot application:
```bash
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8080`

## Configuration

### Database Setup
1. Create a MySQL database named `plant_disease_db`
2. Update the database credentials in `application.properties`

### Google OAuth Setup
1. Go to the [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select an existing one
3. Enable the Google+ API
4. Go to Credentials and create OAuth 2.0 Client ID
5. Add authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
6. Copy the client ID and client secret to `application.properties`

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

Your Name - ashutoshrana972@gmail.com
Project Link: https://github.com/ashu7295/PlantDiseaseDetection 
