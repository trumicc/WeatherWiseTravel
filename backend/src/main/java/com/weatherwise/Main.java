package com.weatherwise;

import com.weatherwise.models.Activity;
import com.weatherwise.models.Weather;
import com.weatherwise.services.LocationService;
import com.weatherwise.services.WeatherService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.sql.SQLOutput;
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
    
    // TODO: implement
    private static void handleRecommendations(Context ctx) {
        String city = ctx.queryParam("city");
        
        if (city == null || city.isEmpty()) {
            ctx.status(400).json(new ErrorResponse("Missing city parameter"));
            return;
        }
        
        ctx.json(new Response("Recommendations", "1.0", "TODO: implement"));
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
