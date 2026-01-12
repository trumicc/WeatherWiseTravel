package com.weatherwise.services;

import com.weatherwise.models.Activity;
import com.weatherwise.models.Recommendation;
import com.weatherwise.models.Weather;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Rekommendationsklass som beräknar poängen för varje aktivitet
 */
public class RecommendationEngine {

    private static final double COLD_TEMP = 10.0;
    private static final double WARM_TEMP = 20.0;

    private static final double STRONG_WIND = 25.0;
    private static final int HIGH_HUMIDITY = 80;

    private static final int MAX_RESULTS = 15;

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

            LocalDateTime time = LocalDateTime.now();
            score = checkCategoryTimeBonus(time, activity, score, reasons);

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

        recommendations = diversifyRecommendations(recommendations);

        recommendations.sort((r1, r2) -> Integer.compare(r2.getScore(), r1.getScore()));

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

    private List<Recommendation> diversifyRecommendations(List<Recommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return recommendations;
        }

        Map<String, List<Recommendation>> perCategory = new HashMap<>();

        for (Recommendation recommendation : recommendations) {
            String category = recommendation.getActivity().getCategory();
            if (!perCategory.containsKey(category)) {
                perCategory.put(category, new ArrayList<>());
            }
            perCategory.get(category).add(recommendation);
        }

        List<Recommendation> result = new ArrayList<>();

        for (List<Recommendation> categoryRecommendation : perCategory.values()) {
            int count = Math.min(2, categoryRecommendation.size());
            for (int i = 0; i < count; i++) {
                result.add(categoryRecommendation.get(i));
            }
        }

        for (Recommendation recommendation : recommendations) {
            if (result.size() >= MAX_RESULTS) {
                break;
            }

            if (!result.contains(recommendation)) {
                result.add(recommendation);
            }
        }

        return result;
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

        if (category.equals("cafe") && temperature < COLD_TEMP) {
            score += 10;
            reasons.add("A warm cafe is perfect for cold weather");
        }
        if (category.equals("park") && temperature > WARM_TEMP - 5) {
            score += 15;
            reasons.add("Great weather for enjoying the outdoors in the park");
        }

        return score;
    }

    private int checkCategoryTimeBonus(LocalDateTime time, Activity activity, int score, List<String> reasons) {
        String category = activity.getCategory();
        int hour = time.getHour();

        if (category.equals("mall") && hour >= 10 && hour < 19) {
            score += 5;
            reasons.add("Stores are open for shopping");
            return score;
        }
        if (category.equals("cafe") && ((hour >= 7 && hour < 10) || (hour >= 14 && hour < 17))) {
            score += 5;
            reasons.add("Good time for a fika");
            return score;
        }
        if (category.equals("restaurant") && ((hour >= 11 && hour < 14) || (hour >= 18 && hour < 21))){
            score += 5;
            reasons.add("Ideal time for a warm meal");
            return score;
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

        return String.join(". ", reasons);
    }

}
