package com.example.mtuci_project_practicum_2025;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";
    private RecyclerView historyRecyclerView;
    private TextView emptyHistoryText;
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initializeViews();
        loadHistory();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        emptyHistoryText = findViewById(R.id.emptyHistoryText);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(new ArrayList<>(), this::onHistoryItemClick);
        historyRecyclerView.setAdapter(historyAdapter);
    }

    private void loadHistory() {
        File storageDir = new File(getExternalFilesDir(null), "Recognition");
        Log.d(TAG, "Ищем файлы в директории: " + storageDir.getAbsolutePath());
        
        if (!storageDir.exists()) {
            Log.d(TAG, "Директория не существует");
            showEmptyHistory();
            return;
        }

        List<HistoryItem> historyItems = new ArrayList<>();
        File[] files = storageDir.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (files == null) {
            Log.d(TAG, "files == null");
            showEmptyHistory();
            return;
        }
        
        Log.d(TAG, "Найдено файлов: " + files.length);

        if (files.length == 0) {
            Log.d(TAG, "Нет файлов в директории");
            showEmptyHistory();
            return;
        }

        // Сортируем файлы по дате изменения (новые сверху)
        Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        for (File file : files) {
            try {
                Log.d(TAG, "Обработка файла: " + file.getName());
                String jsonContent = readJsonFile(file);
                JSONObject jsonObject = new JSONObject(jsonContent);
                
                String imageUri = jsonObject.getString("imageUri");
                long timestamp = jsonObject.getLong("timestamp");
                
                JSONArray labels = jsonObject.getJSONArray("labels");
                StringBuilder previewText = new StringBuilder();
                for (int i = 0; i < Math.min(labels.length(), 2); i++) {
                    JSONObject label = labels.getJSONObject(i);
                    if (i > 0) previewText.append(", ");
                    previewText.append(label.getString("text"));
                }

                HistoryItem item = new HistoryItem(
                    Uri.parse(imageUri),
                    Uri.fromFile(file),
                    new Date(timestamp),
                    previewText.toString(),
                    jsonContent
                );
                historyItems.add(item);
                Log.d(TAG, "Файл успешно обработан: " + file.getName());
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при чтении файла истории: " + file.getName(), e);
            }
        }

        if (historyItems.isEmpty()) {
            Log.d(TAG, "Нет элементов истории после обработки файлов");
            showEmptyHistory();
        } else {
            Log.d(TAG, "Загружено элементов истории: " + historyItems.size());
            historyAdapter.updateItems(historyItems);
            emptyHistoryText.setVisibility(View.GONE);
            historyRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private String readJsonFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private void showEmptyHistory() {
        emptyHistoryText.setVisibility(View.VISIBLE);
        historyRecyclerView.setVisibility(View.GONE);
    }

    private void onHistoryItemClick(HistoryItem item) {
        Intent intent = new Intent(this, ResultScreenActivity.class);
        intent.putExtra("imageUri", item.getImageUri().toString());
        intent.putExtra("resultUri", item.getResultUri().toString());
        startActivity(intent);
    }
} 