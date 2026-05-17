package com.nitai.wanderer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

// BAGRUT NOTE: Firebase imports needed for authentication (verifying the user)
// and Firestore (fetching the database records).
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    // 1. Declare ALL the UI elements
    ImageButton btnProfile, btnSettings;
    View btnStartWalk, btnJournal, btnStats;
    TextView tvWeeklyDistanceMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        // BAGRUT NOTE: This hides the battery/notification bar at the top and the navigation
        // buttons at the bottom to give the app a modern, edge-to-edge, full-screen design.
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_main);

        // 2. Connect Java to the XML IDs
        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        tvWeeklyDistanceMain = findViewById(R.id.tvWeeklyDistanceMain);
        btnStartWalk = findViewById(R.id.btnStartWalk);
        btnJournal = findViewById(R.id.btnJournal);
        btnStats = findViewById(R.id.btnStats);

        // --- SECURITY & DATA LOADING ---
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // BAGRUT NOTE: Security Check (Defensive Programming).
        // If the user somehow bypassed the login screen, or their session expired,
        // we immediately kick them back to LoginActivity to protect the app from crashing.
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return; // Stop the rest of onCreate from running
        }

        String userEmail = currentUser.getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // BAGRUT NOTE: Asynchronous Cloud Fetching.
        // We use `.get().addOnSuccessListener()` so the app's UI doesn't freeze while waiting
        // for Google's servers to respond. It runs in the background and triggers when ready.
        db.collection("users").document(userEmail).collection("walks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 1. Clear the local RAM so we don't accidentally duplicate data if it reloads
                    Walk.walkHistory.clear();

                    // 2. Loop through every single document downloaded from the cloud
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // BAGRUT NOTE: 'toObject' is a powerful Firebase feature. It automatically
                        // maps the raw JSON data from the cloud directly into our custom Java 'Walk' class!
                        Walk downloadedWalk = document.toObject(Walk.class);
                        Walk.walkHistory.add(downloadedWalk);
                    }

                    // 3. Force the screen to update the math totals now that the data has arrived
                    updateWeeklyDistanceUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to sync data", Toast.LENGTH_SHORT).show();
                });

        // --- BUTTON CLICKS ---
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // --- THE GPS HARDWARE CHECK ---
        btnStartWalk.setOnClickListener(v -> {

            // BAGRUT NOTE: Hardware vs. Software check.
            // We use LocationManager to check the physical hardware status of the GPS antenna,
            // not just if the user granted the app "Location Permissions".
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = false;

            if (locationManager != null) {
                isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }

            if (isGpsEnabled) {
                // GPS is on! Safe to start the walk.
                Intent intent = new Intent(MainActivity.this, TravelActivity.class);
                startActivity(intent);
            } else {
                // GPS is off! Block the user from crashing the map and show a helpful dialog.
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("GPS Required")
                        .setMessage("Your phone's GPS is currently turned off. You must enable Location Services to track a walk.")
                        .setPositiveButton("Turn On GPS", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // BAGRUT NOTE: System Intents.
                                // Instead of making the user dig through their phone manually,
                                // this Intent teleports them directly to the native Android Location Settings!
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        btnJournal.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, JournalActivity.class);
            startActivity(intent);
        });

        btnStats.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatsActivity.class);
            startActivity(intent);
        });
    }

    // BAGRUT NOTE: Lifecycle Management.
    // onResume() is a built-in Android hook that runs EVERY TIME this screen becomes visible again.
    // By putting the update logic here, the dashboard immediately refreshes with new stats
    // the moment the user returns from finishing a walk in TravelActivity.
    @Override
    protected void onResume() {
        super.onResume();
        updateWeeklyDistanceUI();
    }

    // BAGRUT NOTE: Helper method to keep our code clean and avoid repeating the same lines
    // of formatting code in both onCreate and onResume (DRY Principle - Don't Repeat Yourself).
    private void updateWeeklyDistanceUI() {
        float weeklyTotal = Walk.calculateWeeklyDistance();
        if (tvWeeklyDistanceMain != null) {
            tvWeeklyDistanceMain.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        }
    }
}