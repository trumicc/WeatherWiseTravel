package com.weatherwise.services;

import com.weatherwise.models.Activity;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * LocationService that returns the activity from OpenStreetMap Nominatim API
 */
public class LocationService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "WeatherWiseTravel/1.0 (student)";
    private static final int RESULTS_PER_CATEGORY = 10;

    private static final double SEARCH_RADIUS_KM = 5.0;

    private static final Map<String, Boolean> ACTIVITY_MAP = Map.ofEntries(
            Map.entry("museum", true),
            Map.entry("theatre", true),
            Map.entry("cinema", true),
            Map.entry("library", true),
            Map.entry("mall", true),
            Map.entry("gallery", true),
            Map.entry("suna", true),
            Map.entry("park", false),
            Map.entry("restaurant", false),
            Map.entry("cafe", false),
            Map.entry("gym", false)
    );

    private final ObjectMapper mapper;

    /**
     * LocationService Constructor
     */
    public LocationService() {
        this.mapper = new ObjectMapper();
    }

    /**
     * fetch data for all activities in a city
     * @param city city name
     * @return List with activities in different categories
     */
    public List<Activity> getActivities(String city, List<String> categories) {
        if (city == null || city.trim().isEmpty()) {
            System.out.println("Error: City name cannot be empty");
            return new ArrayList<>();
        }

        List<Activity> activities = new ArrayList<>();

        for (String category : categories) {
            activities.addAll(searchCategory(city, category));
        }

        System.out.println("Found " + activities.size() + " activities in " + city);
        return activities;
    }

    /**
     * calls the API and fetch activities in a specific category
     * @param city city name
     * @param category category
     * @return List with activities in the category
     */
    private List<Activity> searchCategory(String city, String category) {

        String url = buildSearchUrl(city, category);

        try {
            HttpResponse<String> response = Unirest.get(url)
                    .header("User-Agent", USER_AGENT)
                    .asString();

            if (response.getStatus() != 200) {
                System.out.println("API Error for " + category + ": " +
                        response.getStatus() + " - " + response.getStatusText());
                return new ArrayList<>();
            }

            return parseActivitiesResponse(response.getBody(), category);
        } catch (Exception e) {
            System.out.println("Error searching " + category + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Parse the JSON-response from API
     * @param json Json String from API
     * @param category category
     * @return List with activities
     */
    private List<Activity> parseActivitiesResponse(String json, String category) {
        List<Activity> activities = new ArrayList<>();

        try {
            JsonNode root = mapper.readTree(json);
             for (JsonNode place : root) {
                 String name = place.path("display_name").asText();
                 double lat = place.path("lat").asDouble();
                 double lon = place.path("lon").asDouble();
                 String type = place.path("type").asText();
                 int id = place.path("place_id").asInt();

                 // indoor or outdoor activity
                 boolean indoor = isIndoor(type);

                 Activity activity = new Activity(id, name, category, lat, lon, indoor);
                 activities.add(activity);
             }

        } catch (Exception e) {
            System.out.println("Error parsing activities JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return activities;

    }

    /**
     * Decide if an activity is indoor or outdoor
     * @param type type OSM from API
     * @return true if indoor, false if outdoor
     */
    private boolean isIndoor(String type) {
        if(type != null && ACTIVITY_MAP.containsKey(type.toLowerCase())) {
            return ACTIVITY_MAP.get(type.toLowerCase());
        }
        return false;
    }

    /**
     * Build search URL for Nominatim API
     * @param city city name
     * @param category category name
     * @return URL
     */
    private String buildSearchUrl(String city, String category) {
        return String.format("%s?q=%s+%s&format=json&limit=%d&countrycodes=se", // begrensa med Countrycode, gav mig Stockholm caffe i Nederland
                NOMINATIM_URL,
                city,
                category,
                RESULTS_PER_CATEGORY);
    }

    /**
     * Fetch activities based on coordinates
     * @param lat latitude
     * @param lon longitude
     * @return List activities near coordinates
     */
    public List<Activity> getActivitiesByCoordinates(double lat, double lon, List<String> categories) {
        List<Activity> activities = new ArrayList<>();

        if (categories == null || categories.isEmpty()) {
            return new ArrayList<>();
        }

        for (String category : categories) {
            activities.addAll(searchCategoryByCoordinates(lat, lon, category));
        }

        System.out.println("Found " + activities.size() + " activities near coordinates [" + lat + ", " + lon + "]");
        return activities;
    }

    /**
     * Search for activities by coordinates and category
     * @param lat latitude
     * @param lon longitude
     * @param category category
     * @return List with activities
     */
    private List<Activity> searchCategoryByCoordinates(double lat, double lon, String category) {
        String url = buildSearchUrlByCoordinates(lat, lon, category);

        try {
            HttpResponse<String> response = Unirest.get(url).header("User-Agent", USER_AGENT).asString();

            if (response.getStatus() != 200) {
                System.out.println("API Error for " + category + ": " +
                        response.getStatus() + " - " + response.getStatusText());
                return new ArrayList<>();
            }

            return parseActivitiesResponse(response.getBody(), category);
        } catch (Exception e) {
            System.out.println("Error searching " + category + " by coordinates: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Build search URL by coordinates
     * @param lat latitude
     * @param lon longitude
     * @param category category
     * @return URL based coordinate
     */
    private String buildSearchUrlByCoordinates(double lat, double lon, String category) {
        double radiusDegrees = SEARCH_RADIUS_KM / 111.0;

        double minLon = lon - radiusDegrees;
        double maxLon = lon + radiusDegrees;
        double minLat = lat - radiusDegrees;
        double maxLat = lat + radiusDegrees;

        return NOMINATIM_URL + "?q=" + category +
                "&format=json&limit=" + RESULTS_PER_CATEGORY +
                "&viewbox=" + minLon + "," + maxLat + "," + maxLon + "," + minLat +
                "&bounded=1&countrycodes=se";
    }
}
