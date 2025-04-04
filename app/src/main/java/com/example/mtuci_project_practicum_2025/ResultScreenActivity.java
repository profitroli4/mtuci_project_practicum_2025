package com.example.mtuci_project_practicum_2025;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class ResultScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition_result); // Подключаем макет

        // Находим кнопку "Назад"
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Переход на ChooseImageActivity
                    Intent intent = new Intent(ResultScreenActivity.this, ChooseImageActivity.class);
                    startActivity(intent);
                    finish(); // Закрываем текущую активность
                }
            });
        }
    }
}
