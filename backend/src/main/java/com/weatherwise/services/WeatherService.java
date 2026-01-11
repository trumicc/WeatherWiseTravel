package com.weatherwise.services;
import com.weatherwise.models.Weather;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Weather Service-klass som anropar OpenWeather-API:t och returnerar väder,
 * temperatur och vindhastighet.
 */
public class WeatherService {
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private String apiKey;
    private final ObjectMapper mapper;

    /**
     *WeatherService-konstruktor
     * @param apiKey
     */
    public WeatherService(String apiKey) {
        this.apiKey = apiKey;
        this.mapper = new ObjectMapper();
    }

    /**
     * huvudfunktion som returnerar vädret för en specifik stad
     * @param city
     * @return returnerar ett Weather-objekt
     */
    public Weather getWeather(String city) {
        if (city == null || city.trim().isEmpty()) {
            System.out.println("Error: City name cannot be empty");
            return null;
        }

        // generera en URL med stad och nyckel;
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
     * parsa JSON-objektet till ett Weather-objekt
     * @param json
     * @return ett Weather-objekt
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
     * den här funktionen genererar en URL baserad på API-nyckeln och den angivna staden
     * @param city
     * @return en URL med stad
     */
    private String buildUrl(String city) {
        return API_URL + "?q=" + city + "&appid=" + apiKey + "&units=metric" + "&lang=sv";
    }

    /**
     * Hämta väder baserat på koordinater
     * @param lat latitud
     * @param lon longitud
     * @return Väder baserat på koordinater
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
     * Hämta väder baserat på koordinater
     * @param lat latitud
     * @param lon longitud
     * @return url
     */
    private String buildUrlByCoordinates(double lat, double lon) {
        return API_URL + "?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=metric" + "&lang=sv";
    }

}
