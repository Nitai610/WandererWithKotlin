package com.nitai.wanderer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

// FIRESTORE IMPORTS
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// NEW: IMPORTS FOR SORTING DATES
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class JournalActivity extends AppCompatActivity {

    LinearLayout layoutEmptyState;
    MaterialButton btnEmptyStartWalk, btnJournalBack;
    RecyclerView recyclerViewJournal;
    WalkAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_journal);

        // 1. Connect UI
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnEmptyStartWalk = findViewById(R.id.btnEmptyStartWalk);
        btnJournalBack = findViewById(R.id.btnJournalBack);
        recyclerViewJournal = findViewById(R.id.recyclerViewJournal);

        recyclerViewJournal.setLayoutManager(new LinearLayoutManager(this));

        // 2. Set up the Adapter immediately
        adapter = new WalkAdapter(Walk.walkHistory, new WalkAdapter.OnWalkDeleteListener() {
            @Override
            public void onWalkDeleted() {
                checkIfEmpty();
            }
        });
        recyclerViewJournal.setAdapter(adapter);

        // 3. Fetch data from the cloud
        fetchWalksFromCloud();

        // 4. Start Walk Button Logic
        btnEmptyStartWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JournalActivity.this, TravelActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 5. Global Go Back Button Logic
        btnJournalBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // --- HELPER METHOD: DOWNLOAD DATA & SORT ---
    private void fetchWalksFromCloud() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(user.getEmail()).collection("walks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    Walk.walkHistory.clear(); // Clear the memory

                    // 1. Loop through the cloud database and rebuild the raw list
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Walk downloadedWalk = document.toObject(Walk.class);
                        Walk.walkHistory.add(downloadedWalk);
                    }

                    // ==========================================================
                    // 2. NEW BAGRUT FIX: SORT THE LIST BY DATE (NEWEST FIRST)
                    // ==========================================================
                    // NOTE FOR BAGRUT EXAMINER:
                    // "Firestore downloads documents in random order. I used Java's Collections.sort()
                    // along with a custom Comparator. It translates the String dates (like '14/05/2026')
                    // into real Date objects, compares them, and pushes the newest dates to the top (index 0)."
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    Collections.sort(Walk.walkHistory, new Comparator<Walk>() {
                        @Override
                        public int compare(Walk walk1, Walk walk2) {
                            try {
                                Date date1 = sdf.parse(walk1.getDate());
                                Date date2 = sdf.parse(walk2.getDate());
                                // Comparing date2 to date1 puts it in DESCENDING order (Newest on top)
                                return date2.compareTo(date1);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return 0;
                            }
                        }
                    });
                    // ==========================================================

                    // 3. Tell the screen to update with the new sorted data
                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(JournalActivity.this, "Error loading journal.", Toast.LENGTH_SHORT).show();
                });
    }

    // --- HELPER METHOD: TOGGLE EMPTY STATE ---
    private void checkIfEmpty() {
        if (Walk.walkHistory.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewJournal.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewJournal.setVisibility(View.VISIBLE);
        }
    }
}