package com.example.mtuci_project_practicum_2025;

import android.Manifest;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;


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
        // Кнопка "Начать распознавание" нужно добавить дальнейшую обработку
        Button startRecognitionButton = findViewById(R.id.startRecognitionButton);

        ImageButton backButton = findViewById(R.id.backButton);

        photoView = findViewById(R.id.photoView);
        photoPlaceholder = findViewById(R.id.photoPlaceholder);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        Toast.makeText(this, "Выбрано: " + selectedImageUri, Toast.LENGTH_SHORT).show();
                        showSelectedImage(selectedImageUri);
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
                Intent intent = new Intent(this, ResultScreenActivity.class);
                intent.putExtra("imageUri", selectedImageUri.toString());
                startActivity(intent);
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

    private void openCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private void showSelectedImage(Uri imageUri) {
        if (imageUri != null) {
            photoPlaceholder.setVisibility(TextView.GONE);
            photoView.setVisibility(ImageView.VISIBLE);
            Glide.with(this).load(imageUri).into(photoView);
        }
    }

}
