import sys
import json
import tensorflow as tf
import numpy as np
from PIL import Image
import logging
import os

# Suppress TensorFlow logging
tf.get_logger().setLevel('ERROR')
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'  # FATAL
os.environ['CUDA_VISIBLE_DEVICES'] = '-1'  # Disable GPU

# Configure logging to write to stderr
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    stream=sys.stderr
)
logger = logging.getLogger(__name__)

# Get the directory where the script is located
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(SCRIPT_DIR, "models")

# Model paths and their corresponding input sizes
MODEL_CONFIGS = {
    "apple": {
        "path": os.path.join(MODELS_DIR, "Trained_Model_Apple.keras"),
        "input_size": (160, 160)
    },
    "corn": {
        "path": os.path.join(MODELS_DIR, "Trained_Model_Corn.keras"),
        "input_size": (160, 160)
    },
    "grape": {
        "path": os.path.join(MODELS_DIR, "Trained_Model_Grapes.keras"),
        "input_size": (160, 160)
    },
    "peach": {
        "path": os.path.join(MODELS_DIR, "Trained_Model_Peach.keras"),
        "input_size": (160, 160)
    },
    "tomato": {
        "path": os.path.join(MODELS_DIR, "Trained_Model_Tomato.keras"),
        "input_size": (128, 128)
    }
}

# Class names for each plant type
CLASS_NAMES = {
    "apple": [
        'Apple___Apple_scab',
        'Apple___Black_rot',
        'Apple___Cedar_apple_rust',
        'Apple___healthy'
    ],
    "corn": [
        'Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot',
        'Corn_(maize)___Common_rust_',
        'Corn_(maize)___Northern_Leaf_Blight',
        'Corn_(maize)___healthy'
    ],
    "grape": [
        'Grape___Black_rot',
        'Grape___Esca_(Black_Measles)',
        'Grape___Leaf_blight_(Isariopsis_Leaf_Spot)',
        'Grape___healthy'
    ],
    "peach": [
        'Peach___Bacterial_spot',
        'Peach___healthy'
    ],
    "tomato": [
        'Tomato___Bacterial_spot',
        'Tomato___Early_blight',
        'Tomato___Late_blight',
        'Tomato___Leaf_Mold',
        'Tomato___Septoria_leaf_spot',
        'Tomato___Spider_mites Two-spotted_spider_mite',
        'Tomato___Target_Spot',
        'Tomato___Tomato_Yellow_Leaf_Curl_Virus',
        'Tomato___Tomato_mosaic_virus',
        'Tomato___healthy'
    ]
}

def model_prediction(test_image, plant_type):
    logger.info(f"Starting prediction for plant type: {plant_type}")
    
    if plant_type not in MODEL_CONFIGS:
        logger.error(f"Unsupported plant type: {plant_type}")
        raise ValueError(f"Unsupported plant type: {plant_type}. Supported types are: {list(MODEL_CONFIGS.keys())}")
    
    model_config = MODEL_CONFIGS[plant_type]
    logger.info(f"Loading model from: {model_config['path']}")
    model = tf.keras.models.load_model(model_config['path'], compile=False)
    
    logger.info(f"Processing image: {test_image}")
    image = Image.open(test_image).convert("RGB")  # Ensure 3 channels
    image = image.resize(model_config['input_size'])  # Resize to model's expected input size
    input_arr = np.array(image) / 255.0  # Normalize to match training
    input_arr = np.expand_dims(input_arr, axis=0)  # Add batch dimension
    
    logger.info("Making prediction...")
    predictions = model.predict(input_arr, verbose=0)
    result_index = np.argmax(predictions)
    logger.info(f"Prediction complete. Result index: {result_index}")
    
    return result_index, predictions

if __name__ == "__main__":
    logger.info("Script started")
    logger.info(f"Arguments received: {sys.argv}")
    
    if len(sys.argv) != 3:
        error_msg = "Usage: python plant_disease_detector.py <image_path> <plant_type>"
        logger.error(error_msg)
        json_output = {
            "status": "error",
            "message": error_msg
        }
        print(json.dumps(json_output), flush=True)
        sys.exit(1)
        
    try:
        image_path = sys.argv[1]
        plant_type = sys.argv[2].lower()
        logger.info(f"Processing image: {image_path} for plant type: {plant_type}")
        
        if plant_type not in MODEL_CONFIGS:
            error_msg = f"Unsupported plant type: {plant_type}. Supported types are: {list(MODEL_CONFIGS.keys())}"
            logger.error(error_msg)
            json_output = {
                "status": "error",
                "message": error_msg
            }
            print(json.dumps(json_output), flush=True)
            sys.exit(1)
            
        result_index, predictions = model_prediction(image_path, plant_type)
        
        json_output = {
            "status": "success",
            "plant_type": plant_type,
            "prediction": CLASS_NAMES[plant_type][result_index],
            "confidence_scores": predictions.tolist()
        }
        logger.info(f"Prediction result: {json_output['prediction']}")
        logger.info("Writing JSON output to stdout")
        print(json.dumps(json_output), flush=True)
        logger.info("JSON output written successfully")
        
    except Exception as e:
        logger.error(f"Error during prediction: {str(e)}", exc_info=True)
        json_output = {
            "status": "error",
            "message": str(e)
        }
        print(json.dumps(json_output), flush=True)
        sys.exit(1)
