package com.mentee.countlives;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import com.bumptech.glide.Glide;
import androidx.annotation.Nullable;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(ActivityEntry entry);
    }
    
    public interface OnDeleteClickListener {
        void onDelete(int position);
    }

    private final List<ActivityEntry> entries;
    private final OnItemClickListener listener;
    private final OnDeleteClickListener deleteListener;

    public ActivityAdapter(List<ActivityEntry> entries, OnItemClickListener listener) {
        this(entries, listener, null);
    }

    public ActivityAdapter(List<ActivityEntry> entries, OnItemClickListener listener, OnDeleteClickListener deleteListener) {
        this.entries = entries;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityEntry entry = entries.get(position);
        android.util.Log.i("ActivityAdapter", "onBindViewHolder pos=" + position + " type=" + entry.type + " imageUrl=" + entry.imageUrl);
        holder.type.setText(entry.type);
        holder.duration.setText(entry.durationMinutes + " min");
        final String remoteFallback = "https://images.squarespace-cdn.com/content/v1/64a5428b0a8e4f5a25060263/4299df31-6dda-475e-a87b-c9069ffd3277/Olympic+weightlifting+-+Fortress+Gym+4.jpg";

        if (entry.imageUrl != null && !entry.imageUrl.isEmpty()) {
            try {
                com.bumptech.glide.request.RequestOptions options = new com.bumptech.glide.request.RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop();

                Glide.with(holder.itemView.getContext()).load(entry.imageUrl).apply(options)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            // If the user-supplied URL fails, try the remote fallback image
                            try {
                                Glide.with(holder.itemView.getContext()).load(remoteFallback).apply(options).into(holder.thumbnail);
                            } catch (Exception ex) {
                                holder.thumbnail.setImageResource(R.drawable.ic_image_placeholder);
                                android.util.Log.w("ActivityAdapter", "Remote fallback failed", ex);
                            }
                            return true; // we've handled the failure by attempting fallback
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    }).into(holder.thumbnail);
            } catch (Exception e) {
                holder.thumbnail.setImageResource(R.drawable.ic_image_placeholder);
                android.util.Log.w("ActivityAdapter", "Failed to load image", e);
            }
        } else {
            // No user URL — try the remote fallback first, then placeholder
            try {
                com.bumptech.glide.request.RequestOptions options = new com.bumptech.glide.request.RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop();
                Glide.with(holder.itemView.getContext()).load(remoteFallback).apply(options).into(holder.thumbnail);
            } catch (Exception e) {
                holder.thumbnail.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
        holder.itemView.setOnClickListener(v -> {
            try {
                listener.onItemClick(entry);
            } catch (Exception e) {
                android.util.Log.w("ActivityAdapter", "onItemClick handler failed", e);
            }
        });
        
        // Long press to show delete button
        holder.itemView.setOnLongClickListener(v -> {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(deleteV -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(position);
                }
            });
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView type;
        TextView duration;
        android.widget.ImageView thumbnail;
        com.google.android.material.button.MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.tvActivityType);
            duration = itemView.findViewById(R.id.tvActivityDuration);
            thumbnail = itemView.findViewById(R.id.ivActivityThumb);
            btnDelete = itemView.findViewById(R.id.btnDeleteActivity);
        }
    }
}
