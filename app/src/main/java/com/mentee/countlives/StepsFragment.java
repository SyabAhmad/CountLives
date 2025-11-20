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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class StepsFragment extends Fragment implements SensorEventListener {
    private TextView tvSteps;
    // Chart UI removed per TODO — step count persists but no chart is displayed
    private RecyclerView rvRecentMini;
    private ActivityAdapter miniAdapter;
    private java.util.List<ActivityEntry> miniActivities = new java.util.ArrayList<>();
    private TextView tvMotivation;
    private TextView tvNoRecentActivities;
    // dayOffset removed (chart not required)
    private android.widget.ImageView ivMotiv1, ivMotiv2, ivMotiv3;
    // track which URL is currently displayed for each motiv image (primary or fallback)
    private String currentMotivUrl1, currentMotivUrl2, currentMotivUrl3;
    private boolean triedFallback1 = false, triedFallback2 = false, triedFallback3 = false;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int liveSteps = 0;
    private Gson gson = new Gson();
    private static final String PREFS = "CountLivesPrefs";
    private static final String KEY_STEPS_MAP = "steps_by_date";
    // No refresh handler or chart button (removed)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_steps, container, false);
        // Chart UI removed - don't bind chart related views
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
        // Only load images if the views are present in the current layout (may be absent in some variants)
        loadMotivationalImages();

        // Chart navigation removed
        // refresh button removed with chart

        loadTodayStepsFromPrefs();
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
            if (stepSensor == null && tvSteps != null) tvSteps.setText("Sensor not found");
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
        // No auto-refresh required; chart removed
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        // No auto-refresh handler to clear
        // save live steps when the fragment pauses
        updateTodayStepsInPrefs(liveSteps);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // clear view references to avoid accessing them after view is destroyed (prevents NPEs on rotation)
        try {
            if (ivMotiv1 != null) com.bumptech.glide.Glide.with(this).clear(ivMotiv1);
            if (ivMotiv2 != null) com.bumptech.glide.Glide.with(this).clear(ivMotiv2);
            if (ivMotiv3 != null) com.bumptech.glide.Glide.with(this).clear(ivMotiv3);
        } catch (Exception e) {
            android.util.Log.w("StepsFragment", "Failed to clear Glide requests", e);
        }
        tvSteps = null;
        tvMotivation = null;
        tvNoRecentActivities = null;
        rvRecentMini = null;
        miniAdapter = null;
        ivMotiv1 = null;
        ivMotiv2 = null;
        ivMotiv3 = null;
        sensorManager = null; // avoid accidental reuse; re-acquire in onResume
        stepSensor = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            liveSteps += (int) event.values[0];
            if (tvSteps != null) tvSteps.setText(String.valueOf(liveSteps));
            // persist today steps when sensors update
            updateTodayStepsInPrefs(liveSteps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("liveSteps", liveSteps);
    }

    // Chart logic removed; display step count only per TODO.md requirements

    public void loadMiniActivities() {
        if (getContext() == null) return; // fragment not attached - skip
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
        while (miniActivities.size() > 5) miniActivities.remove(miniActivities.size() - 1);
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
        if (rvRecentMini != null) {
            rvRecentMini.setVisibility(miniActivities.isEmpty() ? View.GONE : View.VISIBLE);
        }
        updateMotivation();
    }

    // Load the map of date->steps from SharedPreferences
    private Map<String, Integer> loadStepsMap() {
        if (getContext() == null) return new HashMap<>();
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_STEPS_MAP, null);
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        try {
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String, Integer> map = gson.fromJson(json, type);
            if (map == null) return new HashMap<>();
            return map;
        } catch (Exception e) {
            android.util.Log.w("StepsFragment", "loadStepsMap: failed to parse map", e);
            return new HashMap<>();
        }
    }

    private void saveStepsMap(Map<String, Integer> map) {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_STEPS_MAP, gson.toJson(map));
        editor.apply();
    }

    private void updateTodayStepsInPrefs(int steps) {
        if (getContext() == null) return;
        Map<String, Integer> map = loadStepsMap();
        String key = getDateKey(0);
        map.put(key, steps);
        saveStepsMap(map);
        android.util.Log.i("StepsFragment", "updateTodayStepsInPrefs: saved " + steps + " for " + key);
    }

    private void loadTodayStepsFromPrefs() {
        if (getContext() == null) return;
        Map<String, Integer> map = loadStepsMap();
        String key = getDateKey(0);
        int steps = 0;
        if (map != null && map.containsKey(key)) steps = map.get(key);
        liveSteps = steps;
        if (tvSteps != null) tvSteps.setText(String.valueOf(liveSteps));
        android.util.Log.i("StepsFragment", "loadTodayStepsFromPrefs: loaded " + steps + " for " + key);
    }

    private String getDateKey(int dayOffset) {
        Calendar c = Calendar.getInstance();
        if (dayOffset != 0) c.add(Calendar.DAY_OF_YEAR, dayOffset);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(c.getTime());
    }

    private void updateMotivation() {
        int totalMinutes = 0;
        for (ActivityEntry e : miniActivities) totalMinutes += e.durationMinutes;
        if (totalMinutes >= 60) {
            if (tvMotivation != null) tvMotivation.setText("Amazing — you logged " + totalMinutes + " min recently. Keep it up!");
        } else if (totalMinutes > 0) {
            if (tvMotivation != null) tvMotivation.setText("Nice — " + totalMinutes + " min logged, aim for 60 minutes today!");
        } else {
            if (tvMotivation != null) tvMotivation.setText("No recent activities yet — try logging a 10-minute walk.");
        }
    }

    private void loadMotivationalImages() {
        // Motivational fitness images URLs - free stock images
        String[] motivationUrls = {
            "https://images.pexels.com/photos/315938/pexels-photo-315938.jpeg?auto=compress&cs=tinysrgb&w=300",
            "https://images.pexels.com/photos/3621519/pexels-photo-3621519.jpeg?auto=compress&cs=tinysrgb&w=300",
            "https://images.pexels.com/photos/1552252/pexels-photo-1552252.jpeg?auto=compress&cs=tinysrgb&w=300"
        };

        // If primary remote images fail, try this remote fallback URL, otherwise use local drawable fallback.
        final String remoteFallback = "https://images.squarespace-cdn.com/content/v1/64a5428b0a8e4f5a25060263/4299df31-6dda-475e-a87b-c9069ffd3277/Olympic+weightlifting+-+Fortress+Gym+4.jpg";

        // Use a default motivational drawable as fallback when a remote image cannot be fetched
        com.bumptech.glide.request.RequestOptions options = new com.bumptech.glide.request.RequestOptions()
            .placeholder(R.drawable.ic_motiv_default)
            .error(R.drawable.ic_motiv_default)
            .fallback(R.drawable.ic_motiv_default)
            .centerCrop();

        // Load first image and wire click only if view exists
        if (ivMotiv1 != null) {
            try {
                currentMotivUrl1 = motivationUrls[0];
                com.bumptech.glide.Glide.with(this)
                    .load(motivationUrls[0])
                    .apply(options)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.w("StepsFragment", "Motiv 1 failed, trying remote fallback", e);
                            // Try remote fallback once
                            if (!triedFallback1) {
                                triedFallback1 = true;
                                try {
                                    currentMotivUrl1 = remoteFallback;
                                    com.bumptech.glide.Glide.with(StepsFragment.this)
                                        .load(remoteFallback)
                                        .apply(options)
                                        .into(ivMotiv1);
                                } catch (Exception ex) {
                                    android.util.Log.w("StepsFragment", "Remote fallback load failed", ex);
                                }
                            }
                            return false; // allow error drawable to be shown if fallback also fails
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(ivMotiv1);
            } catch (Exception e) {
                android.util.Log.w("StepsFragment", "Failed to load motivational image 1", e);
            }
            ivMotiv1.setOnClickListener(v -> {
                if (getContext() != null) {
                    try {
                        android.content.Intent intent = new android.content.Intent(getContext(), ImageActivity.class);
                        intent.putExtra(ImageActivity.EXTRA_URL, currentMotivUrl1 == null ? motivationUrls[0] : currentMotivUrl1);
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.w("StepsFragment", "Failed to open motivational image", e);
                    }
                }
            });
        }

        // Load second image
        if (ivMotiv2 != null) {
            try {
                currentMotivUrl2 = motivationUrls[1];
                com.bumptech.glide.Glide.with(this)
                    .load(motivationUrls[1])
                    .apply(options)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.w("StepsFragment", "Motiv 2 failed, trying remote fallback", e);
                            if (!triedFallback2) {
                                triedFallback2 = true;
                                try {
                                    currentMotivUrl2 = remoteFallback;
                                    com.bumptech.glide.Glide.with(StepsFragment.this).load(remoteFallback).apply(options).into(ivMotiv2);
                                } catch (Exception ex) {
                                    android.util.Log.w("StepsFragment", "Remote fallback load failed", ex);
                                }
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(ivMotiv2);
            } catch (Exception e) {
                android.util.Log.w("StepsFragment", "Failed to load motivational image 2", e);
            }
        }
            ivMotiv2.setOnClickListener(v -> {
                if (getContext() != null) {
                    try {
                        android.content.Intent intent = new android.content.Intent(getContext(), ImageActivity.class);
                        intent.putExtra(ImageActivity.EXTRA_URL, motivationUrls[1]);
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.w("StepsFragment", "Failed to open motivational image", e);
                    }
                }
            });

        // Load third image
        if (ivMotiv3 != null) {
            try {
                currentMotivUrl3 = motivationUrls[2];
                com.bumptech.glide.Glide.with(this)
                    .load(motivationUrls[2])
                    .apply(options)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.w("StepsFragment", "Motiv 3 failed, trying remote fallback", e);
                            if (!triedFallback3) {
                                triedFallback3 = true;
                                try {
                                    currentMotivUrl3 = remoteFallback;
                                    com.bumptech.glide.Glide.with(StepsFragment.this).load(remoteFallback).apply(options).into(ivMotiv3);
                                } catch (Exception ex) {
                                    android.util.Log.w("StepsFragment", "Remote fallback load failed", ex);
                                }
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(ivMotiv3);
            } catch (Exception e) {
                android.util.Log.w("StepsFragment", "Failed to load motivational image 3", e);
            }
        }
            ivMotiv3.setOnClickListener(v -> {
                if (getContext() != null) {
                    try {
                        android.content.Intent intent = new android.content.Intent(getContext(), ImageActivity.class);
                        intent.putExtra(ImageActivity.EXTRA_URL, currentMotivUrl3 == null ? motivationUrls[2] : currentMotivUrl3);
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.w("StepsFragment", "Failed to open motivational image", e);
                    }
                }
            });
    }
}
