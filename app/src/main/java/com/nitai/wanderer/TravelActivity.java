package com.nitai.wanderer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;

public class TravelActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    TextView tvLiveDistance, tvLiveTimer, tvLiveSteps, tvLiveCalories;
    MaterialButton btnStopWalk;
    ImageButton btnCancelWalk;

    HealthConnectBridge healthBridge;

    private Handler healthPollingHandler = new Handler(Looper.getMainLooper());
    private Runnable healthPollingRunnable;

    private BroadcastReceiver uiUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateScreenFromService();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        setContentView(R.layout.activity_travel);

        tvLiveDistance = findViewById(R.id.tvLiveDistance);
        tvLiveTimer = findViewById(R.id.tvLiveTimer);
        tvLiveSteps = findViewById(R.id.tvLiveSteps);
        tvLiveCalories = findViewById(R.id.tvLiveCalories);
        btnStopWalk = findViewById(R.id.btnStopWalk);
        btnCancelWalk = findViewById(R.id.btnCancelWalk);

        healthBridge = new HealthConnectBridge(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnStopWalk.setOnClickListener(v -> {
            Intent intent = new Intent(TravelActivity.this, SummaryActivity.class);

            String dist = tvLiveDistance != null ? tvLiveDistance.getText().toString() : "0.00 KM";
            String time = tvLiveTimer != null ? tvLiveTimer.getText().toString() : "00:00:00";

            // NEW: Read the final values directly from our safe static memory
            String steps = String.valueOf(TrackingService.liveStepsTaken);
            String calories = String.valueOf(TrackingService.liveCaloriesBurned);

            intent.putExtra("FINAL_DISTANCE", dist);
            intent.putExtra("FINAL_TIME", time);
            intent.putExtra("FINAL_STEPS", steps);
            intent.putExtra("FINAL_CALORIES", calories);

            stopTrackingService();
            startActivity(intent);
            finish();
        });

        btnCancelWalk.setOnClickListener(v -> {
            stopTrackingService();
            finish();
        });

        healthPollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (TrackingService.isServiceRunning) fetchLiveHealthData();
                healthPollingHandler.postDelayed(this, 15000);
            }
        };
    }

    private void fetchLiveHealthData() {
        // --- STEPS ACCUMULATOR LOGIC ---
        healthBridge.readTodaySteps(new HealthConnectBridge.HealthCallback() {
            @Override
            public void onSuccess(long currentTotalDailySteps) {
                if (TrackingService.lastKnownDailySteps != -1) {
                    // Find out exactly how many steps were taken in the last 15 seconds
                    long newSteps = currentTotalDailySteps - TrackingService.lastKnownDailySteps;
                    if (newSteps > 0) {
                        TrackingService.liveStepsTaken += newSteps; // Add the chunk to our grand total
                    }
                }
                // Update the "last known" checkpoint for the next 15-second loop
                TrackingService.lastKnownDailySteps = currentTotalDailySteps;
                if (tvLiveSteps != null) tvLiveSteps.setText(String.valueOf(TrackingService.liveStepsTaken));
            }
            @Override
            public void onFailure(String errorMessage) { }
        });

        // --- CALORIES ACCUMULATOR LOGIC ---
        healthBridge.readTodayCalories(new HealthConnectBridge.HealthCallback() {
            @Override
            public void onSuccess(long currentTotalDailyCalories) {
                if (TrackingService.lastKnownDailyCalories != -1) {
                    long newCalories = currentTotalDailyCalories - TrackingService.lastKnownDailyCalories;
                    if (newCalories > 0) {
                        TrackingService.liveCaloriesBurned += newCalories;
                    }
                }
                TrackingService.lastKnownDailyCalories = currentTotalDailyCalories;
                if (tvLiveCalories != null) tvLiveCalories.setText(TrackingService.liveCaloriesBurned + " KCAL");
            }
            @Override
            public void onFailure(String errorMessage) { }
        });
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        boolean isResuming = getIntent().getBooleanExtra("RESUME_WALK", false);
        serviceIntent.putExtra("RESUME_WALK", isResuming);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateScreenFromService() {
        if (!TrackingService.isServiceRunning) return;

        float distanceInKm = TrackingService.liveDistanceInMeters / 1000f;
        tvLiveDistance.setText(String.format(java.util.Locale.US, "%.2f KM", distanceInKm));

        int hours = TrackingService.liveSecondsElapsed / 3600;
        int minutes = (TrackingService.liveSecondsElapsed % 3600) / 60;
        int seconds = TrackingService.liveSecondsElapsed % 60;

        if (hours > 0) {
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));
        }

        if (!TrackingService.livePath.isEmpty() && mMap != null) {
            LatLng latestSpot = TrackingService.livePath.get(TrackingService.livePath.size() - 1);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestSpot, 17f));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (!TrackingService.isServiceRunning) {
            checkPermissionsAndStart();
        }
    }

    private void checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.setPadding(0, 0, 0, 500);
            startTrackingService();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void stopTrackingService() {
        stopService(new Intent(this, TrackingService.class));
        TrackingService.isServiceRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(this, uiUpdateReceiver, new IntentFilter("UPDATE_UI_BROADCAST"), ContextCompat.RECEIVER_NOT_EXPORTED);
        updateScreenFromService();
        fetchLiveHealthData();
        healthPollingHandler.post(healthPollingRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(uiUpdateReceiver);
        healthPollingHandler.removeCallbacks(healthPollingRunnable);
    }
}