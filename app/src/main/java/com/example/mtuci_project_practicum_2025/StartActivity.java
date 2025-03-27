package com.example.mtuci_project_practicum_2025;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        TextView title = findViewById(R.id.titleTextView);
        ImageView icon = findViewById(R.id.mainIcon);
        Button startButton = findViewById(R.id.startButton);
        Button historyButton = findViewById(R.id.historyButton);

        // Проверяем, первый ли это запуск
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("isFirstRun", true);

        if (isFirstRun) {
            showWelcomeDialog();
            prefs.edit().putBoolean("isFirstRun", false).apply(); // Ставим флаг, что уже запускали
        }

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Закрываем StartActivity
        });

        historyButton.setOnClickListener(v -> {
            // Пока что пусто, можно потом добавить активити истории
        });
    }

    private void showWelcomeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добро пожаловать!")
                .setMessage("Спасибо за установку приложения! Это ваш первый запуск. Это приложение для распознавания объекта на фото с использованием нейросети. Чтобы продолжить выберите кнопку НАЧАТЬ ")
                .setPositiveButton("Продолжить", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
