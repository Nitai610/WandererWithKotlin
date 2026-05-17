package com.nitai.wanderer;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Walk {

    // ==========================================
    // --- 1. CLASS VARIABLES (PROPERTIES) ---
    // ==========================================
    public String username;
    public String distance;
    public String time;
    public String steps;
    public String calories; // NEW: Added to track calories burned
    public String date;

    // NOTE FOR BAGRUT: Why HashMaps instead of LatLng?
    // "Firestore doesn't natively understand Google's LatLng object. I have to translate the complex
    // GPS coordinates into simple X and Y numbers (HashMaps) so they can be saved in the cloud."
    public List<HashMap<String, Double>> path;
    public static ArrayList<Walk> walkHistory = new ArrayList<>();

    // ==========================================
    // --- 2. CONSTRUCTORS ---
    // ==========================================

    // NOTE FOR BAGRUT: Why is there an empty constructor?
    // "Firebase Firestore uses serialization. When it downloads a document, it creates a blank 'Walk' object
    // first using this empty constructor, and then it fills in the variables one by one. If I delete this, the app crashes."
    public Walk() {
    }

    // NOTE: This constructor bundles all the data together when the user finishes a walk.
    public Walk(String username, String distance, String time, String steps, String calories, String date, ArrayList<LatLng> googlePath) {
        this.username = username;
        this.distance = distance;
        this.time = time;
        this.steps = steps;
        this.calories = calories;
        this.date = date;

        this.path = new ArrayList<>();
        if (googlePath != null) {
            for (LatLng point : googlePath) {
                HashMap<String, Double> cord = new HashMap<>();
                cord.put("lat", point.latitude);
                cord.put("lng", point.longitude);
                this.path.add(cord);
            }
        }
    }

    // ==========================================
    // --- 3. GETTERS ---
    // ==========================================
    public String getUsername() { return username; }
    public String getDate() { return date; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }
    public String getSteps() { return steps; }
    public String getCalories() { return calories; }

    // NOTE: Translates the HashMaps back into Google LatLng so we can draw the blue line on the map.
    public ArrayList<LatLng> getGooglePath() {
        ArrayList<LatLng> googlePath = new ArrayList<>();
        if (path != null) {
            for (HashMap<String, Double> p : path) {
                googlePath.add(new LatLng(p.get("lat"), p.get("lng")));
            }
        }
        return googlePath;
    }

    // ==========================================
    // --- 4. ANALYTICS ENGINE (MATH METHODS) ---
    // ==========================================

    // --- DISTANCE MATH ---
    public static float calculateAllTimeDistance() {
        float total = 0f;
        for (Walk walk : walkHistory) {
            try { // NOTE FOR BAGRUT: We use try/catch to prevent the app from crashing if the text isn't a valid number.
                String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                total += Float.parseFloat(numberOnly);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    public static float calculateMonthlyDistance() {
        float total = 0f;
        // NOTE: Calendar helps us figure out what month we are currently in.
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                // NOTE: We convert the saved "String" date back into a real Java "Date" object so we can compare it.
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

    // --- TIME MATH ---
    // NOTE: It is impossible to do math on "15:30". So we write a helper method to turn it into raw seconds.
    private static int parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.trim().split(":"); // Splits "15:30" into an array -> ["15", "30"]
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]); // Minutes * 60 + Seconds
            } else if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]); // Hours * 3600...
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // NOTE: After we add up all the seconds, we must format it back to a beautiful string to show on the screen.
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

    // --- STEPS MATH ---
    // NOTE FOR BAGRUT: "Why do you check if walk.steps != null?"
    // ANSWER: "Backward compatibility! I added the steps feature later in the project. If I download an old walk
    // from my database that doesn't have a 'steps' field, it will be null. Checking for null prevents a NullPointerException crash."

    public static int calculateAllTimeSteps() {
        int total = 0;
        for (Walk walk : walkHistory) {
            try {
                if (walk.steps != null && !walk.steps.isEmpty()) {
                    String numberOnly = walk.steps.replace(" Steps", "").trim();
                    total += Integer.parseInt(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    public static int calculateMonthlySteps() {
        int total = 0;
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
                    if (walk.steps != null && !walk.steps.isEmpty()) {
                        String numberOnly = walk.steps.replace(" Steps", "").trim();
                        total += Integer.parseInt(numberOnly);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    public static int calculateWeeklySteps() {
        int total = 0;
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
                    if (walk.steps != null && !walk.steps.isEmpty()) {
                        String numberOnly = walk.steps.replace(" Steps", "").trim();
                        total += Integer.parseInt(numberOnly);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }
}