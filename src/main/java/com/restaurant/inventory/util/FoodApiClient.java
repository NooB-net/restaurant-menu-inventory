package com.restaurant.inventory.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Networking & Data Parsing:
 * Performs real HTTP GET requests to open public REST APIs (TheMealDB API)
 * to fetch dish details and dynamic food imagery JSON, and parses JSON data using org.json.
 */
public final class FoodApiClient {

    // Free meal database open REST API
    private static final String API_SEARCH_URL = "https://www.themealdb.com/api/json/v1/1/search.php?s=";

    // Cache of dish name to real image URL
    private static final Map<String, String> IMAGE_URL_CACHE = new HashMap<>();

    static {
        // Fallback high-quality real dish photos in case network is disconnected
        IMAGE_URL_CACHE.put("Cheeseburger", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=600&q=80");
        IMAGE_URL_CACHE.put("Margherita Pizza", "https://images.unsplash.com/photo-1604382355076-af4b0eb60143?auto=format&fit=crop&w=600&q=80");
        IMAGE_URL_CACHE.put("Chicken Alfredo Pasta", "https://images.unsplash.com/photo-1621996346565-e3d5d6281691?auto=format&fit=crop&w=600&q=80");
        IMAGE_URL_CACHE.put("Garden Salad", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=600&q=80");
        IMAGE_URL_CACHE.put("Tomato Soup", "https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=600&q=80");
        IMAGE_URL_CACHE.put("Orange Juice", "https://images.unsplash.com/photo-1613478223719-2ab802602423?auto=format&fit=crop&w=600&q=80");
        IMAGE_URL_CACHE.put("Mango Shake", "https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=600&q=80");
        IMAGE_URL_CACHE.put("Fruit Salad", "https://images.unsplash.com/photo-1490474418585-ba9bad8fd0ea?auto=format&fit=crop&w=600&q=80");
    }

    private FoodApiClient() {}

    /**
     * Sends an HTTP request to TheMealDB REST API, receives JSON,
     * and parses it to retrieve real food image URLs.
     *
     * @param query Search term (e.g., "pizza", "burger", "salad")
     * @return Real HTTP image URL, or cached/fallback URL if unavailable
     */
    public static String fetchRealFoodImageUrl(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        // Curated high quality food images for our catalog take top priority
        if (IMAGE_URL_CACHE.containsKey(query)) {
            return IMAGE_URL_CACHE.get(query);
        }

        // Clean query keyword for search (e.g. "Chicken Alfredo Pasta" -> "pasta" or "chicken")
        String keyword = extractSearchKeyword(query);

        try {
            URI uri = URI.create(API_SEARCH_URL + java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8));
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                // Data Parsing: Parse JSON response
                JSONObject json = new JSONObject(response.toString());
                if (!json.isNull("meals")) {
                    JSONArray meals = json.getJSONArray("meals");
                    if (meals.length() > 0) {
                        JSONObject firstMeal = meals.getJSONObject(0);
                        if (firstMeal.has("strMealThumb") && !firstMeal.isNull("strMealThumb")) {
                            String thumbUrl = firstMeal.getString("strMealThumb");
                            IMAGE_URL_CACHE.put(query, thumbUrl);
                            return thumbUrl;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[FoodApiClient] Network fetch note: " + e.getMessage() + ". Using fallback image.");
        }

        // Return curated real food photo from cache/fallback
        return IMAGE_URL_CACHE.getOrDefault(query, IMAGE_URL_CACHE.getOrDefault(keyword,
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80"));
    }

    private static String extractSearchKeyword(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("burger")) return "burger";
        if (lower.contains("pizza")) return "pizza";
        if (lower.contains("pasta")) return "pasta";
        if (lower.contains("salad")) return "salad";
        if (lower.contains("soup")) return "soup";
        if (lower.contains("juice") || lower.contains("orange")) return "orange";
        if (lower.contains("shake") || lower.contains("mango")) return "mango";
        return name;
    }
}
