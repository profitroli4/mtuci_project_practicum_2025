package com.example.mtuci_project_practicum_2025;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private ImageView capturedImage;
    private Button captureButton, retakeButton;
    private ImageButton acceptButton;
    private ImageCapture imageCapture;
    private Executor executor;
    private File photoFile;

    private ActivityResultLauncher<String> storagePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        executor = ContextCompat.getMainExecutor(this);

        viewFinder = findViewById(R.id.viewFinder);
        capturedImage = findViewById(R.id.capturedImage);
        captureButton = findViewById(R.id.captureButton);
        retakeButton = findViewById(R.id.retakeButton);
        acceptButton = findViewById(R.id.acceptButton);

        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        returnToChooseImageActivity();
                    } else {
                        Toast.makeText(this,
                                "Для сохранения фото необходимо разрешение на запись в хранилище",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        startCamera();

        captureButton.setOnClickListener(v -> takePhoto());
        retakeButton.setOnClickListener(v -> retakePhoto());
        acceptButton.setOnClickListener(v -> checkStoragePermissionAndReturn());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                );
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Ошибка инициализации камеры", Toast.LENGTH_SHORT).show();
            }
        }, executor);
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        photoFile = createImageFile();
        if (photoFile == null) {
            Toast.makeText(this, "Ошибка при создании файла", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                executor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        runOnUiThread(() -> {
                            capturedImage.setImageURI(Uri.fromFile(photoFile));
                            capturedImage.setVisibility(View.VISIBLE);
                            viewFinder.setVisibility(View.GONE);
                            captureButton.setVisibility(View.GONE);
                            retakeButton.setVisibility(View.VISIBLE);
                            acceptButton.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(CameraActivity.this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkStoragePermissionAndReturn() {
        if (photoFile == null) {
            Toast.makeText(this, "Фото не найдено", Toast.LENGTH_SHORT).show();
            return;
        }

        // Для Android 10 (API 29) и выше разрешение WRITE_EXTERNAL_STORAGE не требуется
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
            returnToChooseImageActivity();
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void retakePhoto() {
        capturedImage.setVisibility(View.GONE);
        viewFinder.setVisibility(View.VISIBLE);
        retakeButton.setVisibility(View.GONE);
        acceptButton.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
    }

    private void returnToChooseImageActivity() {
        Intent intent = new Intent();
        intent.putExtra("selectedImageURL", photoFile.getAbsolutePath());
        setResult(RESULT_OK, intent);
        finish();
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(getCacheDir(), "photos");
        if (!storageDir.exists()) {
            // Если папка не существует, создаем её
            if (!storageDir.mkdirs()) {
                Toast.makeText(this, "Ошибка создания папки для фото", Toast.LENGTH_SHORT).show();
                return null;
            }
        };

        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}