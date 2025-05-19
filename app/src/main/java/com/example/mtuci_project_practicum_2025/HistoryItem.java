package com.example.mtuci_project_practicum_2025;

import android.net.Uri;
import java.util.Date;

public class HistoryItem {
    private final Uri imageUri;
    private final Uri resultUri;
    private final Date timestamp;
    private final String previewText;
    private final String fullJson;

    public HistoryItem(Uri imageUri, Uri resultUri, Date timestamp, String previewText, String fullJson) {
        this.imageUri = imageUri;
        this.resultUri = resultUri;
        this.timestamp = timestamp;
        this.previewText = previewText;
        this.fullJson = fullJson;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public Uri getResultUri() {
        return resultUri;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getPreviewText() {
        return previewText;
    }

    public String getFullJson() {
        return fullJson;
    }
} 