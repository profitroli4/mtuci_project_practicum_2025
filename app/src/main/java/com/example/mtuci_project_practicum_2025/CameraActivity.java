package com.example.mtuci_project_practicum_2025;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ? Manifest.permission.WRITE_EXTERNAL_STORAGE : ""
    };

    private PreviewView viewFinder;
    private ImageView capturedImage;
    private Button captureButton, retakeButton;
    private ImageButton acceptButton;
    private ImageCapture imageCapture;
    private Executor executor;
    private File photoFile;
    private ProcessCameraProvider cameraProvider;

    private ActivityResultLauncher<String[]> permissionsLauncher;

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

        permissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean isGranted : permissions.values()) {
                        allGranted &= isGranted;
                    }
                    if (allGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(this,
                                "Для работы приложения необходимы разрешения на камеру и хранилище",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

        if (allPermissionsGranted()) {
        startCamera();
        } else {
            requestPermissions();
        }

        captureButton.setOnClickListener(v -> takePhoto());
        retakeButton.setOnClickListener(v -> retakePhoto());
        acceptButton.setOnClickListener(v -> returnToChooseImageActivity());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Пересоздаем превью камеры при повороте экрана
        if (cameraProvider != null) {
            startCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (permission.isEmpty()) continue;
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        permissionsLauncher.launch(REQUIRED_PERMISSIONS);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

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
                String errorMessage = "Ошибка инициализации камеры: " + e.getMessage();
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        }, executor);
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Камера не инициализирована", Toast.LENGTH_SHORT).show();
            return;
        }

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
                            try {
                            capturedImage.setImageURI(Uri.fromFile(photoFile));
                            capturedImage.setVisibility(View.VISIBLE);
                            viewFinder.setVisibility(View.GONE);
                            captureButton.setVisibility(View.GONE);
                            retakeButton.setVisibility(View.VISIBLE);
                            acceptButton.setVisibility(View.VISIBLE);
                            } catch (Exception e) {
                                Toast.makeText(CameraActivity.this,
                                        "Ошибка отображения фото: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        runOnUiThread(() -> {
                            String errorMessage = "Ошибка сохранения фото: " + exception.getMessage();
                            Toast.makeText(CameraActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    private void retakePhoto() {
        if (photoFile != null && photoFile.exists()) {
            photoFile.delete();
        }
        capturedImage.setVisibility(View.GONE);
        viewFinder.setVisibility(View.VISIBLE);
        retakeButton.setVisibility(View.GONE);
        acceptButton.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
    }

    private void returnToChooseImageActivity() {
        if (photoFile == null || !photoFile.exists()) {
            Toast.makeText(this, "Фото не найдено", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent();
        intent.putExtra("selectedImageURL", photoFile.getAbsolutePath());
        setResult(RESULT_OK, intent);
        finish();
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Для Android 10 и выше используем getExternalFilesDir
            storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Camera");
        } else {
            // Для более старых версий используем общую директорию фотографий
            storageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "Camera");
        }

        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e("CameraActivity", "Не удалось создать директорию: " + storageDir.getPath());
                return null;
            }

        try {
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            return image;
        } catch (IOException e) {
            Log.e("CameraActivity", "Ошибка создания файла: " + e.getMessage());
            return null;
        }
    }
}