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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String PREFS = "CountLivesPrefs";
    private static final String KEY_ACTIVITIES = "activities";

    private TextView tvSteps;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int liveSteps = 0;

    private List<ActivityEntry> activities = new ArrayList<>();
    private ActivityAdapter adapter;
    private Gson gson = new Gson();

    private ActivityResultLauncher<Intent> addActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvSteps = findViewById(R.id.tvSteps);
        RecyclerView rv = findViewById(R.id.rvActivities);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivityAdapter(activities, this::showActivityImage);
        rv.setAdapter(adapter);

        Button btnAdd = findViewById(R.id.btnAddActivity);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddActivity.class);
            addActivityLauncher.launch(intent);
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            if (stepSensor == null) {
                tvSteps.setText("Sensor not found");
            }
        }

        loadActivities();

        addActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // Refresh list after add activity
            loadActivities();
        });

        // Request ACTIVITY_RECOGNITION permission on Android Q (29) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1001);
            }
        }
    }

    private void showActivityImage(ActivityEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ImageView iv = new ImageView(this);
        iv.setAdjustViewBounds(true);
        if (entry.imageUrl != null && !entry.imageUrl.isEmpty()) {
            Glide.with(this).load(entry.imageUrl).into(iv);
        } else {
            iv.setImageResource(R.drawable.ic_launcher_foreground);
        }
        builder.setView(iv)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
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
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            // each event contains the number of steps (usually 1.0)
            liveSteps += (int) event.values[0];
            tvSteps.setText(String.valueOf(liveSteps));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
                tvSteps.setText("Permission denied");
            }
        }
    }
}
