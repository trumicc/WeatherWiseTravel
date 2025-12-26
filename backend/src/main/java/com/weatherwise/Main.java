package com.weatherwise;

import com.weatherwise.models.Activity;
import com.weatherwise.models.Recommendation;
import com.weatherwise.models.Weather;
import com.weatherwise.services.LocationService;
import com.weatherwise.services.WeatherService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

    private static WeatherService weatherService;
    private static LocationService locationService;
    
    public static void main(String[] args) throws IOException {

        Properties props = new Properties();
        props.load(Main.class.getClassLoader().getResourceAsStream("config.properties"));
        String apiKey = props.getProperty("OPENWEATHER_API_KEY");


        weatherService = new WeatherService(apiKey);

        locationService = new LocationService();

        List<Activity> activities = locationService.getActivities("Stockholm");
        System.out.println("Hittade " + activities.size() + " aktiviteter i Stockholm.");

        Javalin app = Javalin.create(config -> {
            // Allow CORS
            config.plugins.enableCors(cors -> {
                cors.add(it -> it.anyHost());
            });
        });
        
        // Basic test endpoint
        app.get("/", ctx -> {
            ctx.json(new Response("WeatherWise Travel API", "1.0", "Running"));
        });
        
        app.get("/health", ctx -> {
            ctx.json(new Response("OK", "1.0", "Healthy"));
        });
        
        app.get("/api/v1/test", ctx -> {
            ctx.json(new Response("Test", "1.0", "Works!"));
        });


        // GET /api/v1/weather/{city} return the weather for a city
        app.get("/api/v1/weather/{city}", ctx -> handleWeather(ctx));

        // GET /api/v1/activities?city={city} return the activities in a city
        app.get("/api/v1/activities", ctx -> handleActivities(ctx));

        // Main endpoints - TODO: implement these
        app.get("/api/v1/recommendations", ctx -> handleRecommendations(ctx));



        app.start(7000);
        System.out.println("Server started on port 7000");
    }
    

    private static void handleRecommendations(Context ctx) {
        String city = ctx.queryParam("city");
        
        if (city == null || city.isEmpty()) {
            ctx.status(400).json(new ErrorResponse("Missing city parameter"));
            return;
        }

        try {
            // Fetch weather
            Weather weather = weatherService.getWeather(city);
            if (weather == null) {
                ctx.status(404).json(new ErrorResponse("Weather NOT FOUND for city: " + city));
                return;
        }

            // Fetch activities
            List<Activity> activities = locationService.getActivities(city);
            if (activities == null || activities.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("Activities NOT FOUND for city: " + city));
                return;
            }

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
            ctx.status(200).json(recommendations);

        } catch (Exception e) {
            System.out.println("Error in handleRecommendations " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("server error: " + e.getMessage()));

            }

    }


    /**
     * GET /api/v1/weather/{city}
     * returns the weather for a city
     * @param ctx
     */
    private static void handleWeather(Context ctx) {
        String city = ctx.pathParam("city");

        if (city == null || city.isEmpty()) {
            ctx.status(400).json(new ErrorResponse("Missing city parameter"));
            return;
        }

        try {
            Weather weather = weatherService.getWeather(city);

            if (weather != null) {
                ctx.status(200).json(weather);
            } else {
                ctx.status(404).json(new ErrorResponse("Weather data not found "));
            }
        } catch (Exception e) {
            System.out.println("Error in handelWeather " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/activities?city={city}
     * return activities in a city
     * @param ctx
     */
    private static void handleActivities(Context ctx) {
        String city = ctx.queryParam("city");

        if (city == null || city.isEmpty()) {
            ctx.status(400).json(new ErrorResponse("Missing city parameter"));
            return;
        }

        try {
            List<Activity> activities = locationService.getActivities(city);

            if(activities != null && !activities.isEmpty()) {
                ctx.status(200).json(activities);
            } else {
                ctx.status(404).json(new ErrorResponse("Activities data not found "));
            }
        } catch (Exception e) {
            System.out.println("Error in handleActivities " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("server error: " + e.getMessage()));
        }
    }
    
    static class Response {
        public String message;
        public String version;
        public String details;
        
        public Response(String message, String version, String details) {
            this.message = message;
            this.version = version;
            this.details = details;
        }
    }
    
    static class ErrorResponse {
        public String error;
        public long timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
