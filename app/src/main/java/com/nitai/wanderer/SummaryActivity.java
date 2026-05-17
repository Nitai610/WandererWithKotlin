package com.nitai.wanderer;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SummaryActivity extends AppCompatActivity {

    TextView tvFinalDistance, tvFinalTime;
    MaterialButton btnSaveWalk, btnDiscardWalk, btnSummaryBack, btnContinueWalk;
    LinearLayout layoutActiveButtons;

    String finalDistance;
    String finalTime;
    String finalSteps;
    String finalCalories;
    ArrayList<LatLng> walkPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        setContentView(R.layout.activity_summary);

        tvFinalDistance = findViewById(R.id.tvFinalDistance);
        tvFinalTime = findViewById(R.id.tvFinalTime);
        btnSaveWalk = findViewById(R.id.btnSaveWalk);
        btnDiscardWalk = findViewById(R.id.btnDiscardWalk);
        btnContinueWalk = findViewById(R.id.btnContinueWalk);
        btnSummaryBack = findViewById(R.id.btnSummaryBack);
        layoutActiveButtons = findViewById(R.id.layoutActiveButtons);

        // NOTE FOR BAGRUT: Why use an index check here?
        // "I am recycling this single Activity for two different purposes to keep the codebase DRY (Don't Repeat Yourself).
        // It acts as a post-walk save screen AND a history viewer from the Journal."
        int oldWalkIndex = getIntent().getIntExtra("OLD_WALK_INDEX", -1);

        if (oldWalkIndex != -1) {
            // SCENARIO 1: Viewing an old walk from the Journal
            Walk oldWalk = Walk.walkHistory.get(oldWalkIndex);
            finalDistance = oldWalk.distance;
            finalTime = oldWalk.time;

            // NOTE FOR BAGRUT: What is backward compatibility?
            // "I added the steps and calories features later in development. If the user clicks on a walk
            // saved in January, those variables will be null. This ternary operator prevents a NullPointerException."
            finalSteps = (oldWalk.steps != null) ? oldWalk.steps : "0";
            finalCalories = (oldWalk.calories != null) ? oldWalk.calories : "0";

            walkPath = oldWalk.getGooglePath();

            layoutActiveButtons.setVisibility(View.GONE);
            btnSummaryBack.setVisibility(View.VISIBLE);
        } else {
            // SCENARIO 2: Just finished a live walk
            finalDistance = getIntent().getStringExtra("FINAL_DISTANCE");
            finalTime = getIntent().getStringExtra("FINAL_TIME");
            finalSteps = getIntent().getStringExtra("FINAL_STEPS");
            finalCalories = getIntent().getStringExtra("FINAL_CALORIES");

            // Copying the GPS path directly from the RAM variable inside TrackingService
            walkPath = new ArrayList<>(TrackingService.livePath);

            layoutActiveButtons.setVisibility(View.VISIBLE);
            btnSummaryBack.setVisibility(View.GONE);
        }

        tvFinalDistance.setText(finalDistance);
        tvFinalTime.setText(finalTime);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.summaryMap);
        if (mapFragment != null) mapFragment.getMapAsync(googleMap -> drawRouteOnMap(googleMap));

        // NOTE: Flawless Resume feature using an Intent flag
        btnContinueWalk.setOnClickListener(v -> {
            Intent intent = new Intent(SummaryActivity.this, TravelActivity.class);
            intent.putExtra("RESUME_WALK", true);
            startActivity(intent);
            finish();
        });

        btnDiscardWalk.setOnClickListener(v -> {
            new AlertDialog.Builder(SummaryActivity.this)
                    .setTitle("Discard Walk?")
                    .setMessage("Are you sure you want to throw away this walk? This action cannot be undone.")
                    .setPositiveButton("Discard", (dialog, which) -> {
                        resetTrackingData();
                        Toast.makeText(SummaryActivity.this, "Walk Discarded", Toast.LENGTH_SHORT).show();

                        // NOTE: FLAG_ACTIVITY_CLEAR_TOP wipes all intermediate activities from the backstack
                        Intent homeIntent = new Intent(SummaryActivity.this, MainActivity.class);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(homeIntent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnSaveWalk.setOnClickListener(v -> saveWalkToFirestore());
        btnSummaryBack.setOnClickListener(v -> finish());
    }

    private void saveWalkToFirestore() {
        btnSaveWalk.setEnabled(false); // Prevents duplicate spam clicks
        btnSaveWalk.setText("SAVING...");

        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String currentUsername = user.getDisplayName();
        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = user.getEmail().split("@")[0]; // Fallback to email prefix
        }

        // Bundle everything into the Walk object using the updated constructor
        Walk completedWalk = new Walk(currentUsername, finalDistance, finalTime, finalSteps, finalCalories, currentDate, walkPath);

        // Upload to Cloud Firestore
        FirebaseFirestore.getInstance().collection("users").document(user.getEmail())
                .collection("walks").add(completedWalk)
                .addOnSuccessListener(ref -> {
                    // Update local RAM so the UI refreshes instantly
                    Walk.walkHistory.add(0, completedWalk);
                    resetTrackingData();
                    finish();
                });
    }

    private void drawRouteOnMap(GoogleMap googleMap) {
        if (walkPath != null && !walkPath.isEmpty()) {
            PolylineOptions line = new PolylineOptions().addAll(walkPath).color(Color.BLUE).width(12f);
            googleMap.addPolyline(line);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(walkPath.get(0), 16f));
        }
    }

    private void resetTrackingData() {
        TrackingService.liveDistanceInMeters = 0f;
        TrackingService.liveSecondsElapsed = 0;
        TrackingService.liveStepsTaken = 0;      // NEW: Clear RAM
        TrackingService.liveCaloriesBurned = 0;  // NEW: Clear RAM
        if (TrackingService.livePath != null) TrackingService.livePath.clear();
    }
}
