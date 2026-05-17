package com.nitai.wanderer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

// NOTE: We must import Firebase tools so the Adapter can talk to the cloud database
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class WalkAdapter extends RecyclerView.Adapter<WalkAdapter.WalkViewHolder> {

    private ArrayList<Walk> walks;
    // NOTE: This listener is like a walkie-talkie back to JournalActivity.
    // It tells JournalActivity "Hey, I deleted something, check if the list is empty now!"
    private OnWalkDeleteListener deleteListener;

    public interface OnWalkDeleteListener {
        void onWalkDeleted();
    }

    public WalkAdapter(ArrayList<Walk> walks, OnWalkDeleteListener deleteListener) {
        this.walks = walks;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public WalkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_walk, parent, false);
        return new WalkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WalkViewHolder holder, int position) {
        Walk currentWalk = walks.get(position);

        // Put the text data onto the screen
        holder.tvWalkDate.setText(currentWalk.getDate());
        String combinedStats = currentWalk.getDistance() + "     " + currentWalk.getTime();
        holder.tvWalkStats.setText(combinedStats);

        // --- CLICK CARD TO OPEN MAP ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SummaryActivity.class);
            intent.putExtra("OLD_WALK_INDEX", holder.getAdapterPosition());
            v.getContext().startActivity(intent);
        });

        // --- DELETE BUTTON LOGIC WITH DIALOG ---
        holder.btnDeleteWalk.setOnClickListener(v -> {
            // We need 'Context' to show popups on the screen
            Context context = v.getContext();

            // BAGRUT REQUIREMENT: Using an AlertDialog to prevent accidental data loss.
            new AlertDialog.Builder(context)
                    .setTitle("Delete Walk")
                    .setMessage("Are you sure you want to permanently delete this walk from your journal?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Only if they click "Delete" do we run the cloud deletion process
                            deleteWalkPermanently(holder.getAdapterPosition(), context);
                        }
                    })
                    .setNegativeButton("Cancel", null) // Does nothing, just closes the box
                    .show();
        });
    }

    // --- HELPER METHOD: DELETE FROM CLOUD AND MEMORY ---
    private void deleteWalkPermanently(int position, Context context) {
        // Safety check to ensure the position is still valid
        if (position == RecyclerView.NO_POSITION) return;

        Walk walkToDelete = walks.get(position);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // 1. FIND THE WALK IN THE CLOUD
        // We ask Firebase to find the exact walk that matches this date, time, and distance.
        db.collection("users").document(userEmail).collection("walks")
                .whereEqualTo("date", walkToDelete.getDate())
                .whereEqualTo("time", walkToDelete.getTime())
                .whereEqualTo("distance", walkToDelete.getDistance())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // 2. DELETE IT FROM THE CLOUD SERVER
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }

                    // 3. REMOVE IT FROM THE PHONE'S MEMORY (RAM)
                    // We must update the local ArrayList so the item visually disappears.
                    walks.remove(position);

                    // Play the shrinking animation and update the list positions
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, walks.size());

                    // 4. TELL JOURNAL ACTIVITY TO REFRESH
                    // This triggers checkIfEmpty() in JournalActivity
                    if (deleteListener != null) {
                        deleteListener.onWalkDeleted();
                    }

                    Toast.makeText(context, "Walk deleted from cloud", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return walks.size();
    }

    public static class WalkViewHolder extends RecyclerView.ViewHolder {
        TextView tvWalkDate, tvWalkStats;
        ImageButton btnDeleteWalk;

        public WalkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWalkDate = itemView.findViewById(R.id.tvWalkDate);
            tvWalkStats = itemView.findViewById(R.id.tvWalkStats);
            btnDeleteWalk = itemView.findViewById(R.id.btnDeleteWalk);
        }
    }
}