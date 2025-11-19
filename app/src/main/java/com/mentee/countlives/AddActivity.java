package com.mentee.countlives;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import android.util.Patterns;

public class AddActivity extends AppCompatActivity {
    private Spinner spinner;
    private SeekBar seekBar;
    private TextView tvDuration;
    private EditText etImageUrl;
    private ImageView ivPreview;
    private Gson gson = new Gson();
    private android.widget.ProgressBar progressPreview;

    private static final String PREFS = "CountLivesPrefs";
    private static final String KEY_ACTIVITIES = "activities";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        spinner = findViewById(R.id.spinnerActivityType);
        seekBar = findViewById(R.id.seekbarDuration);
        tvDuration = findViewById(R.id.tvDurationLabel);
        etImageUrl = findViewById(R.id.etImageUrl);
        ivPreview = findViewById(R.id.ivPreview);
        progressPreview = findViewById(R.id.progressPreview);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.activity_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        seekBar.setMax(180);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvDuration.setText(progress + " min");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Button btnLoad = findViewById(R.id.btnLoadImage);
        btnLoad.setOnClickListener(v -> {
            String url = etImageUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                if (isValidUrl(url)) {
                    progressPreview.setVisibility(View.VISIBLE);
                    com.bumptech.glide.request.RequestOptions options = new com.bumptech.glide.request.RequestOptions()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                        .centerCrop();
                    try {
                        com.bumptech.glide.Glide.with(this).load(url).apply(options).listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            progressPreview.setVisibility(View.GONE);
                            // Show placeholder on failure - don't show error toast
                            android.util.Log.w("AddActivity", "Image load failed, using placeholder for: " + url);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            progressPreview.setVisibility(View.GONE);
                            Toast.makeText(AddActivity.this, "Image loaded successfully", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        }).into(ivPreview);
                    } catch (Exception e) {
                        progressPreview.setVisibility(View.GONE);
                        ivPreview.setImageResource(R.drawable.ic_image_placeholder);
                        android.util.Log.w("AddActivity", "Glide failed to load image: " + url, e);
                    }
                } else {
                    Toast.makeText(this, "Invalid URL, must be https://", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Enter image URL", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnSave = findViewById(R.id.btnSaveActivity);
        btnSave.setOnClickListener(v -> saveActivity());

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        if (!url.startsWith("https://")) return false; // enforce HTTPS for security
        return Patterns.WEB_URL.matcher(url).matches();
    }

    private boolean isNetworkAvailable() {
        // Check permission before accessing network state
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, assume network is available or handle accordingly
            android.util.Log.w("AddActivity", "ACCESS_NETWORK_STATE permission not granted");
            return true; // Assume available to avoid blocking, or request permission
        }
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.net.Network nw = cm.getActiveNetwork();
                if (nw == null) return false;
                android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(nw);
                return caps != null && (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception e) {
            android.util.Log.w("AddActivity", "isNetworkAvailable check failed", e);
            return false;
        }
    }

    private void saveActivity() {
        String type = (String) spinner.getSelectedItem();
        int duration = seekBar.getProgress();
        String url = etImageUrl.getText().toString().trim();
        if (!url.isEmpty() && !isValidUrl(url)) {
            Toast.makeText(this, "Invalid URL, please use http(s)", Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityEntry entry = new ActivityEntry(type, duration, url, System.currentTimeMillis());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String json = prefs.getString(KEY_ACTIVITIES, null);
        List<ActivityEntry> list = new ArrayList<>();
        if (json != null) {
            Type listType = new TypeToken<List<ActivityEntry>>(){}.getType();
            List<ActivityEntry> saved = gson.fromJson(json, listType);
            if (saved != null) {
                list.addAll(saved);
            }
        }
        // add to front
        list.add(0, entry);
        // trim to 5
        if (list.size() > 5) list = list.subList(0, 5);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACTIVITIES, gson.toJson(list));
        editor.apply();
        Toast.makeText(this, "Activity saved", Toast.LENGTH_SHORT).show();
        // return to MainActivity
        setResult(RESULT_OK, new Intent());
        finish();
    }
}
