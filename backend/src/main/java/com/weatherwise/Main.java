package com.weatherwise;

import com.weatherwise.models.Activity;
import com.weatherwise.models.Recommendation;
import com.weatherwise.models.Weather;
import com.weatherwise.services.LocationService;
import com.weatherwise.services.RecommendationEngine;
import com.weatherwise.services.WeatherService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class Main {

    private static WeatherService weatherService;
    private static LocationService locationService;
    private static RecommendationEngine recommendationEngine;

    public static void main(String[] args) throws IOException {

        // --- Load config.properties safely ---
        Properties props = new Properties();
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new RuntimeException("config.properties not found. Put it in src/main/resources/");
            }
            props.load(in);
        }

        String apiKey = props.getProperty("OPENWEATHER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("OPENWEATHER_API_KEY is missing in config.properties");
        }

        weatherService = new WeatherService(apiKey);
        locationService = new LocationService();
        recommendationEngine = new RecommendationEngine();

        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> cors.add(it -> it.anyHost()));

            // Serve frontend from: src/main/resources/public
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";      // http://localhost:7001/
                staticFiles.directory = "/public"; // resources/public
                staticFiles.location = Location.CLASSPATH;
            });
        });

        // API root (so "/" can be the website)
        app.get("/api", ctx -> ctx.json(new Response("WeatherWise Travel API", "1.0", "Running")));
        app.get("/health", ctx -> ctx.json(new Response("OK", "1.0", "Healthy")));
        app.get("/api/v1/test", ctx -> ctx.json(new Response("Test", "1.0", "Works!")));

        // Weather & activities endpoints
        app.get("/api/v1/weather/coordinates", Main::handleWeatherByCoordinates);
        app.get("/api/v1/activities/coordinates", Main::handleActivitiesByCoordinates);
        app.get("/api/v1/recommendations/coordinates", Main::handleRecommendationsByCoordinates);

        app.get("/api/v1/weather/{city}", Main::handleWeather);
        app.get("/api/v1/activities", Main::handleActivities);
        app.get("/api/v1/recommendations", Main::handleRecommendations);

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
            Weather weather = weatherService.getWeather(city);
            if (weather == null) {
                ctx.status(404).json(new ErrorResponse("Weather NOT FOUND for city: " + city));
                return;
            }

            List<Activity> activities = locationService.getActivities(city);
            if (activities == null || activities.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("Activities NOT FOUND for city: " + city));
                return;
            }

            List<Recommendation> recommendations = recommendationEngine.getRecommendations(weather, activities);
            if (recommendations != null && !recommendations.isEmpty()) {
                ctx.status(200).json(recommendations);
            } else {
                ctx.status(404).json(new ErrorResponse("Recommendation data not found"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("Server error: " + e.getMessage()));
        }
    }

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
                ctx.status(404).json(new ErrorResponse("Weather data not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("Server error: " + e.getMessage()));
        }
    }

    private static void handleActivities(Context ctx) {
        String city = ctx.queryParam("city");

        if (city == null || city.isEmpty()) {
            ctx.status(400).json(new ErrorResponse("Missing city parameter"));
            return;
        }

        try {
            List<Activity> activities = locationService.getActivities(city);
            if (activities != null && !activities.isEmpty()) {
                ctx.status(200).json(activities);
            } else {
                ctx.status(404).json(new ErrorResponse("Activities data not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("Server error: " + e.getMessage()));
        }
    }

    private static void handleWeatherByCoordinates(Context ctx) {
        String latStr = ctx.queryParam("lat");
        String lonStr = ctx.queryParam("lon");

        if (latStr == null || lonStr == null) {
            ctx.status(400).json(new ErrorResponse("Missing lat or lon parameter"));
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);

            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                ctx.status(400).json(new ErrorResponse("Invalid coordinates"));
                return;
            }

            Weather weather = weatherService.getWeatherByCoordinates(lat, lon);

            if (weather != null) {
                ctx.status(200).json(weather);
            } else {
                ctx.status(404).json(new ErrorResponse("Weather data not found"));
            }
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("Invalid coordinate format"));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("Server error: " + e.getMessage()));
        }
    }

    private static void handleActivitiesByCoordinates(Context ctx) {
        String latStr = ctx.queryParam("lat");
        String lonStr = ctx.queryParam("lon");

        if (latStr == null || lonStr == null) {
            ctx.status(400).json(new ErrorResponse("Missing lat or lon parameter"));
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);

            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                ctx.status(400).json(new ErrorResponse("Invalid coordinates"));
                return;
            }

            List<Activity> activities = locationService.getActivitiesByCoordinates(lat, lon);

            if (activities != null && !activities.isEmpty()) {
                ctx.status(200).json(activities);
            } else {
                ctx.status(404).json(new ErrorResponse("Activities data not found"));
            }
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("Invalid coordinate format"));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("Server error: " + e.getMessage()));
        }
    }

    private static void handleRecommendationsByCoordinates(Context ctx) {
        String latStr = ctx.queryParam("lat");
        String lonStr = ctx.queryParam("lon");

        if (latStr == null || lonStr == null) {
            ctx.status(400).json(new ErrorResponse("Missing lat or lon parameter"));
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);

            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                ctx.status(400).json(new ErrorResponse("Invalid coordinates"));
                return;
            }

            Weather weather = weatherService.getWeatherByCoordinates(lat, lon);
            if (weather == null) {
                ctx.status(404).json(new ErrorResponse("Weather NOT FOUND for coordinates: [" + lat + ", " + lon + "]"));
                return;
            }

            List<Activity> activities = locationService.getActivitiesByCoordinates(lat, lon);
            if (activities == null || activities.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("Activities NOT FOUND near coordinates: [" + lat + ", " + lon + "]"));
                return;
            }

            List<Recommendation> recommendations = recommendationEngine.getRecommendations(weather, activities);
            ctx.status(200).json(recommendations);

        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("Invalid coordinate format"));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("Server error: " + e.getMessage()));
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
