package com.nitai.wanderer;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;

public class TrackingService extends Service {

    // BAGRUT NOTE: These are STATIC variables.
    // Static variables live in the App's memory as long as the app process is alive.
    // This allows us to "pause" and "resume" because the numbers don't disappear
    // even when the Service or Activity is temporarily stopped.
    public static ArrayList<LatLng> livePath = new ArrayList<>();
    public static float liveDistanceInMeters = 0f;
    public static int liveSecondsElapsed = 0;
    public static boolean isServiceRunning = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastKnownLocation = null;
    private Handler timerHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // --- THE RESUME FIX ---
        // 1. We extract the "RESUME_WALK" signal sent from TravelActivity.
        boolean isResuming = (intent != null) && intent.getBooleanExtra("RESUME_WALK", false);

        if (!isServiceRunning) {
            isServiceRunning = true;

            // 2. BAGRUT LOGIC: If this is a NEW walk (not resuming), we MUST clear old data.
            // If isResuming is TRUE, we skip this block and the static variables keep their values!
            if (!isResuming) {
                livePath.clear();
                liveDistanceInMeters = 0f;
                liveSecondsElapsed = 0;
            }

            // 3. We always set lastKnownLocation to null when starting/resuming.
            // This prevents a "teleportation" bug where the app calculates the distance
            // between the point where you paused and the point where you resumed.
            lastKnownLocation = null;

            startForegroundWithNotification();
            startLocationUpdates();
            startTimer();
        }
        return START_STICKY;
    }

    private void startTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning) {
                    liveSecondsElapsed++;
                    sendUpdateBroadcast();
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    livePath.add(currentLatLng);

                    if (lastKnownLocation != null) {
                        liveDistanceInMeters += lastKnownLocation.distanceTo(location);
                    }
                    lastKnownLocation = location;
                    sendUpdateBroadcast();
                }
            }
        };
    }

    private void sendUpdateBroadcast() {
        Intent intent = new Intent("UPDATE_UI_BROADCAST");
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).setMinUpdateIntervalMillis(2000).build();
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void startForegroundWithNotification() {
        String channelId = "wanderer_tracking_channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Walk Tracking", android.app.NotificationManager.IMPORTANCE_LOW);
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        android.app.Notification notification = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setContentTitle("Wanderer is Active")
                .setContentText("Currently tracking your walk...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        if (timerHandler != null) timerHandler.removeCallbacksAndMessages(null);
        if (fusedLocationClient != null && locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}