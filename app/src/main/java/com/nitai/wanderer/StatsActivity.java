package com.nitai.wanderer; // Keep your package name!

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

public class StatsActivity extends AppCompatActivity {

    // 1. Declare ALL 6 UI elements
    TextView tvStatsWeekly, tvStatsMonthly, tvStatsAllTime;
    TextView tvStatsWeeklyTime, tvStatsMonthlyTime, tvStatsAllTimeTime;
    MaterialButton btnStatsBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- NEW: TURN ON IMMERSIVE MODE ---
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        // Tells Android to let the user swipe from the edge to temporarily see the battery/buttons
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        // Actually hides the top (status) and bottom (navigation) bars!
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // -----------------------------------

        // ... the rest of your code ...
        setContentView(R.layout.activity_stats);

        // 2. Connect to the XML
        tvStatsWeekly = findViewById(R.id.tvStatsWeekly);
        tvStatsMonthly = findViewById(R.id.tvStatsMonthly);
        tvStatsAllTime = findViewById(R.id.tvStatsAllTime);

        tvStatsWeeklyTime = findViewById(R.id.tvStatsWeeklyTime);
        tvStatsMonthlyTime = findViewById(R.id.tvStatsMonthlyTime);
        tvStatsAllTimeTime = findViewById(R.id.tvStatsAllTimeTime);
        btnStatsBack = findViewById(R.id.btnStatsBack);


        btnStatsBack.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                finish(); // Closes the stats screen
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 3. Grab Distance totals
        float weeklyTotal = Walk.calculateWeeklyDistance();
        float monthlyTotal = Walk.calculateMonthlyDistance();
        float allTimeTotal = Walk.calculateAllTimeDistance();

        // 4. Set Distance text
        tvStatsWeekly.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        tvStatsMonthly.setText(String.format(java.util.Locale.US, "%.2f KM", monthlyTotal));
        tvStatsAllTime.setText(String.format(java.util.Locale.US, "%.2f KM", allTimeTotal));

        // 5. Grab Time totals & Set Time text
        tvStatsWeeklyTime.setText(Walk.calculateWeeklyDuration());
        tvStatsMonthlyTime.setText(Walk.calculateMonthlyDuration());
        tvStatsAllTimeTime.setText(Walk.calculateAllTimeDuration());
    }
}