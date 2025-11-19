package com.mentee.countlives;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

// Developer: Report screen removed per client request. This class remains as a no-op stub.
public class ReportActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display a short toast for debugging if this is ever launched by mistake
        Toast.makeText(this, "Report screen removed", Toast.LENGTH_SHORT).show();
        finish();
    }
}
