package com.mentee.countlives;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
// removed menu imports for ReportActivity

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "CountLivesPrefs";
    private static final String KEY_ACTIVITIES = "activities";

    private List<ActivityEntry> activities = new ArrayList<>();
    private Gson gson = new Gson();

    private ActivityResultLauncher<Intent> addActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        // Removed setSupportActionBar to avoid theme conflicts
        // ViewPager + Tabs setup
        ViewPager2 viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public androidx.fragment.app.Fragment createFragment(int position) {
                return position == 0 ? new StepsFragment() : new HistoryFragment();
            }

            @Override
            public int getItemCount() { return 2; }
        });
        TabLayout tabLayoutBottom = findViewById(R.id.tabLayoutBottom);
        new TabLayoutMediator(tabLayoutBottom, viewPager, (tab, position) -> {
            tab.setIcon(position == 0 ? R.drawable.ic_tab_steps : R.drawable.ic_tab_history);
            tab.setText(null);
        }).attach();

        // Setup activity result launcher before wiring UI so it's safe during rotation and click events
        addActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // Refresh list after add activity
            loadActivities();
        });

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddActivity.class);
            if (addActivityLauncher != null) {
                addActivityLauncher.launch(intent);
            } else {
                // fallback
                startActivity(intent);
            }
        });

        loadActivities();

        // Request ACTIVITY_RECOGNITION permission on Android Q (29) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1001);
            }
        }
    }

    // Removed menu options for ReportActivity per user request

    private void showActivityImage(ActivityEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ImageView iv = new ImageView(this);
        iv.setAdjustViewBounds(true);
        final String remoteFallback = "https://images.squarespace-cdn.com/content/v1/64a5428b0a8e4f5a25060263/4299df31-6dda-475e-a87b-c9069ffd3277/Olympic+weightlifting+-+Fortress+Gym+4.jpg";

        if (entry.imageUrl != null && !entry.imageUrl.isEmpty()) {
            try {
                Glide.with(this).load(entry.imageUrl).placeholder(R.drawable.ic_image_placeholder).error(R.drawable.ic_image_placeholder).into(iv);
            } catch (Exception e) {
                iv.setImageResource(R.drawable.ic_image_placeholder);
                android.util.Log.w("MainActivity", "Failed to load dialog image", e);
            }
        } else {
            // No user image — try remote fallback, otherwise use placeholder
            try {
                com.bumptech.glide.request.RequestOptions options = new com.bumptech.glide.request.RequestOptions()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .centerCrop();
                Glide.with(this).load(remoteFallback).apply(options).into(iv);
            } catch (Exception e) {
                iv.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
        builder.setView(iv)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void showActivityDialog(ActivityEntry entry) {
        // open ImageActivity to reduce crash surface
        final String remoteFallback = "https://images.squarespace-cdn.com/content/v1/64a5428b0a8e4f5a25060263/4299df31-6dda-475e-a87b-c9069ffd3277/Olympic+weightlifting+-+Fortress+Gym+4.jpg";

        if (entry.imageUrl != null && !entry.imageUrl.isEmpty()) {
            try {
                Intent intent = new Intent(MainActivity.this, ImageActivity.class);
                intent.putExtra(ImageActivity.EXTRA_URL, entry.imageUrl);
                startActivity(intent);
            } catch (Exception e) {
                // fallback to existing dialog
                showActivityImage(entry);
            }
        } else {
            // if no image URL, prefer the remote fallback
            try {
                Intent intent = new Intent(MainActivity.this, ImageActivity.class);
                intent.putExtra(ImageActivity.EXTRA_URL, remoteFallback);
                startActivity(intent);
            } catch (Exception e) {
                showActivityImage(entry);
            }
        }
    }

    public void launchAddActivity() {
        Intent intent = new Intent(MainActivity.this, AddActivity.class);
        addActivityLauncher.launch(intent);
    }

    private void loadActivities() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String json = prefs.getString(KEY_ACTIVITIES, null);
        activities.clear();
        if (json != null) {
            Type listType = new TypeToken<List<ActivityEntry>>(){}.getType();
            List<ActivityEntry> saved = gson.fromJson(json, listType);
            if (saved != null) {
                activities.addAll(saved);
            }
        }
        // Ensure only 5 items max; trim in-place without changing the list instance
        while (activities.size() > 5) {
            activities.remove(activities.size() - 1);
        }
        // If a HistoryFragment exists, notify it to refresh
        for (androidx.fragment.app.Fragment f : getSupportFragmentManager().getFragments()) {
            if (f instanceof HistoryFragment) {
                ((HistoryFragment) f).loadActivities();
            }
            if (f instanceof StepsFragment) {
                ((StepsFragment) f).loadMiniActivities();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            for (androidx.fragment.app.Fragment f : getSupportFragmentManager().getFragments()) {
                if (f instanceof StepsFragment) {
                    // Let StepsFragment handle registration and updates when permission changes
                }
            }
        }
    }
}
