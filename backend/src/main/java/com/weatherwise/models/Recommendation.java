package com.weatherwise.models;

public class Recommendation {
    private Activity activity;
    private int score;
    private String reason;
    
    public Recommendation() {}
    
    public Recommendation(Activity activity, int score, String reason) {
        this.activity = activity;
        this.score = score;
        this.reason = reason;
    }
    
    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
