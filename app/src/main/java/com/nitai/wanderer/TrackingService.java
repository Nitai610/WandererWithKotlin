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

    public static ArrayList<LatLng> livePath = new ArrayList<>();
    public static float liveDistanceInMeters = 0f;
    public static int liveSecondsElapsed = 0;

    // NEW: Static variables to safely store accumulated data
    public static long liveStepsTaken = 0;
    public static long liveCaloriesBurned = 0;
    public static long lastKnownDailySteps = -1;
    public static long lastKnownDailyCalories = -1;

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
        boolean isResuming = (intent != null) && intent.getBooleanExtra("RESUME_WALK", false);

        if (!isServiceRunning) {
            isServiceRunning = true;

            if (!isResuming) {
                livePath.clear();
                liveDistanceInMeters = 0f;
                liveSecondsElapsed = 0;
                liveStepsTaken = 0;      // NEW: Wipe old data on fresh start
                liveCaloriesBurned = 0;  // NEW: Wipe old data on fresh start
            }

            lastKnownLocation = null;
            // NOTE FOR BAGRUT: Setting these to -1 on Resume is a clever trick!
            // It ensures we don't accidentally count steps/calories you burned while the app was paused (e.g., walking around a store).
            lastKnownDailySteps = -1;
            lastKnownDailyCalories = -1;

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
