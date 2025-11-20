package com.mentee.countlives;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class ImageActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "extra_url";
    private ImageView iv;
    private ProgressBar pb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        iv = findViewById(R.id.ivFull);
        pb = findViewById(R.id.pbFull);
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(EXTRA_URL)) {
            Toast.makeText(this, "No image specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        final String url = intent.getStringExtra(EXTRA_URL);
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Invalid image URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        pb.setVisibility(View.VISIBLE);
        final String remoteFallback = "https://images.squarespace-cdn.com/content/v1/64a5428b0a8e4f5a25060263/4299df31-6dda-475e-a87b-c9069ffd3277/Olympic+weightlifting+-+Fortress+Gym+4.jpg";

        try {
            Glide.with(this).load(url).placeholder(R.drawable.ic_image_placeholder).error(R.drawable.ic_image_placeholder)
                    .listener(new com.bumptech.glide.request.RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<Drawable> target, boolean isFirstResource) {
                            pb.setVisibility(View.GONE);
                            // Try a remote fallback image if original failed
                            try {
                                Glide.with(ImageActivity.this).load(remoteFallback).placeholder(R.drawable.ic_image_placeholder).error(R.drawable.ic_image_placeholder).into(iv);
                            } catch (Exception ex) {
                                iv.setImageResource(R.drawable.ic_image_placeholder);
                                Toast.makeText(ImageActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, com.bumptech.glide.request.target.Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            pb.setVisibility(View.GONE);
                            return false;
                        }
                    }).into(iv);
        } catch (Exception e) {
            pb.setVisibility(View.GONE);
            iv.setImageResource(R.drawable.ic_image_placeholder);
            Toast.makeText(this, "Error while loading image", Toast.LENGTH_SHORT).show();
        }
    }
}
