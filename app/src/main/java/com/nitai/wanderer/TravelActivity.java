package com.nitai.wanderer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

    TextView tvLiveDistance, tvLiveTimer;
    MaterialButton btnStopWalk;
    ImageButton btnCancelWalk;

    // --- THE RADIO RECEIVER (OBSERVER PATTERN) ---
    private BroadcastReceiver uiUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateScreenFromService();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive Mode
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_travel);

        tvLiveDistance = findViewById(R.id.tvLiveDistance);
        tvLiveTimer = findViewById(R.id.tvLiveTimer);
        btnStopWalk = findViewById(R.id.btnStopWalk);
        btnCancelWalk = findViewById(R.id.btnCancelWalk);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnStopWalk.setOnClickListener(v -> {
            // Passing final data to SummaryActivity
            Intent intent = new Intent(TravelActivity.this, SummaryActivity.class);
            intent.putExtra("FINAL_DISTANCE", tvLiveDistance.getText().toString());
            intent.putExtra("FINAL_TIME", tvLiveTimer.getText().toString());
            intent.putParcelableArrayListExtra("PATH_POINTS", TrackingService.livePath);

            stopTrackingService(); // Stop the service while user reviews summary
            startActivity(intent);
            finish();
        });

        btnCancelWalk.setOnClickListener(v -> {
            stopTrackingService();
            finish();
        });
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);

        // BAGRUT LOGIC: We check if TravelActivity itself was opened with a "Resume" flag.
        // We then forward that flag to the Background Service.
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

        // 1. Update Distance
        float distanceInKm = TrackingService.liveDistanceInMeters / 1000f;
        tvLiveDistance.setText(String.format(java.util.Locale.US, "%.2f KM", distanceInKm));

        // 2. Update Timer (Supports HH:MM:SS)
        int hours = TrackingService.liveSecondsElapsed / 3600;
        int minutes = (TrackingService.liveSecondsElapsed % 3600) / 60;
        int seconds = TrackingService.liveSecondsElapsed % 60;

        if (hours > 0) {
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));
        }

        // 3. Update Camera
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(uiUpdateReceiver);
    }
}