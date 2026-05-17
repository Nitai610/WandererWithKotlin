package com.nitai.wanderer;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class LeaderboardActivity extends AppCompatActivity {

    RecyclerView rvLeaderboard;
    MaterialButton btnLeaderboardBack;
    LeaderboardAdapter adapter;
    ArrayList<LeaderboardUser> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive Mode
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_leaderboard);

        btnLeaderboardBack = findViewById(R.id.btnLeaderboardBack);
        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));

        // Connect the empty list to the screen for now
        adapter = new LeaderboardAdapter(userList);
        rvLeaderboard.setAdapter(adapter);

        // Fetch Data from the Cloud
        fetchLeaderboardData();

        btnLeaderboardBack.setOnClickListener(v -> finish());
    }

    private void fetchLeaderboardData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collectionGroup("walks").get().addOnSuccessListener(queryDocumentSnapshots -> {

            // We now group walks together by Username!
            HashMap<String, LeaderboardUser> userMap = new HashMap<>();

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Walk walk = document.toObject(Walk.class);

                // Read the username directly from the saved Walk
                String savedUsername = walk.username;

                // Safety check in case it's a very old walk before we added the username feature
                if (savedUsername == null || savedUsername.isEmpty()) {
                    savedUsername = "Unknown Explorer";
                }

                // If this is the first time we've seen this user, add them to our Map
                if (!userMap.containsKey(savedUsername)) {
                    userMap.put(savedUsername, new LeaderboardUser(savedUsername));
                }

                // Add the walk's distance and time to the user's running total
                LeaderboardUser user = userMap.get(savedUsername);
                user.addDistance(walk.distance);
                user.addTime(walk.time);
            }

            userList.clear();
            userList.addAll(userMap.values());
            Collections.sort(userList);
            adapter.notifyDataSetChanged();

        }).addOnFailureListener(e -> {
            Toast.makeText(LeaderboardActivity.this, "Failed to load leaderboard.", Toast.LENGTH_SHORT).show();
        });
    }
}