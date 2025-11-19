package com.mentee.countlives;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AddActivity extends AppCompatActivity {
    private Spinner spinner;
    private SeekBar seekBar;
    private TextView tvDuration;
    private EditText etImageUrl;
    private ImageView ivPreview;
    private Gson gson = new Gson();

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
                Glide.with(this).load(url).into(ivPreview);
            } else {
                Toast.makeText(this, "Enter image URL", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnSave = findViewById(R.id.btnSaveActivity);
        btnSave.setOnClickListener(v -> saveActivity());

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void saveActivity() {
        String type = (String) spinner.getSelectedItem();
        int duration = seekBar.getProgress();
        String url = etImageUrl.getText().toString().trim();
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
        // return to MainActivity
        setResult(RESULT_OK, new Intent());
        finish();
    }
}
