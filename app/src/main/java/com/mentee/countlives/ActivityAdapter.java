package com.mentee.countlives;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import com.bumptech.glide.Glide;

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
        if (entry.imageUrl != null && !entry.imageUrl.isEmpty()) {
            try {
                Glide.with(holder.itemView.getContext()).load(entry.imageUrl).placeholder(R.drawable.ic_image_placeholder).error(R.drawable.ic_image_placeholder).into(holder.thumbnail);
            } catch (Exception e) {
                holder.thumbnail.setImageResource(R.drawable.ic_image_placeholder);
                android.util.Log.w("ActivityAdapter", "Failed to load image", e);
            }
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_image_placeholder);
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
