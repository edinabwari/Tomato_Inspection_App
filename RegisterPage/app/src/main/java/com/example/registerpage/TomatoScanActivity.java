package com.example.registerpage;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.registerpage.ml.FinalModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class TomatoScanActivity extends AppCompatActivity {

    private static final String TAG = "TomatoScanActivity";
    private static final float CONFIDENCE_THRESHOLD = 0.85f;
    private static final float LEAF_GREEN_THRESHOLD = 0.3f;
    private static final int REQUEST_PERMISSIONS = 1;
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1 MB
    private static final int MODEL_INPUT_SIZE = 256;

    private ImageView imageView;
    private TextView resultTextView;
    private Button detectButton;
    private Bitmap imageBitmap;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Void> takePictureLauncher;

    private Button viewRecommendationsButton;
    private TextView causesTextView;
    private String currentDisease;

    private Map<String, String> diseaseCauses;

    private Button openGalleryButton;
    private Button startCameraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tomato_scan);

        initializeViews();
        setupButtons();
        setupImageLaunchers();
        checkAndRequestPermissions();
        initializeDiseaseInfo();
    }

    //Links UI components to their corresponding XML elements
    private void initializeViews() {
        imageView = findViewById(R.id.image_display);
        resultTextView = findViewById(R.id.result_text);
        detectButton = findViewById(R.id.detect_button);
        causesTextView = findViewById(R.id.causes_text_view);
        viewRecommendationsButton = findViewById(R.id.view_recommendations_button);
        openGalleryButton = findViewById(R.id.open_gallery_button);
        startCameraButton = findViewById(R.id.start_camera_button);
        detectButton.setEnabled(false);
        viewRecommendationsButton.setVisibility(View.GONE);
    }

    private void setupButtons() {
        openGalleryButton.setOnClickListener(v -> pickImage());
        startCameraButton.setOnClickListener(v -> captureImage());
        detectButton.setOnClickListener(v -> predictDisease());
        viewRecommendationsButton.setOnClickListener(v -> showRecommendations());
    }

    //picking and capturing images.
    private void setupImageLaunchers() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                            imageBitmap = compressImage(imageBitmap);
                            imageView.setImageBitmap(imageBitmap);
                            detectButton.setEnabled(true);
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading image", e);
                            showErrorToast("Error loading image: " + e.getMessage());
                        }
                    }
                });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(),
                result -> {
                    if (result != null) {
                        imageBitmap = compressImage(result);
                        imageView.setImageBitmap(imageBitmap);
                        detectButton.setEnabled(true);
                    } else {
                        showErrorToast("Error capturing image");
                    }
                });
    }

    //Checks permissions
    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
        }
    }
    private void initializeDiseaseInfo() {
        diseaseCauses = new HashMap<>();
        diseaseCauses.put("Tomato_Bacterial_spot",
                "Xanthomonas bacteria that spreads through water splashes and contaminated tools. Once the crop is infected, the disease is very difficult to control and can lead to total crop losses.  \n Click here to learn more 👇");
        diseaseCauses.put("Tomato_Early_blight",
                "Alternaria solani fungus that is favored by warm and humid conditions in soil and on other plants.  \n Click here to learn more 👇");
        diseaseCauses.put("Tomato_Late_blight",
                "Phytophthora infestans that thrives in cool, wet weather.  \n Click here to learn more 👇") ;
        diseaseCauses.put("Tomato_Leaf_Mold",
                "Passalora fulva fungus that is prevalent in high humidity environments that can survive without a host for 6 months to a year to a room temperature. \n Click here to learn more 👇");
        diseaseCauses.put("Tomato_Septoria_leaf_spot",
                "Septoria lycopersici fungus. Favors warm, wet conditions and might spread by overhead water and splashing rain. \n Click here to learn more 👇");
        diseaseCauses.put("Tomato_Spider_mites_Two_spotted_spider_mite",
                "Spider mites from the genus Tetranychus urticae that thrives in hot and dry conditions. \n The mites protect themselves with a cocoon on the underside of the leaf. \n Click here to learn more 👇");
        diseaseCauses.put("Tomato_YellowLeaf_Curl_Virus",
                "Tomato yellow leaf curl virus. Transmitted by white-flies. \n Click here to learn more 👇");
        diseaseCauses.put("Tomato_healthy",
                "No disease detected. The leaf appears to be in good condition. \n Click here to learn more 👇");

    }

    private void pickImage() {
        pickImageLauncher.launch("image/*");
    }

    private void captureImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(null);
        } else {
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
    }

    private void predictDisease() {
        if (imageBitmap == null) {
            showErrorToast("Please upload an image first");
            return;
        }
        resultTextView.setText("Wait a minute 🤔...");
        detectButton.setEnabled(false);
//        viewRecommendationsButton.setVisibility(View.GONE);
        viewRecommendationsButton.setVisibility(View.VISIBLE);

        new Thread(() -> {
            FinalModel model = null;
            try {
                model = FinalModel.newInstance(getApplicationContext());
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true);

                if (!isLikelyLeafImage(resizedBitmap)) {
                    runOnUiThread(() -> {
                        detectButton.setEnabled(true);
                        resultTextView.setText("The image doesn't appear to be a tomato leaf 😞. Please upload a clear image of a tomato leaf.");
                        causesTextView.setText("");
                        viewRecommendationsButton.setVisibility(View.GONE);
                    });
                    return;
                }
// converting and passing the resized image as model input for prediction
                ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedBitmap);

                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, 3}, DataType.FLOAT32);
                inputFeature0.loadBuffer(byteBuffer);

                FinalModel.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
//calculates the confidence levels
                float[] confidences = outputFeature0.getFloatArray();
                Log.d(TAG, "Number of output classes: " + confidences.length);

                int maxPos = 0;
                float maxConfidence = 0;

                for (int i = 0; i < confidences.length; i++) {
                    Log.d(TAG, "Class " + i + " confidence: " + confidences[i]);
                    if (confidences[i] > maxConfidence) {
                        maxConfidence = confidences[i];
                        maxPos = i;
                    }
                }
//UI update with results
                String[] classes = {"Tomato_Bacterial_spot", "Tomato_Early_blight", "Tomato_Late_blight", "Tomato_Leaf_Mold",
                        "Tomato_Septoria_leaf_spot", "Tomato_Spider_mites_Two_spotted_spider_mite",
                        "Tomato_YellowLeaf_Curl_Virus", "Tomato_healthy"};

                String outputLabel;
                if (maxPos < classes.length) {
                    outputLabel = classes[maxPos];
                } else {
                    outputLabel = "Unknown";
                    Log.e(TAG, "Model output index out of range: " + maxPos);
                }

                Log.d(TAG, "Predicted class: " + outputLabel + ", Confidence: " + maxConfidence);

                float finalMaxConfidence = maxConfidence;
                String finalOutputLabel = outputLabel;
                runOnUiThread(() -> {
                    detectButton.setEnabled(true);
                    if (finalMaxConfidence >= CONFIDENCE_THRESHOLD) {
                        currentDisease = finalOutputLabel;
                        resultTextView.setText(finalOutputLabel);

                        String causes = diseaseCauses.getOrDefault(finalOutputLabel, "Unknown causes.");
                        causesTextView.setText("Possible Causes: " + causes);

                        viewRecommendationsButton.setVisibility(View.VISIBLE);
                    } else {
                        resultTextView.setText("Unable to confidently identify the disease 😞. Please upload a clearer image of a tomato leaf.");
                        causesTextView.setText("");
//                        viewRecommendationsButton.setVisibility(View.GONE);
                        viewRecommendationsButton.setVisibility(View.VISIBLE);
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Error loading model", e);
                handlePredictionError(e);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during prediction", e);
                handlePredictionError(e);
            } finally {
                if (model != null) {
                    model.close();
                }
            }
        }).start();
    }

    //Checks if the image is  a leaf based on color analysis
    private boolean isLikelyLeafImage(Bitmap bitmap) {
        int greenPixels = 0;
        int totalPixels = bitmap.getWidth() * bitmap.getHeight();

        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                int pixel = bitmap.getPixel(x, y);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                if (green > red && green > blue) {
                    greenPixels++;
                }
            }
        }

        float greenRatio = (float) greenPixels / totalPixels;
        return greenRatio > LEAF_GREEN_THRESHOLD;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < MODEL_INPUT_SIZE; ++i) {
            for (int j = 0; j < MODEL_INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }
        return byteBuffer;
    }

    private void showRecommendations() {
        if (currentDisease != null) {

            Intent intent = new Intent(TomatoScanActivity.this, InformationActivity.class);
            startActivity(intent);

        } else {
            showErrorToast("No recommendations available.");
        }
    }

    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                showErrorToast("Permissions denied. Cannot access images or camera.");
            }
        }
    }

    //Compresses the image to reduce its size
    private Bitmap compressImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int options = 100;
        while (baos.toByteArray().length > MAX_IMAGE_SIZE && options > 0) {
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
            options -= 10;
        }
        byte[] data = baos.toByteArray();
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    private void handlePredictionError(Exception e) {
        runOnUiThread(() -> {
            detectButton.setEnabled(true);
            resultTextView.setText("Error predicting disease. Please try again.");
//            viewRecommendationsButton.setVisibility(View.GONE);
            viewRecommendationsButton.setVisibility(View.VISIBLE);
            Log.e(TAG, "Error predicting disease", e);
            showErrorToast("An error occurred during prediction: " + e.getMessage());
        });
    }

}