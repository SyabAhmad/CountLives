package com.mentee.countlives;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class StepsFragment extends Fragment implements SensorEventListener {
    private TextView tvSteps;
    private MaterialButton btnPrevDay, btnNextDay;
    private TextView tvChartDay;
    private android.widget.LinearLayout chartContainer;
    private RecyclerView rvRecentMini;
    private ActivityAdapter miniAdapter;
    private java.util.List<ActivityEntry> miniActivities = new java.util.ArrayList<>();
    private TextView tvMotivation;
    private TextView tvNoRecentActivities;
    private int dayOffset = 0; // 0 == today, -1 yesterday, etc.
    private android.widget.ImageView ivMotiv1, ivMotiv2, ivMotiv3;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int liveSteps = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_steps, container, false);
        btnPrevDay = v.findViewById(R.id.btnPrevDay);
        btnNextDay = v.findViewById(R.id.btnNextDay);
        tvChartDay = v.findViewById(R.id.tvChartDay);
        chartContainer = v.findViewById(R.id.chartContainer);
        tvMotivation = v.findViewById(R.id.tvMotivation);
        rvRecentMini = v.findViewById(R.id.rvRecentMini);
        rvRecentMini.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        rvRecentMini.setNestedScrollingEnabled(false);
        miniAdapter = new ActivityAdapter(miniActivities, entry -> {
            // show image
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showActivityDialog(entry);
            }
        });
        rvRecentMini.setAdapter(miniAdapter);
        tvNoRecentActivities = v.findViewById(R.id.tvNoRecentActivities);
        tvSteps = v.findViewById(R.id.tvStepsFrag);
        if (savedInstanceState != null) {
            liveSteps = savedInstanceState.getInt("liveSteps", 0);
            tvSteps.setText(String.valueOf(liveSteps));
        }

        ivMotiv1 = v.findViewById(R.id.ivMotiv1);
        ivMotiv2 = v.findViewById(R.id.ivMotiv2);
        ivMotiv3 = v.findViewById(R.id.ivMotiv3);
        loadMotivationalImages();

        btnPrevDay.setOnClickListener(view -> {
            dayOffset -= 1;
            updateChart();
        });
        btnNextDay.setOnClickListener(view -> {
            dayOffset += 1;
            updateChart();
        });
        updateChart();
        loadMiniActivities();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMiniActivities();
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            if (stepSensor == null) tvSteps.setText("Sensor not found");
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                } else {
                    sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            liveSteps += (int) event.values[0];
            tvSteps.setText(String.valueOf(liveSteps));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("liveSteps", liveSteps);
    }

    private void updateChart() {
        // For demo: generate 24 hourly values for dayOffset
        // Today (dayOffset == 0) shows empty chart, past days show data
        chartContainer.removeAllViews();
        int[] values = new int[24];
        int max = 1;
        
        // Only generate data for past days, not today
        if (dayOffset < 0) {
            for (int i = 0; i < 24; i++) {
                values[i] = (int) ((Math.random() * 500) + 200 * Math.abs(dayOffset));
                if (values[i] > max) max = values[i];
            }
        } else {
            // Today - all zeros, no bars shown
            for (int i = 0; i < 24; i++) {
                values[i] = 0;
            }
            max = 100; // prevents division by zero
        }
        
        // build bars - only show if value > 0
        final int barWidthDp = 20;
        for (int i = 0; i < 24; i++) {
            if (values[i] > 0) {
                View bar = new View(getContext());
                int height = (int) ((values[i] / (float) max) * 200); // scale to 200px
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams((int) (barWidthDp * getResources().getDisplayMetrics().density), height);
                lp.setMargins(6, 0, 6, 0);
                bar.setLayoutParams(lp);
                bar.setBackgroundColor(getResources().getColor(R.color.primary_blue_500));
                // add onClick to show a tooltip or toast with hourly value
                final int hourVal = values[i];
                final int hourIndex = i;
                bar.setOnClickListener(v -> android.widget.Toast.makeText(getContext(), "Hour " + hourIndex + ": " + hourVal + " steps", android.widget.Toast.LENGTH_SHORT).show());
                chartContainer.addView(bar);
            }
        }
        
        // If no data, show empty message
        if (dayOffset == 0) {
            android.widget.TextView tvEmptyChart = new android.widget.TextView(getContext());
            tvEmptyChart.setText("No data yet - chart will update as you log activities");
            tvEmptyChart.setTextSize(14);
            tvEmptyChart.setTextColor(getResources().getColor(R.color.primary_blue_700));
            tvEmptyChart.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT);
            tvEmptyChart.setLayoutParams(lp);
            chartContainer.addView(tvEmptyChart);
        }
        
        tvChartDay.setText(dayOffset == 0 ? "Today" : (dayOffset < 0 ? Math.abs(dayOffset) + "d ago" : "+" + dayOffset + "d"));
    }

    public void loadMiniActivities() {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("CountLivesPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("activities", null);
        android.util.Log.i("StepsFragment", "loadMiniActivities: json=" + json);
        miniActivities.clear();
        if (json != null) {
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<ActivityEntry>>(){}.getType();
            java.util.List<ActivityEntry> saved = new com.google.gson.Gson().fromJson(json, listType);
            android.util.Log.i("StepsFragment", "loadMiniActivities: parsed saved size=" + (saved == null ? 0 : saved.size()));
            if (saved != null) miniActivities.addAll(saved);
        }
        while (miniActivities.size() > 3) miniActivities.remove(miniActivities.size() - 1);
        android.util.Log.i("StepsFragment", "loadMiniActivities: miniActivities size after trim=" + miniActivities.size());
        if (miniAdapter != null) {
            miniAdapter.notifyDataSetChanged();
        } else {
            android.util.Log.w("StepsFragment", "miniAdapter is null when trying to notify; skipping refresh.");
        }
        // Show/hide the empty placeholder
        if (tvNoRecentActivities != null) {
            tvNoRecentActivities.setVisibility(miniActivities.isEmpty() ? View.VISIBLE : View.GONE);
        }
        rvRecentMini.setVisibility(miniActivities.isEmpty() ? View.GONE : View.VISIBLE);
        updateMotivation();
    }

    private void updateMotivation() {
        int totalMinutes = 0;
        for (ActivityEntry e : miniActivities) totalMinutes += e.durationMinutes;
        if (totalMinutes >= 60) {
            tvMotivation.setText("Amazing — you logged " + totalMinutes + " min recently. Keep it up!");
        } else if (totalMinutes > 0) {
            tvMotivation.setText("Nice — " + totalMinutes + " min logged, aim for 60 minutes today!");
        } else {
            tvMotivation.setText("No recent activities yet — try logging a 10-minute walk.");
        }
    }

    private void loadMotivationalImages() {
        // Motivational fitness images URLs - free stock images
        String[] motivationUrls = {
            "https://images.pexels.com/photos/315938/pexels-photo-315938.jpeg?auto=compress&cs=tinysrgb&w=300",
            "https://images.pexels.com/photos/3621519/pexels-photo-3621519.jpeg?auto=compress&cs=tinysrgb&w=300",
            "https://images.pexels.com/photos/1552252/pexels-photo-1552252.jpeg?auto=compress&cs=tinysrgb&w=300"
        };

        com.bumptech.glide.request.RequestOptions options = new com.bumptech.glide.request.RequestOptions()
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop();

        // Load first image
        com.bumptech.glide.Glide.with(this)
                .load(motivationUrls[0])
                .apply(options)
                .into(ivMotiv1);

        // Load second image
        com.bumptech.glide.Glide.with(this)
                .load(motivationUrls[1])
                .apply(options)
                .into(ivMotiv2);

        // Load third image
        com.bumptech.glide.Glide.with(this)
                .load(motivationUrls[2])
                .apply(options)
                .into(ivMotiv3);
    }
}
