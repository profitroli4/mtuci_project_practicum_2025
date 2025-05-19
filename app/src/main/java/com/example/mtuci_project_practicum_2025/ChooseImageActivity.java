package com.example.mtuci_project_practicum_2025;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public class ChooseImageActivity extends AppCompatActivity {
    private static final String TAG = "ChooseImageActivity";
    private static final int MAX_IMAGE_SIZE = 12 * 1024 * 1024; // 12MB
    private static final String[] REQUIRED_PERMISSIONS = {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                    Manifest.permission.READ_MEDIA_IMAGES :
                    Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private ImageView photoView;
    private TextView photoPlaceholder;
    private Uri selectedImageUri = null;
    private File currentPhotoFile = null;

    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_image);

        initializeViews();
        setupLaunchers();
        setupClickListeners();
    }

    private void initializeViews() {
        Button cameraButton = findViewById(R.id.cameraButton);
        Button galleryButton = findViewById(R.id.galleryButton);
        Button startRecognitionButton = findViewById(R.id.startRecognitionButton);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        photoView = findViewById(R.id.photoView);
        photoPlaceholder = findViewById(R.id.photoPlaceholder);

        toolbar.setNavigationOnClickListener(v -> navigateBack());
        galleryButton.setOnClickListener(v -> checkAndRequestPermission());
        cameraButton.setOnClickListener(v -> checkAndRequestCameraPermission());
        startRecognitionButton.setOnClickListener(v -> startImageRecognition());
    }

    private void setupLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            try {
                                // Сохраняем изображение в кеш приложения
                                Uri cachedImageUri = saveImageToCache(selectedImageUri);
                                if (cachedImageUri != null) {
                                    this.selectedImageUri = cachedImageUri; // Сохраняем URI кешированного изображения
                                    showSelectedImage(cachedImageUri);
                                } else {
                                    showError("Не удалось сохранить изображение");
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Ошибка при сохранении изображения", e);
                                showError("Ошибка при обработке изображения");
                            }
                        }
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Доступ к галерее отклонён", Toast.LENGTH_SHORT).show();
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCameraActivity();
                    } else {
                        Toast.makeText(this, "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListeners() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String imageUriString = result.getData().getStringExtra("imageUri");
                        if (imageUriString != null) {
                            Uri photoUri = Uri.parse(imageUriString);
                            selectedImageUri = photoUri;
                            showSelectedImage(photoUri);
                        }
                    }
                });
    }

    private Uri saveImageToCache(Uri sourceUri) throws IOException {
        // Создаем директорию для сохранения изображений, если она не существует
        File storageDir = new File(getExternalFilesDir(null), "Pictures/Selected");
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("Не удалось создать директорию для сохранения изображений");
        }

        // Создаем уникальное имя файла
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";
        File imageFile = new File(storageDir, imageFileName);

        // Копируем изображение из галереи в кеш приложения
        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            
            if (inputStream == null) {
                throw new IOException("Не удалось открыть исходное изображение");
            }

            // Читаем и записываем данные
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }

        Log.d(TAG, "Изображение сохранено в: " + imageFile.getAbsolutePath());
        return Uri.fromFile(imageFile);
    }

    private void startImageRecognition() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Пожалуйста, выберите фото!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Начало процесса распознавания изображения: " + selectedImageUri);
        
        // Показываем прогресс
        Toast.makeText(this, "Распознавание...", Toast.LENGTH_SHORT).show();

        ImageRecognition.recognizeObjectsFromImage(
            this,
            selectedImageUri,
            labels -> {
                Log.d(TAG, "Распознавание успешно завершено, количество меток: " + labels.size());
                runOnUiThread(() -> {
                    if (ImageRecognition.recognitionResultFileUri != null) {
                        Log.d(TAG, "Переход к экрану результатов с URI: " + ImageRecognition.recognitionResultFileUri);
                        Intent intent = new Intent(this, ResultScreenActivity.class);
                        intent.putExtra("imageUri", selectedImageUri.toString());
                        intent.putExtra("resultUri", ImageRecognition.recognitionResultFileUri.toString());
                        startActivity(intent);
                    } else {
                        Log.e(TAG, "recognitionResultFileUri is null после успешного распознавания");
                        Toast.makeText(this, 
                            "Ошибка: не удалось сохранить результаты распознавания", 
                            Toast.LENGTH_LONG).show();
                    }
                });
            },
            e -> {
                String errorMessage;
                if (e.getMessage() != null) {
                    errorMessage = "Ошибка распознавания: " + e.getMessage();
                } else {
                    errorMessage = "Произошла неизвестная ошибка при распознавании";
                }
                Log.e(TAG, errorMessage, e);
                runOnUiThread(() -> {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        );

        Log.d(TAG, "Запрос на распознавание отправлен из Activity");
    }

    private void navigateBack() {
        Intent intent = new Intent(this, StartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(REQUIRED_PERMISSIONS[0]);
        }
    }

    @SuppressLint("IntentReset")
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCameraActivity();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private ActivityResultLauncher<Intent> cameraLauncher;

    private void openCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        cameraLauncher.launch(intent);
    }

    private void startRecognition(Uri imageUri) {
        Intent intent = new Intent(this, ResultScreenActivity.class);
        intent.putExtra("imageUri", imageUri.toString());
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSelectedImage(Uri imageUri) {
        if (imageUri != null) {
            photoPlaceholder.setVisibility(TextView.GONE);
            photoView.setVisibility(ImageView.VISIBLE);
            
            RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .error(R.drawable.ic_launcher_foreground);

            Glide.with(this)
                .load(imageUri)
                .apply(options)
                .into(photoView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupPreviousFile();
    }

    private void cleanupPreviousFile() {
        if (currentPhotoFile != null && currentPhotoFile.exists()) {
            if (!currentPhotoFile.delete()) {
                Log.w(TAG, "Не удалось удалить предыдущий файл: " + currentPhotoFile.getPath());
            }
        }
    }
}
