package com.nitai.wanderer;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Walk {

    // --- VARIABLES ---
    public String username; // Added so the Leaderboard knows who did this walk!
    public String distance;
    public String time;
    public String date;

    // NOTE: BAGRUT FIRESTORE TRICK
    // Cloud Firestore is a "JSON-like" NoSQL database. It cannot easily save complex
    // Android objects like Google's 'LatLng'. So, we change the path into a simple
    // list of HashMaps (which are just basic pairs of numbers: "lat" and "lng").
    public List<HashMap<String, Double>> path;

    // The master list that holds all downloaded walks in the phone's RAM while the app is open
    public static ArrayList<Walk> walkHistory = new ArrayList<>();

    // --- 1. THE EMPTY CONSTRUCTOR ---
    // NOTE: This is MANDATORY for Cloud Firestore. When Firestore downloads your data
    // from the cloud, it creates a blank 'Walk' object first using this empty constructor.
    public Walk() {
    }

    // --- 2. THE CREATION CONSTRUCTOR ---
    // This is called by SummaryActivity when you finish a brand new walk.
    public Walk(String username, String distance, String time, String date, ArrayList<LatLng> googlePath) {
        this.username = username;
        this.distance = distance;
        this.time = time;
        this.date = date;

        // NOTE: We take the complex Google Map path and translate it into simple numbers.
        this.path = new ArrayList<>();
        if (googlePath != null) {
            for (LatLng point : googlePath) {
                // Create a simple dictionary/map for each GPS point
                HashMap<String, Double> cord = new HashMap<>();
                cord.put("lat", point.latitude);
                cord.put("lng", point.longitude);

                // Add the simple point to our Firestore-friendly list
                this.path.add(cord);
            }
        }
    }

    // --- GETTERS (Used by the Adapters to show data on screen) ---
    public String getUsername() { return username; }
    public String getDate() { return date; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }

    // NOTE: When the user clicks an old walk, we need to draw it on the Google Map.
    // This method translates the simple Firestore numbers BACK into complex Google LatLng objects!
    public ArrayList<LatLng> getGooglePath() {
        ArrayList<LatLng> googlePath = new ArrayList<>();
        if (path != null) {
            for (HashMap<String, Double> p : path) {
                // Rebuild the Google Map coordinate using the saved numbers
                googlePath.add(new LatLng(p.get("lat"), p.get("lng")));
            }
        }
        return googlePath;
    }

    // =====================================================================
    // --- ANALYTICS ENGINE (Your math code for the Stats and Profile pages) ---
    // =====================================================================

    public static float calculateAllTimeDistance() {
        float total = 0f;
        for (Walk walk : walkHistory) {
            try {
                String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                total += Float.parseFloat(numberOnly);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    public static float calculateMonthlyDistance() {
        float total = 0f;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                if (walkCal.get(java.util.Calendar.MONTH) == currentMonth && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                    total += Float.parseFloat(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    public static float calculateWeeklyDistance() {
        float total = 0f;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentWeek = now.get(java.util.Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                if (walkCal.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                    total += Float.parseFloat(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    // Helper: Turns "15:30" (MM:SS) into raw seconds for easier math
    private static int parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.trim().split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // Helper: Turns raw seconds back into a beautiful "00:00:00" format
    private static String formatSecondsToTimeString(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    public static String calculateAllTimeDuration() {
        int totalSeconds = 0;
        for (Walk walk : walkHistory) {
            totalSeconds += parseTimeToSeconds(walk.time);
        }
        return formatSecondsToTimeString(totalSeconds);
    }

    public static String calculateMonthlyDuration() {
        int totalSeconds = 0;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                if (walkCal.get(java.util.Calendar.MONTH) == currentMonth && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    totalSeconds += parseTimeToSeconds(walk.time);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return formatSecondsToTimeString(totalSeconds);
    }

    public static String calculateWeeklyDuration() {
        int totalSeconds = 0;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentWeek = now.get(java.util.Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                if (walkCal.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    totalSeconds += parseTimeToSeconds(walk.time);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return formatSecondsToTimeString(totalSeconds);
    }
}