package com.nitai.wanderer;

// 'Comparable' allows Java to automatically sort a list of these users!
public class LeaderboardUser implements Comparable<LeaderboardUser> {
    public String username;
    public float totalDistance;
    public int totalTimeSeconds;

    public LeaderboardUser(String username) {
        this.username = username;
        this.totalDistance = 0f;
        this.totalTimeSeconds = 0;
    }

    // Helper method to add distance from a string (like "3.50 KM")
    public void addDistance(String distanceStr) {
        try {
            String numberOnly = distanceStr.replace(" KM", "").replace(",", ".").trim();
            this.totalDistance += Float.parseFloat(numberOnly);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Helper method to add time from a string (like "15:30")
    public void addTime(String timeStr) {
        try {
            String[] parts = timeStr.trim().split(":");
            if (parts.length == 2) {
                this.totalTimeSeconds += (Integer.parseInt(parts[0]) * 60) + Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                this.totalTimeSeconds += (Integer.parseInt(parts[0]) * 3600) + (Integer.parseInt(parts[1]) * 60) + Integer.parseInt(parts[2]);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Formats the raw seconds back into a pretty "00:00:00" string for the screen
    public String getFormattedTime() {
        int hours = totalTimeSeconds / 3600;
        int minutes = (totalTimeSeconds % 3600) / 60;
        int seconds = totalTimeSeconds % 60;
        if (hours > 0) {
            return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    // BAGRUT REQUIREMENT: This tells Java HOW to sort the users.
    // We sort by distance. If distance is equal, we sort by who was faster!
    @Override
    public int compareTo(LeaderboardUser other) {
        if (this.totalDistance != other.totalDistance) {
            // Sort distance descending (highest first)
            return Float.compare(other.totalDistance, this.totalDistance);
        } else {
            // If distances are tied, sort time ascending (lowest/fastest first)
            return Integer.compare(this.totalTimeSeconds, other.totalTimeSeconds);
        }
    }
}