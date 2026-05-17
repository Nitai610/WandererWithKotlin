package com.nitai.wanderer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private ArrayList<LeaderboardUser> userList;

    public LeaderboardAdapter(ArrayList<LeaderboardUser> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        LeaderboardUser user = userList.get(position);

        holder.tvRank.setText("#" + (position + 1));
        holder.tvLeaderboardUsername.setText(user.username);
        holder.tvLeaderboardDistance.setText(String.format(java.util.Locale.US, "%.2f KM", user.totalDistance));
        holder.tvLeaderboardTime.setText(user.getFormattedTime());

        // Visual Polish: Color the Top 3 players!
        if (position == 0) {
            holder.tvRank.setTextColor(Color.parseColor("#FFD700")); // Gold
        } else if (position == 1) {
            holder.tvRank.setTextColor(Color.parseColor("#C0C0C0")); // Silver
        } else if (position == 2) {
            holder.tvRank.setTextColor(Color.parseColor("#CD7F32")); // Bronze
        } else {
            holder.tvRank.setTextColor(Color.parseColor("#9E9E9E")); // Gray for everyone else
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvLeaderboardUsername, tvLeaderboardDistance, tvLeaderboardTime;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvLeaderboardUsername = itemView.findViewById(R.id.tvLeaderboardUsername);
            tvLeaderboardDistance = itemView.findViewById(R.id.tvLeaderboardDistance);
            tvLeaderboardTime = itemView.findViewById(R.id.tvLeaderboardTime);
        }
    }
}