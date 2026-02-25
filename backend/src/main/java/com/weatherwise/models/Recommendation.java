package com.weatherwise.models;

public class Recommendation {

    private Activity activity;
    private int score;
    private String reason;

    private Integer totalActivities;
    private Integer indoorCount;
    private Integer outdoorCount;
    private Double indoorPercentage;
    private Double outdoorPercentage;

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

    public void setInsights(int total, int indoor, int outdoor) {
        this.totalActivities = total;
        this.indoorCount = indoor;
        this.outdoorCount = outdoor;

        if (total > 0) {
            this.indoorPercentage = (indoor * 100.0) / total;
            this.outdoorPercentage = (outdoor * 100.0) / total;
        }
        
    }
    
    public Integer getTotalActivities() { return totalActivities; }
    public Integer getIndoorCount() { return indoorCount; }
    public Integer getOutdoorCount() { return outdoorCount; }
    public Double getIndoorPercentage() { return indoorPercentage; }
    public Double getOutdoorPercentage() { return outdoorPercentage; }
}