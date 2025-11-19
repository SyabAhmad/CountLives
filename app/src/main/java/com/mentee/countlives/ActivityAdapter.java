package com.mentee.countlives;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(ActivityEntry entry);
    }

    private final List<ActivityEntry> entries;
    private final OnItemClickListener listener;

    public ActivityAdapter(List<ActivityEntry> entries, OnItemClickListener listener) {
        this.entries = entries;
        this.listener = listener;
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
        holder.type.setText(entry.type);
        holder.duration.setText(entry.durationMinutes + " min");
        holder.itemView.setOnClickListener(v -> listener.onItemClick(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView type;
        TextView duration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.tvActivityType);
            duration = itemView.findViewById(R.id.tvActivityDuration);
        }
    }
}
