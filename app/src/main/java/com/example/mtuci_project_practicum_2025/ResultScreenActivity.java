package com.example.mtuci_project_practicum_2025;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResultScreenActivity extends AppCompatActivity {
    private static final String TAG = "ResultScreenActivity";

    private ImageView resultImageView;
    private androidx.recyclerview.widget.RecyclerView resultsRecyclerView;
    private TextView noResultsTextView;
    private Uri currentImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition_result);

        initializeViews();
        processIntentData();
    }

    private void initializeViews() {
        resultImageView = findViewById(R.id.resultImageView);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
        noResultsTextView = findViewById(R.id.noResultsTextView);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        Button reRecognizeButton = findViewById(R.id.reRecognizeButton);
        Button historyButton = findViewById(R.id.historyButton);
        Button newImageButton = findViewById(R.id.newImageButton);

        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        toolbar.setNavigationOnClickListener(v -> finish());

        reRecognizeButton.setOnClickListener(v -> reRecognizeImage());
        historyButton.setOnClickListener(v -> openHistory());
        newImageButton.setOnClickListener(v -> startNewRecognition());
    }

    private void processIntentData() {
        String imageUriStr = getIntent().getStringExtra("imageUri");
        String resultUriStr = getIntent().getStringExtra("resultUri");

        if (imageUriStr == null || resultUriStr == null) {
            showError("Ошибка: отсутствуют данные");
            return;
        }

        currentImageUri = Uri.parse(imageUriStr);
        Uri resultUri = Uri.parse(resultUriStr);

        loadImage(currentImageUri);
        loadResults(resultUri);
    }

    private void loadImage(Uri imageUri) {
        Log.d(TAG, "Загрузка изображения из URI: " + imageUri);
        
        RequestOptions options = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .error(R.drawable.image_placeholder)
            .placeholder(R.drawable.image_placeholder);

        Glide.with(this)
            .load(imageUri)
            .apply(options)
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                    Log.e(TAG, "Ошибка загрузки изображения: " + e.getMessage(), e);
                    showError("Не удалось загрузить изображение");
                    return false;
                }

                @Override
                public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, @NonNull Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                    Log.d(TAG, "Изображение успешно загружено");
                    return false;
                }
            })
            .into(resultImageView);
    }

    private void loadResults(Uri resultUri) {
        try {
            File resultFile = new File(resultUri.getPath());
            if (!resultFile.exists()) {
                showError("Файл результатов не найден");
                return;
            }

            StringBuilder jsonString = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(resultFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonString.append(line);
                }
            }

            JSONObject jsonResult = new JSONObject(jsonString.toString());
            JSONArray labelsArray = jsonResult.getJSONArray("labels");

            List<RecognitionResult> recognitionResults = new ArrayList<>();
            for (int i = 0; i < labelsArray.length(); i++) {
                JSONObject label = labelsArray.getJSONObject(i);
                RecognitionResult recognitionResult = new RecognitionResult(
                    label.getString("text"),
                    (float) label.getDouble("confidence"),
                    ""
                );
                recognitionResults.add(recognitionResult);
            }

            if (recognitionResults.isEmpty()) {
                showNoResults();
            } else {
                showResults(recognitionResults);
            }

        } catch (IOException e) {
            Log.e(TAG, "Ошибка чтения файла результатов", e);
            showError("Ошибка чтения результатов");
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка парсинга JSON", e);
            showError("Ошибка обработки результатов");
        }
    }

    private void showResults(List<RecognitionResult> results) {
        noResultsTextView.setVisibility(View.GONE);
        resultsRecyclerView.setVisibility(View.VISIBLE);
        resultsRecyclerView.setAdapter(new RecognitionResultsAdapter(results));
    }

    private void showNoResults() {
        resultsRecyclerView.setVisibility(View.GONE);
        noResultsTextView.setVisibility(View.VISIBLE);
        noResultsTextView.setText("Объекты не найдены");
    }

    private void reRecognizeImage() {
        if (currentImageUri == null) {
            showError("Ошибка: изображение не найдено");
            return;
        }

        // Показываем прогресс
        Toast.makeText(this, "Распознавание...", Toast.LENGTH_SHORT).show();

        ImageRecognition.recognizeObjectsFromImage(
            this,
            currentImageUri,
            labels -> {
                runOnUiThread(() -> {
                    if (ImageRecognition.recognitionResultFileUri != null) {
                        Intent intent = new Intent(this, ResultScreenActivity.class);
                        intent.putExtra("imageUri", currentImageUri.toString());
                        intent.putExtra("resultUri", ImageRecognition.recognitionResultFileUri.toString());
                        startActivity(intent);
                        finish();
                    } else {
                        showError("Ошибка: не удалось сохранить результаты распознавания");
                    }
                });
            },
            e -> {
                String errorMessage = e.getMessage() != null ? 
                    "Ошибка распознавания: " + e.getMessage() :
                    "Произошла неизвестная ошибка при распознавании";
                showError(errorMessage);
            }
        );
    }

    private void startNewRecognition() {
        Intent intent = new Intent(this, ChooseImageActivity.class);
        startActivity(intent);
        finish();
    }

    private void openHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        resultsRecyclerView.setVisibility(View.GONE);
        noResultsTextView.setVisibility(View.VISIBLE);
        noResultsTextView.setText("Ошибка: " + message);
    }

    private static class RecognitionResult {
        final String name;
        final float confidence;
        final String boundingBox;

        RecognitionResult(String name, float confidence, String boundingBox) {
            this.name = name;
            this.confidence = confidence;
            this.boundingBox = boundingBox;
        }
    }

    private static class RecognitionResultsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<RecognitionResultsAdapter.ViewHolder> {
        private final List<RecognitionResult> results;

        RecognitionResultsAdapter(List<RecognitionResult> results) {
            this.results = results;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setPadding(16, 8, 16, 8);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RecognitionResult result = results.get(position);
            String confidence = String.format("%.1f%%", result.confidence * 100);
            holder.textView.setText(String.format("%s (уверенность: %s)", result.name, confidence));
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final TextView textView;

            ViewHolder(TextView textView) {
                super(textView);
                this.textView = textView;
            }
        }
    }
}
