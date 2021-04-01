package com.aliyun.ai.viapi.activty;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.aliyun.ai.viapi.R;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        pauseAndEnter();
    }

    private void pauseAndEnter() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            Intent intentToIndex = new Intent(SplashActivity.this, MainHomeActivity.class);
            startActivity(intentToIndex);
            finish();
        }, 3000);
    }
}
