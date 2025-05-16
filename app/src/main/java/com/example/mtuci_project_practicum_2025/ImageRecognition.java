package com.example.mtuci_project_practicum_2025;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImageRecognition {

    private static final String TAG = "ImageRecognition";
    public static Uri recognitionResultFileUri = null;

    public static void recognizeObjectsFromImage(Context context, Uri imageUri,
                                                 OnSuccessListener<List<DetectedObject>> externalSuccessListener,
                                                 OnFailureListener externalFailureListener) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(context, "Не удалось открыть изображение", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            ObjectDetectorOptions options =
                    new ObjectDetectorOptions.Builder()
                            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                            .enableMultipleObjects()
                            .enableClassification()
                            .build();

            ObjectDetector detector = ObjectDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(detectedObjects -> {
                        try {

                            JSONObject resultJson = new JSONObject();
                            resultJson.put("imageUri", imageUri.toString());

                            JSONArray objectsArray = new JSONArray();
                            for (DetectedObject obj : detectedObjects) {
                                JSONObject objectJson = new JSONObject();
                                if (!obj.getLabels().isEmpty()) {
                                    objectJson.put("name", obj.getLabels().get(0).getText());
                                    objectJson.put("confidence", obj.getLabels().get(0).getConfidence());
                                } else {
                                    objectJson.put("name", "Неизвестный объект");
                                }

                                objectJson.put("boundingBox", obj.getBoundingBox().flattenToString());
                                objectsArray.put(objectJson);
                            }

                            resultJson.put("results", objectsArray);


                            File dir = new File(context.getCacheDir(), "recognition");
                            if (!dir.exists()) dir.mkdirs();

                            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                            File jsonFile = new File(dir, "result_" + timestamp + ".json");

                            try (FileOutputStream fos = new FileOutputStream(jsonFile)) {
                                fos.write(resultJson.toString(2).getBytes());
                            }


                            recognitionResultFileUri = Uri.fromFile(jsonFile);
                            Log.d(TAG, "Результат сохранён: " + recognitionResultFileUri);

                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при сохранении JSON", e);
                        }

                        externalSuccessListener.onSuccess(detectedObjects);
                    })
                    .addOnFailureListener(externalFailureListener);

        } catch (IOException e) {
            Toast.makeText(context, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Ошибка загрузки изображения", e);
        }
    }
}
