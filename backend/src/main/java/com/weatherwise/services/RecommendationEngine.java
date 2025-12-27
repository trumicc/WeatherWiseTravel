package com.weatherwise.services;

import com.weatherwise.models.Activity;
import com.weatherwise.models.Recommendation;
import com.weatherwise.models.Weather;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEngine {

    public RecommendationEngine() {

    }

    public List<Recommendation> getRecommendations(Weather weather, List<Activity> activities) {

        List<Recommendation> recommendations = new ArrayList<>(); // Lista som ska fyllas med reakomadtioner

        // V2 - Algoritm för att skapa rekommendationer baserat på väder och aktiviteter

        for (Activity activity : activities) {
            int score = 50; // vi använder en scala 0-100 och alla börjar i mitten
            String reason = ""; // socre ska följa med nån form av resonemang

            if (weather.getTemperature() < 10) { // basic kalt väder
                if (activity.isIndoor()) {
                    score += 25; // ska testas, vi borjar så - lättare och räkna
                    reason = " Staying indoors may be more comfortable";
                } else {
                    score -= 10;
                    reason = " It's quite cold outside";
                }

            } else if (weather.getTemperature() > 20) { // basic varm väder
                if (!activity.isIndoor()) {
                    score += 20;
                    reason = " Enjoy the warm weather outdoors";
                } else {
                    score -= 10;
                    reason = " It's a nice day outside";
                }
            } else {
                reason = " Weather is nice for most activities";
            }

            // Regn och snö påverkan
            if (weather.getCondition().equals("Rain") || weather.getCondition().equals("Snow")) {
                if (activity.isIndoor()) {
                    score += 30;
                    reason = "Indoors is more preferable in with conditions like this";
                } else {
                    score -= 30;
                    reason = "Outdoor activities may be less enjoyable in this weather";
                }
            }

            // Vind påverkan
            if (weather.getWindSpeed() > 25) { // stark vind enlight googel
                if (!activity.isIndoor()) {
                    score -= 20;
                    reason = "You must exceed minimum weight requirements for strong wind conditions";
                }
            }

            // Kanske  kolla Humidity
            if (weather.getHumidity() > 80) {
                if (activity.isIndoor()) {
                    score += 10;
                    reason = "High humidity makes indoor activities more comfortable";
                }
            }

            // ha en reason för café
            if (activity.getCategory().equals("Cafe")) {
                if (weather.getTemperature() < 10) { // eller annat temp
                    score += 15;
                    reason = "A warm beverage is perfect for cold weather";
                }
            }
            // samma för park
            if (activity.getCategory().equals("Park")) {
                if (weather.getTemperature() > 15 && weather.getCondition().equals("Clear")) {
                    score += 15;
                    reason = "Great weather for enjoying the outdoors in the park";
                }
            }

            // Begränsa score till 0-100
            if (score > 100) score = 100;
            if (score < 0) score = 0;

            Recommendation rec = new Recommendation(activity, score, reason);
            recommendations.add(rec);
        }
        // Sortera rekommendationer baserat på score
        recommendations.sort((r1, r2) -> Integer.compare(r2.getScore(), r1.getScore()));
        // Returnera topp 10
        if (recommendations.size() > 10) {
            recommendations = recommendations.subList(0, 10);
        }

        return recommendations;

    }
}
