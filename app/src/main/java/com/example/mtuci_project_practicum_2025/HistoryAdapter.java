package com.example.mtuci_project_practicum_2025;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private static final String TAG = "HistoryAdapter";
    private final List<HistoryItem> items;
    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(HistoryItem item);
    }

    public HistoryAdapter(List<HistoryItem> items, OnHistoryItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        
        // Форматируем дату
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String formattedDate = dateFormat.format(item.getTimestamp());
        
        holder.dateTextView.setText(formattedDate);
        holder.previewTextView.setText(item.getPreviewText());

        // Загружаем изображение с обработкой ошибок
        RequestOptions options = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .error(R.drawable.image_placeholder)
            .placeholder(R.drawable.image_placeholder);

        Log.d(TAG, "Загрузка изображения из URI: " + item.getImageUri());

        Glide.with(holder.itemView.getContext())
            .load(item.getImageUri())
            .apply(options)
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                    Log.e(TAG, "Ошибка загрузки изображения для истории", e);
                    return false;
                }

                @Override
                public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, @NonNull Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            })
            .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<HistoryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView dateTextView;
        final TextView previewTextView;

        HistoryViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.historyImageView);
            dateTextView = itemView.findViewById(R.id.historyDateTextView);
            previewTextView = itemView.findViewById(R.id.historyPreviewTextView);
        }
    }
} 