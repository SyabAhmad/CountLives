package com.mentee.countlives;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {
    private RecyclerView rv;
    private List<ActivityEntry> activities = new ArrayList<>();
    private ActivityAdapter adapter;
    private RecyclerView.OnItemTouchListener recyclerItemClickListener;
    private Gson gson = new Gson();
    private android.widget.TextView tvEmptyState;
    private android.widget.LinearLayout llEmptyStateContainer;
    private boolean showingAll = false;

    private static final String PREFS = "CountLivesPrefs";
    private static final String KEY_ACTIVITIES = "activities";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        rv = v.findViewById(R.id.rvActivitiesFrag);
        tvEmptyState = v.findViewById(R.id.tvEmptyState);
        llEmptyStateContainer = v.findViewById(R.id.llEmptyStateContainer);
        com.google.android.material.button.MaterialButton btnAdd = v.findViewById(R.id.btnAddInHistory);
        com.google.android.material.button.MaterialButton btnShowMore = v.findViewById(R.id.btnShowMore);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ActivityAdapter(activities, this::showActivityImage, position -> {
            // Delete callback
            new AlertDialog.Builder(getContext())
                    .setMessage("Delete this activity?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        activities.remove(position);
                        saveActivities();
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(activities.isEmpty() ? View.VISIBLE : View.GONE);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rv.setAdapter(adapter);

        // long press to delete
        recyclerItemClickListener = new RecyclerItemClickListener(getContext(), rv, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                try {
                    if (position < 0 || position >= activities.size()) return;
                    ActivityEntry entry = activities.get(position);
                    if (entry == null) return;
                    if (entry.imageUrl != null && !entry.imageUrl.isEmpty()) {
                        android.content.Intent intent = new android.content.Intent(getContext(), ImageActivity.class);
                        intent.putExtra(ImageActivity.EXTRA_URL, entry.imageUrl);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    android.util.Log.w("HistoryFragment", "Failed to handle item click", e);
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {
                new AlertDialog.Builder(getContext())
                        .setMessage("Delete this activity?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            activities.remove(position);
                            saveActivities();
                            adapter.notifyDataSetChanged();
                            tvEmptyState.setVisibility(activities.isEmpty() ? View.VISIBLE : View.GONE);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        if (rv != null && recyclerItemClickListener != null) rv.addOnItemTouchListener(recyclerItemClickListener);

        btnAdd.setOnClickListener(v1 -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).launchAddActivity();
            }
        });
        btnShowMore.setOnClickListener(v12 -> {
            showingAll = !showingAll;
            btnShowMore.setText(showingAll ? "Show less" : "Show all");
            loadActivities();
        });

        loadActivities();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadActivities();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Avoid holding references to view objects after the view is destroyed
        rv = null;
        adapter = null;
        tvEmptyState = null;
        llEmptyStateContainer = null;
        // If an item click listener was attached, remove it now to avoid callbacks to a destroyed fragment
        if (recyclerItemClickListener != null && rv != null) {
            try {
                rv.removeOnItemTouchListener(recyclerItemClickListener);
            } catch (Exception e) {
                android.util.Log.w("HistoryFragment", "Failed to remove RecyclerItemClickListener", e);
            }
        }
    }

    private void saveActivities() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACTIVITIES, gson.toJson(activities));
        editor.apply();
    }

    public void loadActivities() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ACTIVITIES, null);
        android.util.Log.i("HistoryFragment", "loadActivities: json=" + json);
        activities.clear();
        if (json != null) {
            Type listType = new TypeToken<List<ActivityEntry>>(){}.getType();
            List<ActivityEntry> saved = gson.fromJson(json, listType);
            android.util.Log.i("HistoryFragment", "loadActivities: parsed saved size=" + (saved == null ? 0 : saved.size()));
            if (saved != null) activities.addAll(saved);
        }
        // If showingAll is true, show everything. Otherwise show up to 5 items by default.
        int limit = showingAll ? Integer.MAX_VALUE : 5;
        while (activities.size() > limit) {
            activities.remove(activities.size() - 1);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        boolean empty = activities.isEmpty();
        if (tvEmptyState != null) tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (llEmptyStateContainer != null) llEmptyStateContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (rv != null) rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        android.util.Log.i("HistoryFragment", "loadActivities: activities after trim=" + activities.size() + ", adapterCount=" + (adapter == null ? 0 : adapter.getItemCount()));
    }

    private void showActivityImage(ActivityEntry entry) {
        // Use a custom dialog with spinner overlay
        if (getContext() == null) return;
        final android.view.View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_image_preview, null);
        final ImageView iv = dialogView.findViewById(R.id.ivDialogImage);
        final android.widget.ProgressBar pb = dialogView.findViewById(R.id.pbDialogImage);
        pb.setVisibility(View.VISIBLE);
        if (entry.imageUrl != null && !entry.imageUrl.isEmpty()) {
                com.bumptech.glide.request.RequestOptions options = new com.bumptech.glide.request.RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                    .centerCrop();
            try {
                com.bumptech.glide.Glide.with(this).load(entry.imageUrl).apply(options).listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                    pb.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                    pb.setVisibility(View.GONE);
                    return false;
                }
                }).into(iv);
            } catch (Exception e) {
                pb.setVisibility(View.GONE);
                iv.setImageResource(R.drawable.ic_image_placeholder);
                android.util.Log.w("HistoryFragment", "Glide failed to load image: " + entry.imageUrl, e);
            }
        } else {
            pb.setVisibility(View.GONE);
            iv.setImageResource(R.drawable.ic_launcher_foreground);
        }
        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
