package com.example.mtuci_project_practicum_2025;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ImageRecognition {
    private static final String TAG = "ImageRecognition";
    public static Uri recognitionResultFileUri = null;
    private static ImageLabeler labeler = null;
    private static Translator englishRussianTranslator = null;
    private static final float CONFIDENCE_THRESHOLD = 0.6f;
    private static final int MAX_RESULTS = 15;

    private static synchronized ImageLabeler getLabeler() {
        if (labeler == null) {
            ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
                    .build();
            labeler = ImageLabeling.getClient(options);
            Log.d(TAG, "Создан новый экземпляр ImageLabeler с порогом " + CONFIDENCE_THRESHOLD);
        }
        return labeler;
    }

    private static synchronized Translator getTranslator() {
        if (englishRussianTranslator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.RUSSIAN)
                    .build();
            englishRussianTranslator = Translation.getClient(options);
        }
        return englishRussianTranslator;
    }

    private static Bitmap preprocessImage(Bitmap originalBitmap) {
        // Оптимальный размер для ML Kit
        final int TARGET_WIDTH = 640;
        final int TARGET_HEIGHT = 480;

        float aspectRatio = (float) originalBitmap.getWidth() / originalBitmap.getHeight();
        int width = TARGET_WIDTH;
        int height = (int) (TARGET_WIDTH / aspectRatio);

        if (height > TARGET_HEIGHT) {
            height = TARGET_HEIGHT;
            width = (int) (TARGET_HEIGHT * aspectRatio);
        }

        return Bitmap.createScaledBitmap(originalBitmap, width, height, true);
    }

    public static void recognizeObjectsFromImage(Context context, Uri imageUri,
                                               OnSuccessListener<List<ImageLabel>> externalSuccessListener,
                                                 OnFailureListener externalFailureListener) {
        if (context == null || imageUri == null) {
            Log.e(TAG, "Context или Uri равны null");
            if (externalFailureListener != null) {
                externalFailureListener.onFailure(new IllegalArgumentException("Context или Uri равны null"));
            }
            return;
        }

        Log.d(TAG, "Начало распознавания изображения: " + imageUri);

        try {
            // Загружаем изображение
            Bitmap originalBitmap = loadBitmapFromUri(context, imageUri);
            if (originalBitmap == null) {
                throw new IOException("Не удалось загрузить изображение");
            }

            // Предобработка изображения
            Bitmap processedBitmap = preprocessImage(originalBitmap);
            if (originalBitmap != processedBitmap) {
                originalBitmap.recycle();
            }

            InputImage image = InputImage.fromBitmap(processedBitmap, 0);
            Log.d(TAG, "Создан InputImage для ML Kit");

            // Получаем переводчик
            Translator translator = getTranslator();
            
            // Загружаем модель перевода
            DownloadConditions conditions = new DownloadConditions.Builder()
                    .requireWifi()
                    .build();
                    
            translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Модель перевода успешно загружена");
                        // После загрузки модели начинаем распознавание
                        processImageWithTranslation(context, image, processedBitmap, imageUri, translator, externalSuccessListener, externalFailureListener);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка загрузки модели перевода", e);
                        processedBitmap.recycle();
                        if (externalFailureListener != null) {
                            externalFailureListener.onFailure(e);
                        }
                    });

            Log.d(TAG, "Запрос на загрузку модели перевода отправлен");

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обработке изображения", e);
            if (externalFailureListener != null) {
                externalFailureListener.onFailure(e);
            }
        }
    }

    private static void processImageWithTranslation(Context context, InputImage image, Bitmap bitmap,
                                                  Uri imageUri, Translator translator,
                                                  OnSuccessListener<List<ImageLabel>> externalSuccessListener,
                                                  OnFailureListener externalFailureListener) {
        // Создаем задачу распознавания
        Task<List<ImageLabel>> labelingTask = getLabeler().process(image);
        Log.d(TAG, "Создана задача распознавания");

        labelingTask.addOnSuccessListener(labels -> {
            Log.d(TAG, "Успешное распознавание. Найдено меток: " + labels.size());
            if (labels.isEmpty()) {
                Log.w(TAG, "Не найдено объектов с достаточной уверенностью");
                bitmap.recycle();
                if (externalFailureListener != null) {
                    externalFailureListener.onFailure(
                            new RuntimeException("Не удалось распознать объекты на изображении")
                    );
                }
                return;
            }

            // Фильтруем и сортируем результаты по уверенности
            List<ImageLabel> filteredLabels = new ArrayList<>();
            for (ImageLabel label : labels) {
                if (label.getConfidence() >= CONFIDENCE_THRESHOLD) {
                    filteredLabels.add(label);
                    Log.d(TAG, String.format("Найден объект: %s (уверенность: %.2f)",
                            label.getText(), label.getConfidence()));
                }
            }

            // Создаем список задач перевода
            List<Task<String>> translationTasks = new ArrayList<>();
            for (ImageLabel label : filteredLabels) {
                translationTasks.add(translator.translate(label.getText()));
            }

            // Ждем завершения всех переводов
            Tasks.whenAllSuccess(translationTasks)
                    .addOnSuccessListener(translatedTexts -> {
                        List<ImageLabel> translatedLabels = new ArrayList<>();
                        for (int i = 0; i < filteredLabels.size(); i++) {
                            ImageLabel originalLabel = filteredLabels.get(i);
                            String translatedText = (String) translatedTexts.get(i);
                            translatedLabels.add(
                                    new ImageLabel(
                                            translatedText,
                                            originalLabel.getConfidence(),
                                            originalLabel.getIndex()
                                    )
                            );
                            Log.d(TAG, String.format("Распознанный объект: %s (перевод: %s, уверенность: %.2f)",
                                    originalLabel.getText(), translatedText, originalLabel.getConfidence()));
                        }

                        try {
                            Log.d(TAG, "Начинаем сохранение результатов...");
                            saveRecognitionResults(context, imageUri, translatedLabels);
                            Log.d(TAG, "Результаты успешно сохранены в JSON");
                            if (recognitionResultFileUri == null) {
                                throw new IOException("URI результата равен null после сохранения");
                            }
                            Log.d(TAG, "Вызываем callback успешного завершения");
                            if (externalSuccessListener != null) {
                                externalSuccessListener.onSuccess(translatedLabels);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при сохранении результатов", e);
                            if (externalFailureListener != null) {
                                externalFailureListener.onFailure(e);
                            }
                        } finally {
                            bitmap.recycle();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка при переводе меток", e);
                        bitmap.recycle();
                        if (externalFailureListener != null) {
                            externalFailureListener.onFailure(e);
                        }
                    });
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Ошибка при распознавании объектов: " + e.getMessage(), e);
            bitmap.recycle();
            if (externalFailureListener != null) {
                externalFailureListener.onFailure(e);
            }
        });

        Log.d(TAG, "Запрос на распознавание отправлен");
    }

    private static Bitmap loadBitmapFromUri(Context context, Uri imageUri) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                throw new IOException("Не удалось открыть изображение");
            }

            // Сначала проверим размеры изображения
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Вычисляем размер сэмпла для больших изображений
            int sampleSize = 1;
            if (options.outHeight > 1024 || options.outWidth > 1024) {
                sampleSize = Math.max(1, Math.min(options.outWidth / 1024, options.outHeight / 1024));
            }

            Log.d(TAG, String.format("Размеры изображения: %dx%d, sampleSize: %d",
                    options.outWidth, options.outHeight, sampleSize));

            // Пересоздаем поток и загружаем изображение
            inputStream = context.getContentResolver().openInputStream(imageUri);
            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (bitmap == null) {
                throw new IOException("Не удалось декодировать изображение");
            }

            Log.d(TAG, "Изображение успешно декодировано в Bitmap");
            return bitmap;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Ошибка при закрытии потока", e);
                }
            }
        }
    }

    private static void saveRecognitionResults(Context context, Uri imageUri, List<ImageLabel> labels) throws JSONException, IOException {
        Log.d(TAG, "Начало сохранения результатов распознавания");

        // Получаем реальный путь к файлу изображения
        String imagePath = null;
        if ("file".equals(imageUri.getScheme())) {
            imagePath = imageUri.getPath();
        } else {
            try {
                String[] projection = {android.provider.MediaStore.MediaColumns.DATA};
                android.database.Cursor cursor = context.getContentResolver().query(imageUri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA);
                    imagePath = cursor.getString(columnIndex);
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении пути к изображению", e);
            }
        }

        if (imagePath == null) {
            throw new IOException("Не удалось получить путь к изображению");
        }

        JSONObject resultJson = new JSONObject();
        resultJson.put("imageUri", imagePath); // Сохраняем полный путь к файлу
        resultJson.put("timestamp", System.currentTimeMillis());

        JSONArray labelsArray = new JSONArray();
        for (ImageLabel label : labels) {
            if (label != null) {
                JSONObject labelJson = new JSONObject();
                labelJson.put("text", label.getText());
                labelJson.put("confidence", label.getConfidence());
                labelsArray.put(labelJson);
            }
        }
        resultJson.put("labels", labelsArray);

        File dir = new File(context.getExternalFilesDir(null), "Recognition");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Не удалось создать директорию для сохранения результатов");
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File jsonFile = new File(dir, "result_" + timestamp + ".json");

        Log.d(TAG, "Сохранение результатов в файл: " + jsonFile.getAbsolutePath());
        Log.d(TAG, "Путь к изображению: " + imagePath);

        try (FileOutputStream fos = new FileOutputStream(jsonFile)) {
            String jsonString = resultJson.toString(2);
            fos.write(jsonString.getBytes());
            fos.flush();
            Log.d(TAG, "JSON сохранен: " + jsonString);
        }

        recognitionResultFileUri = Uri.fromFile(jsonFile);
        Log.d(TAG, "Результат сохранён: " + recognitionResultFileUri);

        // Проверяем, что файл действительно создан и содержит данные
        if (!jsonFile.exists() || jsonFile.length() == 0) {
            throw new IOException("Файл результатов не создан или пуст: " + jsonFile.getAbsolutePath());
        }
    }
}
