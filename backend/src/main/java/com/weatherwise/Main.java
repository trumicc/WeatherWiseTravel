package com.weatherwise;

import com.weatherwise.models.Activity;
import com.weatherwise.models.Weather;
import com.weatherwise.services.LocationService;
import com.weatherwise.services.WeatherService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.SQLOutput;
import java.util.List;
import java.util.Properties;

public class Main {
    
    public static void main(String[] args) {

        Properties props = new Properties();
        try {
            props.load(Main.class.getClassLoader().getResourceAsStream("config.properties"));
            String apiKey = props.getProperty("OPENWEATHER_API_KEY");

            WeatherService ws = new WeatherService(apiKey);
            Weather weather= ws.getWeather("Stockholm");

            if (weather != null) {
                System.out.println("Weather in " + weather.getCity() + ": " + weather.getTemperature() + "°C, " + weather.getCondition() + ". Det är: " + weather.getDescription() + ", luftfuktighet: " + weather.getHumidity() + "%, vindhastighet: " + weather.getWindSpeed() + " m/s");
            } else {
                System.out.println("Failed to retrieve weather data.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        LocationService ls = new LocationService();
        List<Activity> activities = ls.getActivities("Stockholm");
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

        // Main endpoints - TODO: implement these
        app.get("/api/v1/recommendations", ctx -> handleRecommendations(ctx));
        app.get("/api/v1/weather/{city}", ctx -> handleWeather(ctx));
        app.get("/api/v1/activities", ctx -> handleActivities(ctx));



        app.start(7000);
        System.out.println("Server started on port 7000");
    }
    
    // TODO: implement
    private static void handleRecommendations(Context ctx) {
        String city = ctx.queryParam("city");
        
        if (city == null || city.isEmpty()) {
            ctx.status(400).json(new ErrorResponse("Missing city parameter"));
            return;
        }
        
        ctx.json(new Response("Recommendations", "1.0", "TODO: implement"));
    }
    
    // TODO: implement
    private static void handleWeather(Context ctx) {
        String city = ctx.pathParam("city");
        ctx.json(new Response("Weather for " + city, "1.0", "TODO: implement"));
    }
    
    // TODO: implement
    private static void handleActivities(Context ctx) {
        String city = ctx.queryParam("city");
        
        if (city == null || city.isEmpty()) {
            ctx.status(400).json(new ErrorResponse("Missing city parameter"));
            return;
        }
        
        ctx.json(new Response("Activities for " + city, "1.0", "TODO: implement"));
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
