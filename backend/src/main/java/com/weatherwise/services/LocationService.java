package com.weatherwise.services;
import com.weatherwise.models.Activity;
import java.util.List;
import java.util.ArrayList;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


public class LocationService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    public LocationService() {

    }

    public List<Activity> getActivities(String city) {
        //  tom lista för activitis,
        List<Activity> activities = new ArrayList<>();
        // sök efter activiteter - museum
        List<Activity> museums = searchCategory(city, "museum");
        activities.addAll(museums);
        // cafe
        List<Activity> cafes = searchCategory(city, "cafe");
        activities.addAll(cafes);
        // park
        List<Activity> parks = searchCategory(city, "park");
        activities.addAll(parks);

        return activities;
    }

    private List<Activity> searchCategory(String city, String category) {

        String url = NOMINATIM_URL + "?q=" + city + "+" + category + "&format=json&limit=10";
        // HTTP request
        HttpResponse<String> response = Unirest.get(url).header("User-Agent", "WeatherWiseTravel/1.0 (student)").asString();

        if (response.getStatus() != 200) {
            System.out.println("Error: " + response.getStatus() + " - " + response.getStatusText());
            return new ArrayList<>();
        }

        String json = response.getBody();

        // parse JSON response
        List<Activity> result = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            // svaret är array
            for (JsonNode place : root) {
                String name = place.path("display_name").asText();
                double lat = place.path("lat").asDouble();
                double lon = place.path("lon").asDouble();
                String type = place.path("type").asText();
                int id = place.path("place_id").asInt();

                // indor eller outdor
                boolean indoor = isIndoor(type);
                // skapa ArrayList<Activity> från resultatet
                Activity activity = new Activity(id, name, category, lat, lon, indoor);
                result.add(activity);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

        return result;
    }
    private boolean isIndoor(String type) {
        // koda vilka typer som är indoor
        return switch (type.toLowerCase()) {
            case "museum", "theatre", "cinema", "library", "mall" -> true;
            default -> false;
        };

    }

}
