import subprocess
import json
import os

def test_plant_detection(image_path, plant_type):
    try:
        # Run the plant disease detector script
        result = subprocess.run(
            ['python3', 'plant_disease_detector.py', image_path, plant_type],
            capture_output=True,
            text=True
        )
        
        # Find the JSON output in the stdout
        stdout_lines = result.stdout.strip().split('\n')
        json_output = None
        for line in stdout_lines:
            if line.startswith('{') and line.endswith('}'):
                json_output = line
                break
        
        if json_output:
            # Parse the JSON output
            output = json.loads(json_output)
            
            if output['status'] == 'success':
                print(f"\nSuccessfully detected disease for {plant_type}")
                print(f"Prediction: {output['prediction']}")
                print("\nConfidence scores for each class:")
                for i, score in enumerate(output['confidence_scores'][0]):
                    print(f"Class {i}: {score:.4f}")
            else:
                print(f"Error: {output['message']}")
        else:
            print("No valid JSON output found in the response")
            print("Raw stdout:", result.stdout)
            
    except Exception as e:
        print(f"Error running detection: {str(e)}")
        print("Full error details:", str(e))

if __name__ == "__main__":
    # Test with a sample image (you'll need to provide a valid image path)
    test_image = "test_image.JPG"  # Updated to match actual filename
    plant_type = "tomato"  # Replace with the plant type you want to test
    
    if os.path.exists(test_image):
        test_plant_detection(test_image, plant_type)
    else:
        print(f"Test image not found: {test_image}") 