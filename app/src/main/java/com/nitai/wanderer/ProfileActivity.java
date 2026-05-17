package com.nitai.wanderer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// FIREBASE IMPORTS
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class ProfileActivity extends AppCompatActivity {

    // UI Variables
    TextView tvProfileUsername, tvProfileEmail;
    TextView tvProfileTotalTime, tvProfileTotalDistance;
    MaterialButton btnProfileBack, btnEditUsername, btnOpenLeaderboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        // Hides the status bar and navigation bar for a clean, modern look
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_profile);

        // --- CONNECT UI TO XML ---
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileTotalTime = findViewById(R.id.tvProfileTotalTime);
        tvProfileTotalDistance = findViewById(R.id.tvProfileTotalDistance);
        btnProfileBack = findViewById(R.id.btnProfileBack);
        btnEditUsername = findViewById(R.id.btnEditUsername);
        btnOpenLeaderboard = findViewById(R.id.btnOpenLeaderboard);

        // --- INITIALIZE DATA ---
        // Load the user's email and username from Firebase
        loadProfileData();

        // --- BUTTON LOGIC: OPEN LEADERBOARD ---
        btnOpenLeaderboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, LeaderboardActivity.class);
                startActivity(intent);
            }
        });

        // --- BUTTON LOGIC: EDIT USERNAME ---
        btnEditUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditUsernameDialog();
            }
        });

        // --- BUTTON LOGIC: GO BACK ---
        btnProfileBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Safely closes the activity and returns to the previous screen
            }
        });
    }

    // --- HELPER METHOD: LOAD USER DATA ---
    // Grabs the DisplayName and Email from Firebase Authentication
    private void loadProfileData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            String fullEmail = currentUser.getEmail();

            // Try to get their custom saved username first
            String username = currentUser.getDisplayName();

            // BAGRUT TRICK: If they haven't set a username yet, generate one from their email!
            // We split "nitai@gmail.com" at the "@" symbol and take the first part ("nitai").
            if (username == null || username.isEmpty()) {
                username = fullEmail.split("@")[0];
                // Capitalize the first letter for a polished look
                username = username.substring(0, 1).toUpperCase() + username.substring(1);
            }

            tvProfileUsername.setText(username);
            tvProfileEmail.setText(fullEmail);
        }
    }

    // --- LIVE STATS UPDATE (LIFECYCLE HOOK) ---
    // onResume() is triggered every time the user returns to this screen.
    // This ensures the mini-stats always match the main StatsActivity.
    @Override
    protected void onResume() {
        super.onResume();

        // Calculate totals using our existing math engine in Walk.java
        float allTimeDistance = Walk.calculateAllTimeDistance();
        String allTimeDuration = Walk.calculateAllTimeDuration();

        // Put the calculated data onto the screen
        if (tvProfileTotalDistance != null && tvProfileTotalTime != null) {
            tvProfileTotalDistance.setText(String.format(java.util.Locale.US, "%.2f KM", allTimeDistance));
            tvProfileTotalTime.setText(allTimeDuration);
        }
    }

    // --- SECURE DIALOG & FIREBASE UPDATE ---
    // Requires re-authentication before making sensitive changes.
    private void showEditUsernameDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // 1. Build a custom layout for the Popup Dialog dynamically in Java
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextInputEditText etNewUsername = new TextInputEditText(this);
        etNewUsername.setHint("Enter New Username");
        layout.addView(etNewUsername);

        TextInputEditText etPassword = new TextInputEditText(this);
        etPassword.setHint("Enter Current Password");
        // Security: Mask the password input with dots
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPassword);

        // 2. Create and show the AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("Change Username")
                .setMessage("For your security, please verify your password to change your username.")
                .setView(layout)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newUsername = etNewUsername.getText().toString().trim();
                        String password = etPassword.getText().toString().trim();

                        // Validation: Check for empty fields
                        if (newUsername.isEmpty() || password.isEmpty()) {
                            Toast.makeText(ProfileActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 3. SECURITY RE-AUTHENTICATION
                        // Create a secure token using their email and the password they just typed
                        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

                        // Send the token to Google's servers to verify
                        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // 4. Password verified! Send the new DisplayName to Firebase.
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(newUsername)
                                        .build();

                                currentUser.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ProfileActivity.this, "Username updated!", Toast.LENGTH_SHORT).show();
                                        loadProfileData(); // Refresh the screen so the new name appears instantly

                                        // --- NEW: UPDATE LEADERBOARD DATA (FIRESTORE BATCH UPDATE) ---
                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        String userEmail = currentUser.getEmail();

                                        // Find all past walks belonging to this user
                                        db.collection("users").document(userEmail).collection("walks")
                                                .get()
                                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                                    // A WriteBatch lets us update multiple cloud documents at the exact same time
                                                    WriteBatch batch = db.batch();

                                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                                        // Queue up an update for the "username" field on every old walk
                                                        batch.update(document.getReference(), "username", newUsername);
                                                    }

                                                    // Execute the massive update
                                                    batch.commit().addOnSuccessListener(aVoid -> {
                                                        // Update the local RAM memory too so it matches instantly
                                                        for (Walk walk : Walk.walkHistory) {
                                                            walk.username = newUsername;
                                                        }
                                                    });
                                                });
                                        // -------------------------------------------------------------
                                    }
                                });
                            } else {
                                // Password was incorrect
                                Toast.makeText(ProfileActivity.this, "Incorrect password. Try again.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}