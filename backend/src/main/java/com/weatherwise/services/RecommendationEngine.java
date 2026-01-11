package com.weatherwise.services;

import com.weatherwise.models.Activity;
import com.weatherwise.models.Recommendation;
import com.weatherwise.models.Weather;

import java.util.ArrayList;
import java.util.List;

/**
 * Rekommendationsklass som beräknar poängen för varje aktivitet
 */
public class RecommendationEngine {

    private static final double COLD_TEMP = 10.0;
    private static final double WARM_TEMP = 20.0;

    private static final double STRONG_WIND = 25.0;
    private static final int HIGH_HUMIDITY = 80;

    private static final int MAX_RESULTS = 10;

    public RecommendationEngine() {

    }

    /**
     * Rekommendationsklass som beräknar poängen för varje aktivitet
     * @param weather väder objekt
     * @param activities Lista av aktiviteter
     * @return rekomendation Objekt
     */
    public List<Recommendation> getRecommendations(Weather weather, List<Activity> activities) {

        if (weather == null || activities == null || activities.isEmpty()) {
            return new ArrayList<>();
        }

        List<Recommendation> recommendations = new ArrayList<>();

        for (Activity activity : activities) {
            if (activity == null) {
                continue;
            }

            int score = 50;
            List<String> reasons = new ArrayList<>();

            score = checkTemperature(weather, activity, score, reasons);
            score = checkCondition(weather, activity, score, reasons);
            score = checkWind(weather, activity, score, reasons);
            score = checkHumidity(weather, activity, score, reasons);
            score = checkCategoryBonus(weather, activity, score, reasons);

            if (score > 100) {
                score = 100;
            }
            if (score < 0) {
                score = 0;
            }

            String allReasons = buildString(reasons);

            recommendations.add(new Recommendation(activity, score, allReasons));

        }

        recommendations.sort((r1, r2) -> Integer.compare(r2.getScore(), r1.getScore()));

        if (recommendations.size() > MAX_RESULTS) {
            recommendations = recommendations.subList(0, MAX_RESULTS);
        }

        return recommendations;
    }

    /**
     * beräkna poäng baserat på vädertemperaturen
     *
     * @param weather väder
     * @param activity aktivitet
     * @param score poäng
     * @param reasons anledningar
     * @return poäng efter beräkning
     */
    private int checkTemperature(Weather weather, Activity activity, int score, List<String> reasons) {
        double temperature = weather.getTemperature();

        if (temperature < COLD_TEMP) {
            if (activity.isIndoor()) {
                score += 25;
                reasons.add("Staying indoors may be more comfortable");
            } else {
                score -= 10;
                reasons.add("It's quite cold outside");
            }
        } else if (temperature > WARM_TEMP) {
            if (!activity.isIndoor()) {
                score += 20;
                reasons.add("Enjoy the warm weather outdoors");
            } else {
                score -= 10;
                reasons.add("It's a nice day outside");
            }
        } else {
            reasons.add("Weather is nice for most activities");
        }
        return score;
    }

    /**
     * beräkna poäng baserat på vädertemperaturen
     *
     * @param weather väder
     * @param activity aktivitet
     * @param score poäng
     * @param reasons anledningar 
     * @return poäng efter beräkning
     */
    private int checkCondition(Weather weather, Activity activity, int score, List<String> reasons) {
        String condition = weather.getCondition();

        if (condition.equals("Rain") || condition.equals("Snow")) {
            if (activity.isIndoor()) {
                score += 30;
                reasons.add("Indoors is more preferable in with conditions like this");
            } else {
                score -= 30;
                reasons.add("Outdoor activities may be less enjoyable in this weather");
            }
        }
        return score;
    }

    /**
     * beräkna poäng baserat på vädertemperaturen
     *
     * @param weather väder
     * @param activity aktivitet
     * @param score poäng
     * @param reasons anledningar
     * @return poäng efter beräkning
     */
    private int checkWind(Weather weather, Activity activity, int score, List<String> reasons) {
        double wind = weather.getWindSpeed();

        if (wind > STRONG_WIND) {
            if (!activity.isIndoor()) {
                score -= 20;
                reasons.add("You must exceed minimum weight requirements for strong wind conditions");
            }
        }

        return score;
    }

    /**
     * beräkna poäng baserat på luftfuktigheten
     *
     * @param weather väder
     * @param activity aktivitet
     * @param score poäng
     * @param reasons anledningar
     * @return poäng efter beräkning
     */
    private int checkHumidity(Weather weather, Activity activity, int score, List<String> reasons) {
        double humidity = weather.getHumidity();

        if (humidity > HIGH_HUMIDITY) {
            if (activity.isIndoor()) {
                score += 10;
                reasons.add("High humidity makes indoor activities more comfortable");
            }
        }

        return score;
    }

    /**
     * beräkna poäng baserat på vädertemperatur och aktivitetstyp
     *
     * @param weather väder
     * @param activity aktivitet
     * @param score poäng
     * @param reasons anledningar
     * @return beräknad poäng
     */
    private int checkCategoryBonus(Weather weather, Activity activity, int score, List<String> reasons) {
        double temperature = weather.getTemperature();
        String category = activity.getCategory();

        if (category.equals("Cafe") && temperature < COLD_TEMP) {
            score += 10;
            reasons.add("A warm beverage is perfect for cold weather");
        }
        if (category.equals("Park") && temperature > WARM_TEMP - 5) {
            score += 15;
            reasons.add("Great weather for enjoying the outdoors in the park");
        }

        return score;

    }

    /**
     * bygga en sträng med alla anledningar
     * @param reasons anledningar
     * @return en sträng med anledningar
     */
    private String buildString(List<String> reasons) {
        if (reasons.isEmpty()) {
            return "Weather is suitable for this activity";
        }

        return String.join(". ", reasons + ".");
    }

}
