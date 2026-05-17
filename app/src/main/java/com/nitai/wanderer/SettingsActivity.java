package com.nitai.wanderer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

// NEW: Health Connect permissions imports
import androidx.health.connect.client.PermissionController;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    MaterialButton btnLogout, btnClearHistory, btnSettingsBack, btnConnectHealth;
    HealthConnectBridge healthBridge;

    // NOTE FOR BAGRUT: What is an ActivityResultLauncher?
    // "In older Android versions, we had to use 'startActivityForResult' and 'onRequestPermissionsResult'
    // which was messy. The new ActivityResultLauncher is much cleaner. We define exactly what we want
    // to ask for (Health Connect Permissions) and define exactly what should happen when the user replies,
    // all in one neat block of code!"
    private final ActivityResultLauncher<Set<String>> requestPermissionActivityContract =
            registerForActivityResult(
                    PermissionController.createRequestPermissionResultContract(),
                    granted -> {
                        if (granted.containsAll(healthBridge.getRequiredPermissions())) {
                            Toast.makeText(this, "Health Connect Linked Successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Permissions Denied. Live stats will be disabled.", Toast.LENGTH_LONG).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_settings);

        btnLogout = findViewById(R.id.btnLogout);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        btnSettingsBack = findViewById(R.id.btnSettingsBack);
        btnConnectHealth = findViewById(R.id.btnConnectHealth); // NEW

        // Initialize the Kotlin Bridge
        healthBridge = new HealthConnectBridge(this);

        // --- CONNECT HEALTH LOGIC ---
        btnConnectHealth.setOnClickListener(v -> {
            // This triggers the Android system popup asking the user to approve Steps and Calories
            requestPermissionActivityContract.launch(healthBridge.getRequiredPermissions());
        });

        // --- GO BACK BUTTON LOGIC ---
        btnSettingsBack.setOnClickListener(v -> finish());

        // --- CLEAR HISTORY BUTTON LOGIC ---
        btnClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Clear Journal")
                    .setMessage("Are you sure you want to permanently delete all your saved walks from the cloud?")
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null || user.getEmail() == null) return;

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        String userEmail = user.getEmail();

                        db.collection("users").document(userEmail).collection("walks").get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    WriteBatch batch = db.batch();
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        batch.delete(document.getReference());
                                    }
                                    batch.commit().addOnSuccessListener(aVoid -> {
                                        Walk.walkHistory.clear();
                                        Toast.makeText(SettingsActivity.this, "Journal cleared", Toast.LENGTH_SHORT).show();
                                    });
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --- LOGOUT BUTTON LOGIC ---
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes, Log Out", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        Walk.walkHistory.clear();
                        Toast.makeText(SettingsActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
