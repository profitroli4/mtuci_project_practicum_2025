package com.example.mtuci_project_practicum_2025;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;


public class ChooseImageActivity extends AppCompatActivity {

    private ImageView photoView;
    private TextView photoPlaceholder;
    private Uri selectedImageUri = null;

    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_image);

        Button cameraButton = findViewById(R.id.cameraButton);
        Button galleryButton = findViewById(R.id.galleryButton);
        Button startRecognitionButton = findViewById(R.id.startRecognitionButton);

        ImageButton backButton = findViewById(R.id.backButton);

        photoView = findViewById(R.id.photoView);
        photoPlaceholder = findViewById(R.id.photoPlaceholder);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedUri = result.getData().getData();

                        if (selectedUri == null) {
                            Toast.makeText(this, "Ошибка: выбранный URI пуст", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        String imageFileName = "JPEG_" + timeStamp + "_";

                        File storageDir = new File(getCacheDir(), "photos");
                        if (!storageDir.exists()) {
                            // Если папка не существует, создаем её
                            if (!storageDir.mkdirs()) {
                                Toast.makeText(this, "Ошибка создания папки для фото", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        File cachedFile = new File(storageDir, imageFileName + ".jpg");


                        try (InputStream inputStream = getContentResolver().openInputStream(selectedUri)) {
                            if (inputStream == null) {
                                Toast.makeText(this, "Ошибка: не удалось открыть поток", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            try (OutputStream outputStream = new FileOutputStream(cachedFile)) {
                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, length);
                                }

                                selectedImageUri = Uri.fromFile(cachedFile); // Используем ссылку на кешированное изображение
                                Toast.makeText(this, "Выбрано: " + selectedImageUri, Toast.LENGTH_SHORT).show();
                                showSelectedImage(selectedImageUri);
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Ошибка сохранения изображения в кеш", Toast.LENGTH_SHORT).show();
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

        galleryButton.setOnClickListener(v -> checkAndRequestPermission());
        cameraButton.setOnClickListener(v -> checkAndRequestCameraPermission());

        startRecognitionButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {

                Toast.makeText(this, "Распознавание...", Toast.LENGTH_SHORT).show();

                ImageRecognition.recognizeObjectsFromImage(
                        this,
                        selectedImageUri,
                        detectedObjects -> {

                            if (ImageRecognition.recognitionResultFileUri != null) {
                                Intent intent = new Intent(this, ResultScreenActivity.class);
                                intent.putExtra("imageUri", selectedImageUri.toString());
                                intent.putExtra("resultUri", ImageRecognition.recognitionResultFileUri.toString());
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Ошибка: JSON-файл не найден", Toast.LENGTH_SHORT).show();
                            }
                        },
                        e -> {
                            Toast.makeText(this, "Ошибка распознавания", Toast.LENGTH_SHORT).show();
                        }
                );
            } else {
                Toast.makeText(this, "Пожалуйста, выберите фото!", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StartActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void checkAndRequestPermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            // Android 12 и ниже
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(permission);
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


    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String imagePath = result.getData().getStringExtra("selectedImageURL");
                    assert imagePath != null;
                    selectedImageUri = Uri.fromFile(new File(imagePath));
                    showSelectedImage(selectedImageUri);
                }
            });

    private void openCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        cameraLauncher.launch(intent);
    }

    private void showSelectedImage(Uri imageUri) {
        if (imageUri != null) {
            photoPlaceholder.setVisibility(TextView.GONE);
            photoView.setVisibility(ImageView.VISIBLE);
            Glide.with(this).load(imageUri).into(photoView);
        }
    }

}
