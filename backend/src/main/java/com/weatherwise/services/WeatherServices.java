package com.weatherwise.services;
import com.weatherwise.models.Weather;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


public class WeatherServices {
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private String apiKey;



    public WeatherServices(String apiKey) {
        this.apiKey = apiKey;
    }

    // main method som andra kallar för väder data
    public Weather getWeather(String city) {

        String url = buildUrl(city);
        System.out.println("URL: " + url);
        // TODO: koda: URL med city och key; HTTP request; parse JSON response; skapa och returnera Weather object

        HttpResponse<String> response = Unirest.get(url).asString();
        if (response.getStatus() != 200) {
            System.out.println("Error: " + response.getStatus() + "body" + response.getBody());
            return null;
        }

        String json = response.getBody();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            String cityName = root.path("name").asText();
            double temperature = root.path("main").path("temp").asDouble();
            String condition = root.path("weather").get(0).path("main").asText();
            String description = root.path("weather").get(0).path("description").asText();
            int humidity = root.path("main").path("humidity").asInt();
            double windSpeed = root.path("wind").path("speed").asDouble();

            Weather weather = new Weather(cityName, temperature, condition, description, humidity, windSpeed);
            return weather;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

    private String buildUrl(String city) {
        String url = API_URL + "?q=" + city + "&appid=" + apiKey + "&units=metric" + "&lang=sv";

        return url;
    }

}
