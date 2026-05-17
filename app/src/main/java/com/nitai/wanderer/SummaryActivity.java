package com.nitai.wanderer;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
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

    // --- UI ELEMENTS ---
    TextView tvFinalDistance, tvFinalTime;
    MaterialButton btnSaveWalk, btnDiscardWalk, btnSummaryBack, btnContinueWalk;
    LinearLayout layoutActiveButtons;

    // --- DATA VARIABLES ---
    String finalDistance;
    String finalTime;
    ArrayList<LatLng> walkPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NOTE: Immersive Mode hides system bars to focus on the map
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_summary);

        // --- CONNECTING JAVA TO XML ---
        tvFinalDistance = findViewById(R.id.tvFinalDistance);
        tvFinalTime = findViewById(R.id.tvFinalTime);
        btnSaveWalk = findViewById(R.id.btnSaveWalk);
        btnDiscardWalk = findViewById(R.id.btnDiscardWalk);
        btnContinueWalk = findViewById(R.id.btnContinueWalk);
        btnSummaryBack = findViewById(R.id.btnSummaryBack);
        layoutActiveButtons = findViewById(R.id.layoutActiveButtons);

        // --- INTENT HANDLING (BAGRUT TRICK) ---
        // We use one screen for two purposes: viewing old history or finishing a new walk.
        int oldWalkIndex = getIntent().getIntExtra("OLD_WALK_INDEX", -1);

        if (oldWalkIndex != -1) {
            // SCENARIO 1: Viewing an old walk from JournalActivity
            Walk oldWalk = Walk.walkHistory.get(oldWalkIndex);
            finalDistance = oldWalk.distance;
            finalTime = oldWalk.time;
            walkPath = oldWalk.getGooglePath();

            layoutActiveButtons.setVisibility(View.GONE); // Hide Save/Discard/Continue
            btnSummaryBack.setVisibility(View.VISIBLE); // Show Back button
        } else {
            // SCENARIO 2: Just finished tracking a live walk
            finalDistance = getIntent().getStringExtra("FINAL_DISTANCE");
            finalTime = getIntent().getStringExtra("FINAL_TIME");
            walkPath = getIntent().getParcelableArrayListExtra("PATH_POINTS");

            layoutActiveButtons.setVisibility(View.VISIBLE);
            btnSummaryBack.setVisibility(View.GONE);
        }

        tvFinalDistance.setText(finalDistance);
        tvFinalTime.setText(finalTime);

        // --- GOOGLE MAPS SETUP ---
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.summaryMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> drawRouteOnMap(googleMap));
        }

        // --- BUTTON: RESUME WALKING ---
        btnContinueWalk.setOnClickListener(v -> {
            // NOTE: Flawless Resume. We pass a flag to tell TravelActivity NOT to reset the timer
            Intent intent = new Intent(SummaryActivity.this, TravelActivity.class);
            intent.putExtra("RESUME_WALK", true);
            startActivity(intent);
            finish();
        });

        // --- BUTTON: DISCARD WALK (DIALOG REQUIREMENT) ---
        btnDiscardWalk.setOnClickListener(v -> {
            // BAGRUT REQUIREMENT: Using an AlertDialog to prevent accidental data loss
            new AlertDialog.Builder(SummaryActivity.this)
                    .setTitle("Discard Walk?")
                    .setMessage("Are you sure you want to throw away this walk? This action cannot be undone.")
                    .setPositiveButton("Discard", (dialog, which) -> {
                        resetTrackingData(); // Wipe static variables
                        Toast.makeText(SummaryActivity.this, "Walk Discarded", Toast.LENGTH_SHORT).show();

                        // CLEAR_TOP ensures we don't build a stack of open activities
                        Intent homeIntent = new Intent(SummaryActivity.this, MainActivity.class);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(homeIntent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --- BUTTON: SAVE TO CLOUD ---
        btnSaveWalk.setOnClickListener(v -> saveWalkToFirestore());

        btnSummaryBack.setOnClickListener(v -> finish());
    }

    private void saveWalkToFirestore() {
        btnSaveWalk.setEnabled(false); // Prevent multiple clicks/saves
        btnSaveWalk.setText("SAVING...");

        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // BAGRUT TRICK: If display name is missing, use email prefix
        String currentUsername = user.getDisplayName();
        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = user.getEmail().split("@")[0];
        }

        Walk completedWalk = new Walk(currentUsername, finalDistance, finalTime, currentDate, walkPath);

        FirebaseFirestore.getInstance().collection("users").document(user.getEmail())
                .collection("walks").add(completedWalk)
                .addOnSuccessListener(ref -> {
                    Walk.walkHistory.add(0, completedWalk); // Add to top of local list
                    resetTrackingData(); // Clear live counters
                    finish();
                });
    }

    private void drawRouteOnMap(GoogleMap googleMap) {
        if (walkPath != null && !walkPath.isEmpty()) {
            // Draw a line connecting all GPS points
            PolylineOptions line = new PolylineOptions().addAll(walkPath).color(Color.BLUE).width(12f);
            googleMap.addPolyline(line);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(walkPath.get(0), 16f));
        }
    }

    private void resetTrackingData() {
        // We must clear static variables so the next walk starts at 0
        TrackingService.liveDistanceInMeters = 0f;
        TrackingService.liveSecondsElapsed = 0;
        if (TrackingService.livePath != null) TrackingService.livePath.clear();
    }
}