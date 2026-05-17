
package com.nitai.wanderer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
// NEW FIRESTORE IMPORTS
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class StatsActivity extends AppCompatActivity {

    TextView tvStatsWeekly, tvStatsMonthly, tvStatsAllTime;
    TextView tvStatsWeeklyTime, tvStatsMonthlyTime, tvStatsAllTimeTime;
    TextView tvStatsWeeklySteps, tvStatsMonthlySteps, tvStatsAllTimeSteps;
    MaterialButton btnStatsBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_stats);

        tvStatsWeekly = findViewById(R.id.tvStatsWeekly);
        tvStatsMonthly = findViewById(R.id.tvStatsMonthly);
        tvStatsAllTime = findViewById(R.id.tvStatsAllTime);

        tvStatsWeeklyTime = findViewById(R.id.tvStatsWeeklyTime);
        tvStatsMonthlyTime = findViewById(R.id.tvStatsMonthlyTime);
        tvStatsAllTimeTime = findViewById(R.id.tvStatsAllTimeTime);

        tvStatsWeeklySteps = findViewById(R.id.tvStatsWeeklySteps);
        tvStatsMonthlySteps = findViewById(R.id.tvStatsMonthlySteps);
        tvStatsAllTimeSteps = findViewById(R.id.tvStatsAllTimeSteps);

        btnStatsBack = findViewById(R.id.btnStatsBack);

        btnStatsBack.setOnClickListener(v -> finish());

        // NEW: Load the data from the cloud as soon as the screen is created
        fetchStatsFromCloud();
    }

    // --- NEW HELPER METHOD: DOWNLOAD CLOUD DATA ---
    private void fetchStatsFromCloud() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(user.getEmail()).collection("walks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Clear local memory before rebuilding from the cloud
                    Walk.walkHistory.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Walk downloadedWalk = document.toObject(Walk.class);
                        Walk.walkHistory.add(downloadedWalk);
                    }

                    // Once the data is downloaded, update the UI labels
                    updateStatsUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StatsActivity.this, "Failed to load stats", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fallback: If data is already in RAM, show it immediately while the cloud fetch runs
        updateStatsUI();
    }

    // Moved the UI logic into a separate method so it can be called after the Firestore success
    private void updateStatsUI() {
        // Distance Totals
        float weeklyTotal = Walk.calculateWeeklyDistance();
        float monthlyTotal = Walk.calculateMonthlyDistance();
        float allTimeTotal = Walk.calculateAllTimeDistance();

        tvStatsWeekly.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        tvStatsMonthly.setText(String.format(java.util.Locale.US, "%.2f KM", monthlyTotal));
        tvStatsAllTime.setText(String.format(java.util.Locale.US, "%.2f KM", allTimeTotal));

        // Time Totals
        tvStatsWeeklyTime.setText(Walk.calculateWeeklyDuration());
        tvStatsMonthlyTime.setText(Walk.calculateMonthlyDuration());
        tvStatsAllTimeTime.setText(Walk.calculateAllTimeDuration());

        // Step Totals
        tvStatsWeeklySteps.setText(String.valueOf(Walk.calculateWeeklySteps()));
        tvStatsMonthlySteps.setText(String.valueOf(Walk.calculateMonthlySteps()));
        tvStatsAllTimeSteps.setText(String.valueOf(Walk.calculateAllTimeSteps()));
    }
}