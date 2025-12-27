package com.weatherwise.services;
import com.weatherwise.models.Weather;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Weather Service class, calls the openWeathersAPI and returns the weather,
 * temperature and wind speed.
 */
public class WeatherService {
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private String apiKey;
    private final ObjectMapper mapper;

    /**
     * WeatherService constructor
     * @param apiKey
     */
    public WeatherService(String apiKey) {
        this.apiKey = apiKey;
        this.mapper = new ObjectMapper();
    }

    /**
     * main function that returns the weather for a specific city
     * @param city
     * @return returns a Weather object
     */
    public Weather getWeather(String city) {
        if (city == null || city.trim().isEmpty()) {
            System.out.println("Error: City name cannot be empty");
            return null;
        }

        // generate a URL with city and key;
        String url = buildUrl(city);
        System.out.println("URL: " + url);

        try {
            HttpResponse<String> response = Unirest.get(url).asString();
            if (response.getStatus() != 200) {
                System.out.println("Error: " + response.getStatus() + " - " + response.getBody());
                return null;
            }
            return parseWeatherResponse(response.getBody());

        } catch (Exception e) {
            System.out.println("Error fetching weather data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * parse the json object to a Weather object
     * @param json
     * @return a weather object
     */
    private Weather parseWeatherResponse(String json) {
        try {
            JsonNode root = mapper.readTree(json);

            String cityName = root.path("name").asText();
            double temperature = root.path("main").path("temp").asDouble();
            String condition = root.path("weather").get(0).path("main").asText();
            String description = root.path("weather").get(0).path("description").asText();
            int humidity = root.path("main").path("humidity").asInt();
            double windSpeed = root.path("wind").path("speed").asDouble();

            return new Weather(cityName, temperature, condition, description, humidity, windSpeed);
        } catch (Exception e) {
            System.out.println("Error parsing weather JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * this function generate a URL based on the APIKey and the given city
     * @param city
     * @return a URL with city
     */
    private String buildUrl(String city) {
        return API_URL + "?q=" + city + "&appid=" + apiKey + "&units=metric" + "&lang=sv";
    }

    /**
     * Fetch weather based on coordinates
     * @param lat latitude
     * @param lon longitude
     * @return Weather on coordinates
     */
    public Weather getWeatherByCoordinates(double lat, double lon) {

        String url = buildUrlByCoordinates(lat, lon);
        System.out.println("URL: " + url);

        try {
            HttpResponse<String> response = Unirest.get(url).asString();
            if (response.getStatus() != 200) {
                System.out.println("Error: " + response.getStatus() + " - " + response.getBody());
                return null;
            }

            return parseWeatherResponse(response.getBody());
        } catch (Exception e) {
            System.out.println("Error fetching weather data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate URL based on coordinates
     * @param lat latitude
     * @param lon longitude
     * @return url
     */
    private String buildUrlByCoordinates(double lat, double lon) {
        return API_URL + "?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=metric" + "&lang=sv";
    }

}
